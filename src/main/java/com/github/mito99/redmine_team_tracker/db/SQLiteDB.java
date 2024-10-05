package com.github.mito99.redmine_team_tracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.github.mito99.redmine_team_tracker.model.Aggregation;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

public class SQLiteDB {

  @SneakyThrows
  public static Connection getConnection(String sqliteDbUrl) {
    val sqliteConn = DriverManager.getConnection(sqliteDbUrl);
    sqliteConn.setAutoCommit(false);
    return sqliteConn;
  }

  public static void createSQLiteTable(Connection sqliteConn) throws SQLException {
    val sql = "CREATE TABLE IF NOT EXISTS ticket_aggregation (" + "date TEXT, " + "team_name TEXT, "
        + "incomplete_increase INTEGER, " + "incomplete_total INTEGER, "
        + "PRIMARY KEY (date, team_name)" + ");";
    val stmt = sqliteConn.createStatement();
    stmt.execute(sql);
    stmt.close();
  }

  @SneakyThrows
  public static void insertAggregation(Connection sqliteConn, Aggregation aggregation) {
    val sql =
        "INSERT INTO ticket_aggregation (date, team_name, incomplete_increase, incomplete_total) "
            + "VALUES (?, ?, ?, ?) " + "ON CONFLICT(date, team_name) DO UPDATE SET "
            + "incomplete_increase=excluded.incomplete_increase, "
            + "incomplete_total=excluded.incomplete_total;";

    @Cleanup
    val pstmt = sqliteConn.prepareStatement(sql);
    aggregation.forEach(teamStats -> {
      pstmt.setString(1, teamStats.getDate().toString());
      pstmt.setString(2, teamStats.getTeamName());
      pstmt.setInt(3, teamStats.getIncrease());
      pstmt.setInt(4, teamStats.getTotal());
      pstmt.addBatch();
    });
    pstmt.executeBatch();
  }
}
