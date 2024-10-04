package jp.hitachi.mineo.tool.redmine_team_tracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppMain {

  // 定数定義
  private static final String INCOMPLETE_STATUS_1 = "新規";
  private static final String INCOMPLETE_STATUS_2 = "調査依頼";
  private static final String INCOMPLETE_STATUS_3 = "調査中";
  private static final Set<String> INCOMPLETE_STATUSES = new HashSet<>(Arrays.asList(
      INCOMPLETE_STATUS_1, INCOMPLETE_STATUS_2, INCOMPLETE_STATUS_3));

  // カスタムフィールドキー
  private static final int TEAM_FIELD_KEY = 54;
  // ステータスフィールドキー（Redmineのバージョンによって異なる場合があります）
  private static final int STATUS_FIELD_KEY = 7; // 仮定値。実際のキーを確認してください。

  public static void main(String... args) {
    if (args.length != 2) {
      System.out.println("使用方法: java RedmineTicketAggregator <開始日> <終了日>");
      System.out.println("日付フォーマット: yyyy-MM-dd");
      return;
    }

    String startDateStr = args[0];
    String endDateStr = args[1];

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate startDate;
    LocalDate endDate;
    try {
      startDate = LocalDate.parse(startDateStr, formatter);
      endDate = LocalDate.parse(endDateStr, formatter);
    } catch (Exception e) {
      System.out.println("日付の形式が正しくありません。フォーマット: yyyy-MM-dd");
      return;
    }

    // Redmineデータベースの接続情報
    String redmineJdbcUrl = "jdbc:mysql://localhost:13306/redmine"; // 例: MySQL
    String redmineDbUser = "redmine";
    String redmineDbPassword = "redmine";

    // SQLiteの出力先
    String sqliteDbUrl = "jdbc:sqlite:output.db";

    Connection redmineConn = null;
    Connection sqliteConn = null;

    try {
      // Redmineデータベースに接続
      redmineConn = DriverManager.getConnection(redmineJdbcUrl, redmineDbUser, redmineDbPassword);
      redmineConn.setAutoCommit(false);

      // SQLiteデータベースに接続（なければ作成）
      sqliteConn = DriverManager.getConnection(sqliteDbUrl);
      sqliteConn.setAutoCommit(false);

      // 集計結果を保持するマップ: date -> team -> [増加, 総計]
      Map<LocalDate, Map<String, TeamStats>> aggregation = new HashMap<>();

      // 日付範囲をリスト化
      List<LocalDate> dateRange = new ArrayList<>();
      LocalDate date = startDate;
      while (!date.isAfter(endDate)) {
        dateRange.add(date);
        date = date.plusDays(1);
      }

      // 全てのチーム名を取得（カスタムフィールドから）
      Set<String> allTeams = getAllTeams(redmineConn);

      // 初期化
      for (LocalDate d : dateRange) {
        aggregation.put(d, new HashMap<>());
        for (String team : allTeams) {
          aggregation.get(d).put(team, new TeamStats());
        }
      }

      // チケットの取得
      String issueQuery = "SELECT" +
          "  i.id, " +
          "  i.created_on, " +
          "  i.status_id " +
          "FROM " +
          "  issues i " +
          "JOIN versions v ON i.fixed_version_id = v.id " +
          "JOIN projects p ON v.project_id = p.id " +
          "JOIN trackers t ON i.tracker_id = t.id " +
          "WHERE " +
          "  v.name = 'バージョン未決定' " +
          "AND p.name = 'バグ・タスク管理' " +
          "AND t.name = 'B票'";
      Statement issueStmt = redmineConn.createStatement();
      ResultSet issueRs = issueStmt.executeQuery(issueQuery);

      // ステータスIDからステータス名をマッピング
      Map<Integer, String> statusMap = getStatusMap(redmineConn);

      // チケットごとに処理
      while (issueRs.next()) {
        int issueId = issueRs.getInt("id");
        Timestamp createdOnTs = issueRs.getTimestamp("created_on");
        LocalDate createdOn = createdOnTs.toLocalDateTime().toLocalDate();
        int currentStatusId = issueRs.getInt("status_id");
        String currentStatus = statusMap.get(currentStatusId);

        // ステータス履歴の取得
        Map<LocalDate, String> statusHistory = getStatusHistory(redmineConn, issueId, statusMap);
        // チーム履歴の取得
        Map<LocalDate, String> teamHistory = getTeamHistory(redmineConn, issueId);

        // 状態とチームの初期値を設定
        String status = currentStatus;
        String team = teamHistory.isEmpty() ? "未設定" : "未設定"; // 初期値

        // チケットの作成日以前の履歴は無視
        // チケットの作成日から日付範囲の開始日までの履歴を適用
        // ステータスとチームの初期状態を決定
        List<LocalDate> sortedDates = new ArrayList<>(statusHistory.keySet());
        Collections.sort(sortedDates);
        for (LocalDate d : sortedDates) {
          if (d.isBefore(createdOn)) {
            continue;
          }
          if (!d.isAfter(endDate)) {
            status = statusHistory.get(d);
          }
        }

        sortedDates = new ArrayList<>(teamHistory.keySet());
        Collections.sort(sortedDates);
        for (LocalDate d : sortedDates) {
          if (d.isBefore(createdOn)) {
            continue;
          }
          if (!d.isAfter(endDate)) {
            team = teamHistory.get(d);
          }
        }

        // 日付範囲内の日ごとに状態とチームを追跡
        LocalDate currentDate = createdOn.isAfter(startDate) ? createdOn : startDate;
        while (!currentDate.isAfter(endDate)) {
          // ステータス更新
          if (statusHistory.containsKey(currentDate)) {
            status = statusHistory.get(currentDate);
          }
          // チーム更新
          if (teamHistory.containsKey(currentDate)) {
            team = teamHistory.get(currentDate);
          }

          // 未完了かどうか
          boolean isIncomplete = INCOMPLETE_STATUSES.contains(status);
          if (isIncomplete) {
            // 総計に追加
            TeamStats stats = aggregation.get(currentDate).get(team);
            stats.total += 1;

            // 増加の判定
            // チケットがこの日に未完了状態になった場合
            boolean becameIncompleteToday = false;
            // 前日に未完了でなかった場合
            if (currentDate.equals(startDate)) {
              // 作成日に未完了であれば増加
              becameIncompleteToday = true;
            } else {
              LocalDate previousDate = currentDate.minusDays(1);
              // 前日の状態を取得
              String prevStatus = statusHistory.getOrDefault(previousDate, status);
              becameIncompleteToday = INCOMPLETE_STATUSES.contains(status)
                  && !INCOMPLETE_STATUSES.contains(prevStatus);
            }

            if (becameIncompleteToday) {
              stats.increase += 1;
            }
          }

          currentDate = currentDate.plusDays(1);
        }
      }

      issueRs.close();
      issueStmt.close();

      // SQLiteに出力
      createSQLiteTable(sqliteConn);
      insertAggregation(sqliteConn, aggregation);

      // コミット
      sqliteConn.commit();
      System.out.println("集計が完了し、SQLiteデータベースに出力されました。");

    } catch (Exception e) {
      e.printStackTrace();
      try {
        if (redmineConn != null)
          redmineConn.rollback();
        if (sqliteConn != null)
          sqliteConn.rollback();
      } catch (SQLException se) {
        se.printStackTrace();
      }
    } finally {
      try {
        if (redmineConn != null)
          redmineConn.close();
        if (sqliteConn != null)
          sqliteConn.close();
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }

  // チーム名を全て取得
  private static Set<String> getAllTeams(Connection redmineConn) throws SQLException {
    Set<String> teams = new HashSet<>();
    String teamQuery = "SELECT DISTINCT value FROM journal_details WHERE prop_key = ?";
    PreparedStatement pstmt = redmineConn.prepareStatement(teamQuery);
    pstmt.setInt(1, TEAM_FIELD_KEY);
    ResultSet rs = pstmt.executeQuery();
    while (rs.next()) {
      String team = rs.getString("value");
      if (team != null && !team.isEmpty()) {
        teams.add(team);
      }
    }
    rs.close();
    pstmt.close();
    // 追加で"未設定"を含める
    teams.add("未設定");
    return teams;
  }

  // ステータスマッピングを取得
  private static Map<Integer, String> getStatusMap(Connection redmineConn) throws SQLException {
    Map<Integer, String> statusMap = new HashMap<>();
    String statusQuery = "SELECT id, name FROM issue_statuses";
    Statement stmt = redmineConn.createStatement();
    ResultSet rs = stmt.executeQuery(statusQuery);
    while (rs.next()) {
      int id = rs.getInt("id");
      String name = rs.getString("name");
      statusMap.put(id, name);
    }
    rs.close();
    stmt.close();
    return statusMap;
  }

  // チケットのステータス履歴を取得
  private static Map<LocalDate, String> getStatusHistory(Connection redmineConn, int issueId,
      Map<Integer, String> statusMap) throws SQLException {
    Map<LocalDate, String> statusHistory = new HashMap<>();
    String statusHistoryQuery = "SELECT journals.created_on, journal_details.value " +
        "FROM journal_details " +
        "JOIN journals ON journal_details.journal_id = journals.id " +
        "WHERE journals.journalized_id = ? AND journal_details.prop_key = ? " +
        "ORDER BY journals.created_on ASC";
    PreparedStatement pstmt = redmineConn.prepareStatement(statusHistoryQuery);
    pstmt.setInt(1, issueId);
    pstmt.setInt(2, STATUS_FIELD_KEY);
    ResultSet rs = pstmt.executeQuery();
    while (rs.next()) {
      Timestamp ts = rs.getTimestamp("created_on");
      LocalDate date = ts.toLocalDateTime().toLocalDate();
      String statusIdStr = rs.getString("value");
      int statusId = Integer.parseInt(statusIdStr);
      String status = statusMap.get(statusId);
      statusHistory.put(date, status);
    }
    rs.close();
    pstmt.close();
    return statusHistory;
  }

  // チケットのチーム履歴を取得
  private static Map<LocalDate, String> getTeamHistory(Connection redmineConn, int issueId) throws SQLException {
    Map<LocalDate, String> teamHistory = new HashMap<>();
    String teamHistoryQuery = "SELECT journals.created_on, journal_details.value " +
        "FROM journal_details " +
        "JOIN journals ON journal_details.journal_id = journals.id " +
        "WHERE journals.journalized_id = ? AND journal_details.prop_key = ? " +
        "ORDER BY journals.created_on ASC";
    PreparedStatement pstmt = redmineConn.prepareStatement(teamHistoryQuery);
    pstmt.setInt(1, issueId);
    pstmt.setInt(2, TEAM_FIELD_KEY);
    ResultSet rs = pstmt.executeQuery();
    while (rs.next()) {
      Timestamp ts = rs.getTimestamp("created_on");
      LocalDate date = ts.toLocalDateTime().toLocalDate();
      String team = rs.getString("value");
      teamHistory.put(date, team);
    }
    rs.close();
    pstmt.close();
    return teamHistory;
  }

  // SQLiteのテーブルを作成
  private static void createSQLiteTable(Connection sqliteConn) throws SQLException {
    String createTableSQL = "CREATE TABLE IF NOT EXISTS ticket_aggregation (" +
        "date TEXT, " +
        "team_name TEXT, " +
        "incomplete_increase INTEGER, " +
        "incomplete_total INTEGER, " +
        "PRIMARY KEY (date, team_name)" +
        ");";
    Statement stmt = sqliteConn.createStatement();
    stmt.execute(createTableSQL);
    stmt.close();
  }

  // 集計結果をSQLiteに挿入
  private static void insertAggregation(Connection sqliteConn, Map<LocalDate, Map<String, TeamStats>> aggregation)
      throws SQLException {
    String insertSQL = "INSERT INTO ticket_aggregation (date, team_name, incomplete_increase, incomplete_total) " +
        "VALUES (?, ?, ?, ?) " +
        "ON CONFLICT(date, team_name) DO UPDATE SET " +
        "incomplete_increase=excluded.incomplete_increase, " +
        "incomplete_total=excluded.incomplete_total;";
    PreparedStatement pstmt = sqliteConn.prepareStatement(insertSQL);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    for (Map.Entry<LocalDate, Map<String, TeamStats>> dateEntry : aggregation.entrySet()) {
      String dateStr = dateEntry.getKey().format(formatter);
      for (Map.Entry<String, TeamStats> teamEntry : dateEntry.getValue().entrySet()) {
        String team = teamEntry.getKey();
        TeamStats stats = teamEntry.getValue();
        pstmt.setString(1, dateStr);
        pstmt.setString(2, team);
        pstmt.setInt(3, stats.increase);
        pstmt.setInt(4, stats.total);
        pstmt.addBatch();
      }
    }
    pstmt.executeBatch();
    pstmt.close();
  }

  // チームごとの統計データを保持するクラス
  private static class TeamStats {
    int increase = 0;
    int total = 0;
  }

}
