package jp.hitachi.mineo.tool.redmine_team_tracker.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jp.hitachi.mineo.tool.redmine_team_tracker.db.RedmineDBEntity.Issue;
import jp.hitachi.mineo.tool.redmine_team_tracker.db.RedmineDBEntity.Journal;
import lombok.val;

public class IssueTeamHistoriesTest {

  @Test
  void 当日担当となったチーム名が取得できるか() {
    val issue = Issue.builder()
        .createdOn("2024-05-07 16:17:35")
        .teamName("Aチーム")
        .build();

    val journals = Arrays.asList(new Journal[] {
        new Journal("2024-05-16 13:59:02", "Bチーム", "Aチーム"),
        new Journal("2024-05-16 17:59:02", "Cチーム", "Bチーム")
    });

    // テスト実行
    val h = new IssueTeamHistories(issue, journals);
    assertThat(h.getAssignedTeamOnDate(LocalDate.parse("2024-05-06"))).isEqualTo("");
    assertThat(h.getAssignedTeamOnDate(LocalDate.parse("2024-05-07"))).isEqualTo("Aチーム");
    assertThat(h.getAssignedTeamOnDate(LocalDate.parse("2024-05-08"))).isEqualTo("");
    assertThat(h.getAssignedTeamOnDate(LocalDate.parse("2024-05-16"))).isEqualTo("Cチーム");

  }
}
