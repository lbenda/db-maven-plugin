package com.nesting.maven2.db;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/** Abstract mojo that all DB related mojos
 * inherit from. */
public abstract class AbstractDBMojo extends AbstractMojo {

  /** The database connection settings for
   * the application.
   * @parameter
   * @required */
  private DbConfig[] dbConfig;

  public DbConfig getDbConfig() { return currentDbConfig; }
  /** Currenent executed db config */
  private DbConfig currentDbConfig;

  /** The names of configs which is executed, if length = 0 or null then execute for all configs
   * the application.
   * @parameter */
  private String[] runConfigs;

  /** The batch size when executing batches.
   * @parameter default-value="20"
   * @required */
  private int batchSize;

  /** Whether or not to use SQL batches.
   * @parameter default-value="true"
   * @required */
  private boolean useBatch;

  /** The {@link Settings} object.
   * @parameter default-value="${settings}"
   * @required
   * @readonly */
  private Settings settings;

  /** Child mojos need to implement this.
   * @throws MojoExecutionException on error.
   * @throws MojoFailureException on error */
  public abstract void executeInternal() throws MojoExecutionException,  MojoFailureException;

  /** {@inheritDoc} */
  public final void execute() throws MojoExecutionException, MojoFailureException {
    Set<String> cfNames = new HashSet<>();
    if (runConfigs != null) { cfNames.addAll(Arrays.asList(runConfigs)); }
    for (DbConfig cf : dbConfig) {
      if (cfNames.isEmpty() || cfNames.contains(cf.getName())) {
        getLog().info("dbConfig: " + cf.getName());
        currentDbConfig = cf;
        checkDbSettings(cf.getAdminDbConnectionSettings(), "admin");
        checkDbSettings(cf.getAppDbConnectionSettings(), "application");
        executeInternal();
      }
    }
  }

  /** Checks the given database connection settings.
   * @param dbSettings the settings to check
   * @param name the name of the settings
   * @throws MojoExecutionException on error
   * @throws MojoFailureException on error */
  private void checkDbSettings(DatabaseConnectionSettings dbSettings, String name) throws MojoExecutionException, MojoFailureException {

    // check server
    if (!StringUtils.isEmpty(dbSettings.getServerId())) {
      Server server = settings.getServer(dbSettings.getServerId());
      if (server==null) {
        throw new MojoFailureException("["+name+"] Server ID: "
                                       +dbSettings.getServerId()+" not found!");
      } else if (StringUtils.isEmpty(server.getUsername())) {
        throw new MojoFailureException("["+name+"] Server ID: "+dbSettings.getServerId()+" found, "
                                       +"but username is empty!");
      }
      // check non server settings
    } else if (StringUtils.isEmpty(dbSettings.getUserName())) {
      throw new MojoFailureException("["+name+"] No username defined!");
    }

    // check url and jdbc driver
    if (StringUtils.isEmpty(dbSettings.getJdbcUrl())) {
      throw new MojoFailureException("["+name+"] No jdbc url defined!");
    } else if (StringUtils.isEmpty(dbSettings.getJdbcDriver())) {
      throw new MojoFailureException("["+name+"] No jdbc driver defined!");
    }
  }

  /** Executes all of the sql scripts in a given directory
   * using the given database connection.
   * @param directory the directory where the scripts reside
   * @param con the database connection
   * @throws SQLException on error
   * @throws MojoFailureException on error
   * @throws MojoExecutionException on error
   * @throws IOException on error */
  protected void executeScriptsInDirectory(File directory, Connection con)
    throws SQLException, MojoFailureException, MojoExecutionException, IOException {

    // talk a bit :)
    getLog().info("Executing scripts in: "+directory.getName());

    // make sure we can read it, and that it's
    // a file and not a directory
    if (!directory.isDirectory()) {
      throw new MojoFailureException(
                                     directory.getName()+" is not a directory");
    }

    // get all files in directory
    File[] files = directory.listFiles();

    // sort
    Arrays.sort(files, new Comparator() {
        public int compare(Object arg0, Object arg1) {
          return ((File)arg0).getName().compareTo(((File)arg1).getName());
        } }
      );

    // loop through all the files and execute them
    for (int i = 0; i<files.length; i++) {
      if (!files[i].isDirectory() && files[i].isFile() && !files[i].getName().endsWith("~")) {
        double startTime = System.currentTimeMillis();
        if (useBatch) {
          batchExecuteSqlScript(files[i], con);
        } else {
          executeSqlScript(files[i], con);
        }
        double endTime = System.currentTimeMillis();
        double elapsed = ((endTime-startTime)/1000.0);
        getLog().info(" script completed execution in "+elapsed+" second(s)");
      }
    }

  }

