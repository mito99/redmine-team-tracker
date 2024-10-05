package com.github.mito99.redmine_team_tracker;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import com.github.mito99.redmine_team_tracker.db.RedmineDB;
import com.github.mito99.redmine_team_tracker.db.SQLiteDB;
import com.github.mito99.redmine_team_tracker.model.Aggregation;
import com.github.mito99.redmine_team_tracker.model.IssueStatusHistories;
import com.github.mito99.redmine_team_tracker.model.IssueTeamHistories;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Cleanup;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(name = "redmine-team-tracker", description = "トラッカー")
public class RedmineTeamTrackerCommand implements Callable<Integer> {

  @Option(names = {"-s", "--start-date"}, description = "集計範囲開始(yyyy-mm-dd)", required = true)
  private String startDateString;

  @Option(names = {"-e", "--end-date"}, description = "集計範囲終了(yyyy-mm-dd)", required = true)
  private String endDateString;

  private final Dotenv dotenv;

  public RedmineTeamTrackerCommand() {
    this.dotenv = Dotenv.configure().directory(".env").load();
  }

  @Override
  public Integer call() throws Exception {

    val startDate = LocalDate.parse(startDateString);
    val endDate = LocalDate.parse(endDateString);

    val aggregation = new Aggregation();

    @Cleanup
    val redmineCon = RedmineDB.getConnection(dotenv.get("REDMINE_JDBC_URL"),
        dotenv.get("REDMINE_DB_USER"), dotenv.get("REDMINE_DB_PASSWORD"));
    val issues = RedmineDB.getTargetIssue(redmineCon);
    issues.forEach(issue -> {
      val ticketId = issue.getId();
      log.debug("ticketId={}", ticketId);

      val statusJournals = RedmineDB.getStatusJornal(redmineCon, ticketId);
      val statusHistories = new IssueStatusHistories(issue, statusJournals);

      val teamJournals = RedmineDB.getTeamJornal(redmineCon, ticketId);
      val teamHistories = new IssueTeamHistories(issue, teamJournals);

      Stream.iterate(startDate, date -> date.plusDays(1))
          .limit(ChronoUnit.DAYS.between(startDate, endDate) + 1).forEach(currentDate -> {

            val status = statusHistories.getConfirmedStatusNameAtDate(currentDate);
            if (!status.isIncomplete()) {
              return;
            }

            val cTeamName = teamHistories.getConfirmedTeamNameAtDate(currentDate);
            if (!cTeamName.isEmpty()) {
              val teamStats = aggregation.get(currentDate, cTeamName);
              teamStats.incrementTotal();
            }

            val aTeamName = teamHistories.getAssignedTeamOnDate(currentDate);
            if (!aTeamName.isEmpty()) {
              val teamStats = aggregation.get(currentDate, aTeamName);
              teamStats.incrementIncrease();
            }
          });
    });

    @Cleanup
    val sqliteCon = SQLiteDB.getConnection(dotenv.get("SQLITE_DB_URL"));
    SQLiteDB.createSQLiteTable(sqliteCon);
    SQLiteDB.insertAggregation(sqliteCon, aggregation);
    sqliteCon.commit();
    System.out.println("集計が完了し、SQLiteデータベースに出力されました。");

    return 0;
  }

}
