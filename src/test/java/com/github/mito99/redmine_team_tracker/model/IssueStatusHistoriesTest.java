package com.github.mito99.redmine_team_tracker.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.github.mito99.redmine_team_tracker.db.RedmineDBEntity.Issue;
import com.github.mito99.redmine_team_tracker.db.RedmineDBEntity.Journal;
import lombok.val;

public class IssueStatusHistoriesTest {

  @Test
  void 一度ステータス変更が行われたケース() {
    val issue = Issue.builder().createdOn("2024-05-07 16:17:35").statusName("新規").build();

    val journals = Arrays.asList(new Journal[] {new Journal("2024-05-16 13:59:02", "対応不要", "新規")});

    // テスト実行
    val histories = new IssueStatusHistories(issue, journals);
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-06")))
        .hasToString("");
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-07")))
        .hasToString("新規");
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-15")))
        .hasToString("新規");
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-16")))
        .hasToString("対応不要");
  }

  @Test
  void 一度もステータス変更が行われなかったケース() {
    val issue = Issue.builder().createdOn("2024-05-07 16:17:35").statusName("新規").build();
    val journals = new ArrayList<Journal>();

    val histories = new IssueStatusHistories(issue, journals);
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-06")))
        .hasToString("");
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-07")))
        .hasToString("新規");
    assertThat(histories.getConfirmedStatusNameAtDate(LocalDate.parse("2024-05-16")))
        .hasToString("新規");
  }
}
