# db-maven-plugin
Maven plugin for manipulate with database

This project is fork of project **maven-db-plugin** from googlecode

https://maven-db-plugin.googlecode.com

# Repository configuration
```
  <pluginRepositories>
    <pluginRepository>
      <id>db-maven-plugin-mvn-repo</id>
      <url>https://raw.github.com/lbenda/db-maven-plugin/mvn-repo/</url>
      <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
```

# Bring new fetchures
1. The can read BOM in file so can determine encoding of script which is created by *SQL Managemant Studio*.
2. Can deal with MS SQL GO to execute transaction.
3. Can define more then one database and script for this database in single project.

Also the plugin is renamed from reserved *maven-..-plugin* to *..-maven-plugin*

# goals

* db:create - execute the create database statements
* db:drop - execute the drop database statements
* db:schema - execute all of the scripts in the schema scripts directory
* db:data - execute all of the data in the schema scripts directory
* db:update - execute all of the update in the schema scripts directory

# common usage

to get started:

```mvn db:create db:schema db:data```

to re-initialize database:

```mvn db:drop db:create db:schema db:data```

to update database:

```mvn db:update```

# sample development database configuration

this sample assumes the following:

* database is mysql and located at localhost - root password is empty
* database name is someAppDb, user is someAppDbUser and password is someAppDbPassword
* schema scripts are in src/main/sql/schema
* data scripts are in src/main/sql/data
* update scripts are in src/main/sql/update

```
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.lbenda</groupId>
        <artifactId>db-maven-plugin</artifactId>
        <version>1.6-SNAPSHOT</version>
        <configuration>
          <runConfigs>
            <param>DWHCORE</param>
            <param>DWHMART</param>
          </runConfigs>
          <dbConfig>
            <param>
              <name>DWHCORE</name>
              <adminDbConnectionSettings>
                <jdbcDriver>com.microsoft.sqlserver.jdbc.SQLServerDriver</jdbcDriver>
                <jdbcUrl>${mssql.jdbc.dwhcore.url}</jdbcUrl>
                <userName>${mssql.jdbc.dwhcore.userName}</userName>
                <password>${mssql.jdbc.dwhcore.password}</password>
              </adminDbConnectionSettings>
              <appDbConnectionSettings>
                <jdbcDriver>com.microsoft.sqlserver.jdbc.SQLServerDriver</jdbcDriver>
                <jdbcUrl>${mssql.jdbc.dwhcore.url}</jdbcUrl>
                <userName>${mssql.jdbc.dwhcore.userName}</userName>
                <password>${mssql.jdbc.dwhcore.password}</password>
              </appDbConnectionSettings>
              <scriptEncoding>UTF-8</scriptEncoding>
              <sqlDelimiter>;</sqlDelimiter>
              <transactionDelimiter>go</transactionDelimiter>
              <dataDirectory><param>src/main/sql/core/data</param></dataDirectory>
              <schemaDirectory><param>src/main/sql/core/schema</param></schemaDirectory>
              <updateDirectory><param>src/main/sql/core/update</param></updateDirectory>
              <dbCreateFile>src/main/sql/core/createDb.sql</dbCreateFile>
              <dbDropFile>src/main/sql/core/dropDb.sql</dbDropFile>
            </param>
            <param>
              <name>DWHMART</name>
              <adminDbConnectionSettings>
                <jdbcDriver>com.microsoft.sqlserver.jdbc.SQLServerDriver</jdbcDriver>
                <jdbcUrl>${mssql.jdbc.dwhmarts.url}</jdbcUrl>
                <userName>${mssql.jdbc.dwhmarts.userName}</userName>
                <password>${mssql.jdbc.dwhmarts.password}</password>
              </adminDbConnectionSettings>
              <appDbConnectionSettings>
                <jdbcDriver>com.microsoft.sqlserver.jdbc.SQLServerDriver</jdbcDriver>
                <jdbcUrl>${mssql.jdbc.dwhmarts.url}</jdbcUrl>
                <userName>${mssql.jdbc.dwhmarts.userName}</userName>
                <password>${mssql.jdbc.dwhmarts.password}</password>
              </appDbConnectionSettings>
              <scriptEncoding>UTF-8</scriptEncoding>
              <sqlDelimiter>;</sqlDelimiter>
              <transactionDelimiter>go</transactionDelimiter>
              <dataDirectory><param>src/main/sql/marts/data</param></dataDirectory>
              <schemaDirectory><param>src/main/sql/marts/schema</param></schemaDirectory>
              <updateDirectory><param>src/main/sql/marts/update</param></updateDirectory>
              <dbCreateFile>src/main/sql/marts/createDb.sql</dbCreateFile>
              <dbDropFile>src/main/sql/marts/dropDb.sql</dbDropFile>
            </param>
          </dbConfig>
        </configuration>
        <dependencies>
          <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>sqljdbc4</artifactId>
            <version>4.0</version>
          </dependency>
        </dependencies>
      </plugin>
```

the *adminDbConnectionSettings* is for administrative access to the database (ability to create and drop databases - etc.). *appDbConnectionSettings* is for non-admin access to the database. This is the database that you would be working on in your application for example. Both of these also support the <serverId> attribute in case you'd rather store the host/usr/pass in your maven settings.xml instead.

Script files can have any extension but should contain sql code. Any script file ending in the .gz extension is assumed to be a compressed script and will be decompressed in memory before executing it on the database server.

By the array *runConfigs* you can choose which configuration will be executed.
