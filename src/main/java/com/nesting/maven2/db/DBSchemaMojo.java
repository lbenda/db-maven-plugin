package com.nesting.maven2.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/** Mojo for creating database schemas.
 * @goal schema */
public class DBSchemaMojo extends AbstractDBMojo {

  /** {@inheritDoc} */
  public void executeInternal() throws MojoExecutionException, MojoFailureException {
    try {
      Connection con = openApplicationDbConnection();
      for (File schemaDirectory : getDbConfig().getSchemaDirectory()) {
        executeScriptsInDirectory(schemaDirectory, con);
      }
    } catch(SQLException se) {
      throw new MojoExecutionException("Error executing database scripts", se);
    } catch(IOException ioe) {
      throw new MojoExecutionException("Error executing database scripts", ioe);
    }
  }
}