  private Reader inputStreamToReaderBOM(InputStream in) throws IOException {
    BOMInputStream bOMInputStream = new BOMInputStream(in, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE,
                                                       ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
    ByteOrderMark bom = bOMInputStream.getBOM();
    String charsetName = bom == null ? getDbConfig().getScriptEncoding() : bom.getCharsetName();
    return new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);
  }

  /** Batch executes a script file.
   * @param file the file
   * @param con the connection
   * @throws SQLException on error
   * @throws MojoFailureException on error
   * @throws MojoExecutionException on error
   * @throws IOException on error */
  protected void batchExecuteSqlScript(File file, Connection con)
    throws SQLException, MojoFailureException, MojoExecutionException, IOException {
    // talk a bit :)
    getLog().info("batch executing script: "+file.getName());

    // make sure we can read it, and that it's
    // a file and not a directory
    if (!file.exists() || !file.canRead()
        || file.isDirectory() || !file.isFile()) {
      throw new MojoFailureException(file.getName()+" is not a file");
    }

    // open input stream to file
    InputStream ips = new FileInputStream(file);

    // if it's a compressed file (gzip) then unzip as
    // we read it in
    if (file.getName().toUpperCase().endsWith("GZ")) {
      ips = new GZIPInputStream(ips);
    }

    // check encoding
    checkEncoding();
    // our file reader
    Reader reader = inputStreamToReaderBOM(ips);
    // create SQL Statement
    Statement st = con.createStatement();

    StringBuilder sql = new StringBuilder();
    String line;
    BufferedReader in = new BufferedReader(reader);

    // loop through the statements
    int execCount = 0;
    List<String> sqlLines = new ArrayList<>();
    line = in.readLine();
    do {
      // append the line
      line = line == null ? getDbConfig().getTransactionDelimiter() : line;
      line.trim();
      if (getDbConfig().getTransactionDelimiter().toUpperCase().equals(line.toUpperCase())) {
        sqlLines.add(sql.toString());
        executeBatch(st, sqlLines);
        sql.delete(0, sql.length());
        sqlLines.clear();
        execCount++;
      } else {
        sql.append("\n").append(line);
        // if the line ends with the delimiter, then
        // lets execute it
        if (sql.toString().endsWith(getDbConfig().getSqlDelimiter())) {
          String sqlLine = sql.substring(0, sql.length() - getDbConfig().getSqlDelimiter().length());
          sqlLines.add(sqlLine);
          sql.replace(0, sql.length(), "");
          execCount++;
          if (sqlLines.size() >= batchSize) {
            executeBatch(st, sqlLines);
            sqlLines.clear();
          }
        }
      }
    } while ((line = in.readLine()) != null || sql.length() > 0);

    st.close();
    reader.close();
    in.close();

    getLog().info(" "+execCount+" statements batch executed from "+file.getName());
  }

  /** Executes the given sql script, using the given
   * connection.
   * @param file the file to execute
   * @param con the connection
   * @throws SQLException on error
   * @throws MojoFailureException on error
   * @throws MojoExecutionException on error
   * @throws IOException on error */
  protected void executeSqlScript(File file, Connection con)
    throws SQLException, MojoFailureException, MojoExecutionException, IOException {
    // talk a bit :)
    getLog().info("executing script: "+file.getName());
    // make sure we can read it, and that it's
    // a file and not a directory
    if (!file.exists() || !file.canRead() || file.isDirectory() || !file.isFile()) {
      throw new MojoFailureException(file.getName()+" is not a file");
    }

    // open input stream to file
    InputStream ips = new FileInputStream(file);

    // if it's a compressed file (gzip) then unzip as
    // we read it in
    if (file.getName().toUpperCase().endsWith("GZ")) {
      ips = new GZIPInputStream(ips);
      getLog().info(" file is gz compressed, using gzip stream");
    }

    // check encoding
    checkEncoding();

    // our file reader
    Reader reader = inputStreamToReaderBOM(ips);

    // create SQL Statement
    Statement st = con.createStatement();

    StringBuffer sql = new StringBuffer();
    String line;
    BufferedReader in = new BufferedReader(reader);

    // loop through the statements
    int execCount = 0;
    line = in.readLine();
    do {
      line = line == null ? getDbConfig().getTransactionDelimiter() : line;
      if (getDbConfig().getTransactionDelimiter().toUpperCase().equals(line.toUpperCase())) {
        executeStatement(st, sql.toString());
        sql.delete(0, sql.length());
        execCount++;
      } else {
        // append the line
        line.trim();
        sql.append("\n").append(line);
        // if the line ends with the delimiter, then
        // lets execute it
        if (sql.toString().endsWith(getDbConfig().getSqlDelimiter())) {
          String sqlLine = sql.substring(0, sql.length() - getDbConfig().getSqlDelimiter().length());
          executeStatement(st, sqlLine);
          sql.replace(0, sql.length(), "");
          execCount++;
        }
      }
    } while ((line = in.readLine()) != null || sql.length() > 0);

    st.close();
    reader.close();
    in.close();

    getLog().info(" "+execCount+" statements executed from "+file.getName());
  }

