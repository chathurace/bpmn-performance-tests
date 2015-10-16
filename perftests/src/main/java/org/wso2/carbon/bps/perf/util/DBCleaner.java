/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.bps.perf.util;

import java.sql.*;

public class DBCleaner {

    String dbURL = "jdbc:mysql://localhost:3306/bpmnperfdb1";

    public static void main(String[] args) {

//        String dbURL = "jdbc:mysql://localhost:3306/activitibmdb";

        try {
            new DBCleaner().dropTables(args);
//            new DBCleaner().checkDB(dbURL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanDB(String dbURL) {

        Connection conn = null;
        try {
            System.out.println("Loading mysql driver...");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            System.out.println("Getting a connection to the database...");
            conn = DriverManager.getConnection(dbURL, "fs", "fs");
            conn.setAutoCommit(false);
            DatabaseMetaData m = conn.getMetaData();
            ResultSet tables = m.getTables(conn.getCatalog(), null, "%", null);
            execute("SET FOREIGN_KEY_CHECKS=0", conn);
            while (tables.next()) {
                String tableName = tables.getString(3);
                String clearStatement = "DELETE FROM " + tableName;
                execute(clearStatement, conn);
            }
            execute("SET FOREIGN_KEY_CHECKS=1", conn);
            conn.commit();
            System.out.println("DB clean completed successfully.");
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void dropTables(String[] args) {

        if (args.length == 3) {
            String ip = args[1];
            String dbName = args[2];
            dbURL = "jdbc:mysql://" + ip + ":3306/" + dbName;
        }

        Connection conn = null;
        try {
            System.out.println("Loading mysql driver...");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            System.out.println("Getting a connection to the database...");
            conn = DriverManager.getConnection(dbURL, "fs", "fs");
            conn.setAutoCommit(false);
            DatabaseMetaData m = conn.getMetaData();
            ResultSet tables = m.getTables(conn.getCatalog(), null, "%", null);
            execute("SET FOREIGN_KEY_CHECKS=0", conn);
            while (tables.next()) {
                String tableName = tables.getString(3);
                String clearStatement = "DROP TABLE " + tableName;
                execute(clearStatement, conn);
            }
            execute("SET FOREIGN_KEY_CHECKS=1", conn);
            conn.commit();
            System.out.println("DB clean completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    public void checkDB(String dbURL) {

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbURL, "fs", "fs");
            conn.setAutoCommit(false);
            DatabaseMetaData m = conn.getMetaData();
            ResultSet tables = m.getTables(conn.getCatalog(), null, "%", null);
            while (tables.next()) {
                String tableName = tables.getString(3);
                String checkStatement = "SELECT COUNT(*) FROM " + tableName;
                Statement s = conn.createStatement();
                ResultSet r = s.executeQuery(checkStatement);
                if (r.next()) {
                    int count = r.getInt(1);
                    if (count > 0) {
                        System.out.println(tableName + " - " + count);
                    }
                }
                r.close();
                s.close();
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void execute(String statement, Connection conn) throws SQLException {
        System.out.println("Executing: " + statement);
        Statement s = conn.createStatement();
        s.execute(statement);
        s.close();
    }
}
