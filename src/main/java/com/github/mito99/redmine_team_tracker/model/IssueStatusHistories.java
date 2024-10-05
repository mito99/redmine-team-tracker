package com.github.mito99.redmine_team_tracker.model;

import java.time.LocalDate;
import java.util.List;
import com.github.mito99.redmine_team_tracker.db.RedmineDBEntity.Issue;
import com.github.mito99.redmine_team_tracker.db.RedmineDBEntity.Journal;
import com.github.mito99.redmine_team_tracker.util.FlexibleMap;
import lombok.val;

public class IssueStatusHistories {

  private final Issue issue;
  private final FlexibleMap<String, Journal> journals = new FlexibleMap<>();

  public IssueStatusHistories(Issue issue, List<Journal> journalList) {
    this.issue = issue;
    journalList.forEach(j -> {
      this.journals.put(j.getCreatedOn(), j);
    });
  }

  public IssueStatus getStatusName(String currentDate) {
    val journal = journals.getNearestOrEqualValue(currentDate);
    if (journal != null) {
      return new IssueStatus(journal.getNewValue());
    }

    if (issue.getCreatedOn().compareTo(currentDate) < 0) {
      return new IssueStatus(issue.getStatusName());
    }
    return new IssueStatus("");
  }

  public IssueStatus getConfirmedStatusNameAtDate(LocalDate currentDate) {
    val dateTime = currentDate.toString() + " 23:59:59";
    return getStatusName(dateTime);
  }
}