  /** Executes a batch update.
   * @param st the statement
   * @param sqlLines the sql lines
   * @throws SQLException on error */
  protected void executeBatch(Statement st, List<String> sqlLines) throws SQLException {
    for (Iterator<String> itt = sqlLines.iterator(); itt.hasNext(); ) {
      String str = itt.next();
      if (str == null || "".equals(str.trim())) { itt.remove(); }
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug("Executing batch");
      getLog().debug(StringUtils.join(sqlLines, "\n"));
    }

    // add to batch
    for (int i=0; i<sqlLines.size(); i++) {
      st.addBatch((String) sqlLines.get(i));
    }

    int[] ret  = st.executeBatch();
    if (getLog().isDebugEnabled()) {
      getLog().debug("    "+ret.length+" statement(s) executed");
    }

    for (int i=0; i<ret.length; i++) {
      if (ret[i] == Statement.SUCCESS_NO_INFO && getLog().isDebugEnabled()) {
        getLog().debug("    statement " + i + " processed successfully without return results");
      } else if (ret[i] == Statement.EXECUTE_FAILED) {
        getLog().error("    error durring batch execution of statement: "+sqlLines.get(i));
        throw new SQLException("Error executing: "+ sqlLines.get(i));
      } else if (ret[i] >= 0 && getLog().isDebugEnabled()) {
        getLog().debug("    statement "+i+" processed successfully with "+ret[i]+" records effected");
      }
    }

  }

  /** Runs the given SQL statement.
   * @param st the statement to run it on
   * @param sqlLine the sql statement
   * @throws SQLException on error */
  protected void executeStatement(Statement st, String sqlLine) throws SQLException {
    if (getLog().isDebugEnabled()) {
      getLog().debug("    executing:\n"+sqlLine);
    }
    boolean execRet = false;
    try {
      execRet = st.execute(sqlLine);
    } catch(SQLException sqle) {
      SQLException se = new SQLException(sqle.getMessage() + "\n\nSQL:\n" + sqlLine,
                                         sqle.getSQLState(), sqle.getErrorCode());
      se.setNextException(sqle);
      throw se;
    }
    boolean loop = true;
    while (loop) {
      if (execRet) {
        getLog().warn(" statement returned a resultset");
      } else {
        // Got an update count
        int count = st.getUpdateCount();

        if (count == -1) {
          // Nothing left
          loop = false;
        } else if (getLog().isDebugEnabled()){
          // An update count was returned
          getLog().debug("    "+count+" row(s) updated");
        }
      }
      if (loop) {
        execRet = st.getMoreResults();
      }
    }
  }

  /** Returns a {@link Connection} to the application database.
   * @return the connection
   * @throws SQLException on error
   * @throws MojoFailureException on error */
  protected Connection openApplicationDbConnection() throws SQLException, MojoFailureException {
    return openConnection(getDbConfig().getAppDbConnectionSettings());
  }

  /** Returns a {@link Connection} to the application
   * database.
   * @return the connection
   * @throws SQLException on error
   * @throws MojoFailureException on error */
  protected Connection openAdminDbConnection() throws SQLException, MojoFailureException {
    return openConnection(getDbConfig().getAdminDbConnectionSettings());
  }

  /** Opens a connection using the given settings.
   * @param dbSettings the connection settings
   * @return the Connection
   * @throws SQLException on error
   * @throws MojoFailureException on error */
  private Connection openConnection(DatabaseConnectionSettings dbSettings)
    throws SQLException, MojoFailureException {

    String username = null;
    String password = null;

    // use settings to get authentication info for the given server
    if (!StringUtils.isEmpty(dbSettings.getServerId())) {
      Server server = settings.getServer(dbSettings.getServerId());
      username = server.getUsername();
      password = server.getPassword();
      // use settings in pom.xml
    } else {
      username = dbSettings.getUserName();
      password = dbSettings.getPassword();
    }

    // make sure the driver is good
    try {
      Class.forName(dbSettings.getJdbcDriver());
    } catch(Exception e) {
      throw new MojoFailureException(e.getMessage());
    }

    // consult the driver manager for the connection
    Connection con = DriverManager.getConnection(dbSettings.getJdbcUrl(), username, password);
    // we're good :)
    return con;
  }

  private void checkEncoding() {
    if (getDbConfig().getScriptEncoding() == null) {
      getDbConfig().setScriptEncoding(Charset.defaultCharset().name());
      getLog().warn("Using platform encoding (" + getDbConfig().getScriptEncoding() + ") for executing script, i.e. build is platform dependent!");
    } else {
      getLog().info(" setting encoding for executing script: " + getDbConfig().getScriptEncoding());
    }
  }
}
