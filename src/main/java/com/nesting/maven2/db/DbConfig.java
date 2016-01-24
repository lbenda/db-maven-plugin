package com.nesting.maven2.db;

import java.io.File;

/** Configuraiton of single databsae */
public class DbConfig {
  public String name; public String getName() { return name; }; public void setName(String name) { this.name = name; }
  private DatabaseConnectionSettings appDbConnectionSettings; public DatabaseConnectionSettings getAppDbConnectionSettings() { return appDbConnectionSettings; } public void setAppDbConnectionSettings(DatabaseConnectionSettings appDbConnectionSettings) { this.appDbConnectionSettings = appDbConnectionSettings; }
  private DatabaseConnectionSettings adminDbConnectionSettings; public DatabaseConnectionSettings getAdminDbConnectionSettings() { return adminDbConnectionSettings; } public void setAdminDbConnectionSettings(DatabaseConnectionSettings adminDbConnectionSettings) { this.adminDbConnectionSettings = adminDbConnectionSettings; }
  private String scriptEncoding; public String getScriptEncoding() { return scriptEncoding; } public void setScriptEncoding(String scriptEncoding) { this.scriptEncoding = scriptEncoding; }
  private String sqlDelimiter; public String getSqlDelimiter() { return sqlDelimiter; } public void setSqlDelimiter(String sqlDelimiter) { this.sqlDelimiter = sqlDelimiter; }
  private String transactionDelimiter; public String getTransactionDelimiter() { return transactionDelimiter; } public void setTransactionDelimiter(String transactionDelimiter) { this.transactionDelimiter = transactionDelimiter; }
  private File dbCreateFile; public File getDbCreateFile() { return dbCreateFile; } public void setDbCreateFile(File dbCreateFile) { this.dbCreateFile = dbCreateFile; }
  private File dbDropFile; public File getDbDropFile() { return dbDropFile; } public void setDbDropFile(File dbDropFile) { this.dbDropFile = dbDropFile; }
  private File[] dataDirectory; public File[] getDataDirectory() { return dataDirectory; } public void setDataDirectory(File[] dataDirectory) { this.dataDirectory = dataDirectory; }
  private File[] updateDirectory; public File[] getUpdateDirectory() { return updateDirectory; } public void setUpdateDirectory(File[] updateDirectory) { this.updateDirectory = updateDirectory; }
  private File[] schemaDirectory; public File[] getSchemaDirectory() { return schemaDirectory; } public void setSchemaDirectory(File[] schemaDirectory) { this.schemaDirectory = schemaDirectory; }
}
