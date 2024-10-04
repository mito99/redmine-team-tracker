package jp.hitachi.mineo.tool.redmine_team_tracker.model;

import java.time.LocalDate;
import java.util.List;

import jp.hitachi.mineo.tool.redmine_team_tracker.db.RedmineDBEntity.Issue;
import jp.hitachi.mineo.tool.redmine_team_tracker.db.RedmineDBEntity.Journal;
import jp.hitachi.mineo.tool.redmine_team_tracker.util.FlexibleMap;
import lombok.val;

public class IssueTeamHistories {
  private final Issue issue;
  private final FlexibleMap<String, Journal> journals = new FlexibleMap<>();

  public IssueTeamHistories(Issue issue, List<Journal> journalList) {
    this.issue = issue;
    journalList.forEach(j -> {
      this.journals.put(j.getCreatedOn(), j);
    });
  }

  public String getTeamName(String currentDate) {
    val journal = journals.getNearestOrEqualValue(currentDate);
    if (journal != null) {
      return journal.getNewValue();
    }

    if (issue.getCreatedOn().compareTo(currentDate) < 0) {
      return issue.getTeamName();
    }
    return "";
  }

  public String getConfirmedTeamNameAtDate(LocalDate currentDate) {
    val dateTime = currentDate.toString() + " 23:59:59";
    return getTeamName(dateTime);
  }

  public String getAssignedTeamOnDate(LocalDate targetDate) {
    val startDateTime = targetDate.toString() + " 00:00:00";
    val endDateTime = targetDate.toString() + " 23:59:59";
    val values = journals.getValuesInRange(startDateTime, endDateTime);
    if (values.isEmpty()) {
      val issueCreateDateTime = issue.getCreatedOnLocalDate();
      val equalDate = issueCreateDateTime.isEqual(targetDate);
      return equalDate ? issue.getTeamName() : "";
    }
    return values.get(values.size() - 1).getNewValue();
  }
}
