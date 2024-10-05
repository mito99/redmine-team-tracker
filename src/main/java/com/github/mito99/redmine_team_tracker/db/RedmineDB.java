package com.github.mito99.redmine_team_tracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import com.github.mito99.redmine_team_tracker.db.RedmineDBEntity.Issue;
import com.github.mito99.redmine_team_tracker.db.RedmineDBEntity.Journal;
import com.github.mito99.redmine_team_tracker.util.ThrowingFunction;
import lombok.SneakyThrows;
import lombok.val;

public class RedmineDB {

  @SneakyThrows
  public static Connection getConnection(String redmineJdbcUrl, String redmineDbUser,
      String redmineDbPassword) {
    val connection = DriverManager.getConnection(redmineJdbcUrl, redmineDbUser, redmineDbPassword);
    connection.setAutoCommit(false);
    return connection;
  }

  @SneakyThrows
  public static Stream<Issue> getTargetIssue(Connection con) {
    val sql = "SELECT "
        + "i.id, i.created_on, i.status_id, is2.id, is2.name as status_name, cv.value as team_name "
        + "FROM issues i " + "JOIN versions v ON i.fixed_version_id = v.id "
        + "JOIN projects p ON v.project_id = p.id " + "JOIN trackers t ON i.tracker_id = t.id "
        + "JOIN issue_statuses is2 ON i.status_id = is2.id "
        + "JOIN custom_values cv ON cv.customized_id = i.id AND cv.custom_field_id = 54 "
        + "WHERE v.name = 'バージョン未決定' " + "AND p.name = 'バグ・タスク管理' " + "AND t.name = 'B票' "
        + "ORDER BY i.created_on asc";

    val stmt = con.createStatement();
    val resultSet = stmt.executeQuery(sql);
    return resultSetToStream(resultSet, rs -> {
      return Issue.builder().id(rs.getInt("id")).statusName(rs.getString("status_name"))
          .teamName(rs.getString("team_name")).createdOn(rs.getString("created_on")).build();
    });
  }

  @SneakyThrows
  public static List<Journal> getStatusJornal(Connection con, int ticketId) {
    return getJounal(con, ticketId, "status_id");
  }

  @SneakyThrows
  public static List<Journal> getTeamJornal(Connection con, int ticketId) {
    return getJounal(con, ticketId, "status_id");
  }

  @SneakyThrows
  public static List<Journal> getJounal(Connection con, int ticketId, String propKey) {
    val sql = "SELECT " + "  j.created_on, " + "  jd.value AS new_value, " + "  jd.old_value "
        + "FROM " + "  journals j " + "  JOIN journal_details jd ON jd.journal_id = j.id "
        + "WHERE " + "  j.journalized_id = ? " + "  AND jd.prop_key = ? " + "ORDER BY "
        + "  j.created_on ";

    val stmt = con.prepareStatement(sql);
    stmt.setInt(1, ticketId);
    stmt.setString(2, propKey);

    val resultSet = stmt.executeQuery();
    return resultSetToStream(resultSet, rs -> {
      return Journal.builder().createdOn(rs.getString("created_on"))
          .newValue(rs.getString("new_value")).oldValue(rs.getString("old_value")).build();
    }).collect(Collectors.toList());
  }

  private static <T, E extends SQLException> Stream<T> resultSetToStream(ResultSet rs,
      ThrowingFunction<ResultSet, T, E> mapper) {

    Spliterator<T> spliterator =
        new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
          @Override
          public boolean tryAdvance(Consumer<? super T> action) {
            try {
              if (!rs.next()) {
                return false;
              }
              action.accept(mapper.apply(rs));
              return true;
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        };
    return StreamSupport.stream(spliterator, false).onClose(() -> {
      try {
        rs.close();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
