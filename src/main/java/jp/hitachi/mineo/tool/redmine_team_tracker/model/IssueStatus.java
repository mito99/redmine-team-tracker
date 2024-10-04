package jp.hitachi.mineo.tool.redmine_team_tracker.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IssueStatus {

  private static Set<String> INCOMPLETE_STATUSES = new HashSet<>(
      Arrays.asList("新規", "調査依頼", "調査中"));

  private final String value;

  public IssueStatus(String v) {
    this.value = v;
  }

  public boolean isIncomplete() {
    return INCOMPLETE_STATUSES.contains(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
