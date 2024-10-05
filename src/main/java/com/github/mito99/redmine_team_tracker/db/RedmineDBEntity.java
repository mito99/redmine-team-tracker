package com.github.mito99.redmine_team_tracker.db;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

public class RedmineDBEntity {

  @Getter
  @Builder
  @RequiredArgsConstructor
  public static class Issue {
    private final int id;
    private final String createdOn;
    private final String statusId;
    private final String statusName;
    private final String teamName;

    public LocalDateTime getCreatedOnLocalDateTime() {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      return LocalDateTime.parse(createdOn, formatter);
    }

    public LocalDate getCreatedOnLocalDate() {
      return getCreatedOnLocalDateTime().toLocalDate();
    }
  }

  @Getter
  @Builder
  @RequiredArgsConstructor
  public static class Journal {
    private final String createdOn;
    private final String newValue;
    private final String oldValue;
  }
}
