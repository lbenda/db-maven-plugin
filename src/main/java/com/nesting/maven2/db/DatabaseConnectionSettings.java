package com.nesting.maven2.db;

/** Class for storing database connection settings. */
public class DatabaseConnectionSettings {
  private String jdbcUrl; public String getJdbcUrl() { return jdbcUrl; } public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
  private String jdbcDriver; public String getJdbcDriver() { return jdbcDriver; } public void setJdbcDriver(String jdbcDriver) { this.jdbcDriver = jdbcDriver; }
  private String serverId; public String getServerId() { return serverId; } public void setServerId(String serverId) { this.serverId = serverId; }
  private String userName; public String getUserName() { return userName; } public void setUserName(String userName) { this.userName = userName; }
  private String password; public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
}
