package jp.hitachi.mineo.tool.redmine_team_tracker.model;

import static jp.hitachi.mineo.tool.redmine_team_tracker.util.ThrowingConsumer.tryConsumer;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jp.hitachi.mineo.tool.redmine_team_tracker.util.ThrowingConsumer;
import lombok.Builder;
import lombok.Getter;
import lombok.val;

public class Aggregation {

  private final Map<String, TeamStats> values;

  public Aggregation() {
    this.values = new HashMap<>();
  }

  public Stream<TeamStats> stream() {
    return values.values().stream();
  }

  public void forEach(ThrowingConsumer<TeamStats, Exception> c) {
    values.values().stream().forEach(tryConsumer(c::accept));
  }

  public TeamStats get(LocalDate date, String teamName) {
    val dateStr = date.toString();
    val key = dateStr + "@" + teamName;
    values.putIfAbsent(key, TeamStats.builder()
        .date(date)
        .teamName(teamName)
        .build());
    return values.get(key);
  }

  @Getter
  public static class TeamStats {
    private final LocalDate date;
    private final String teamName;
    private Integer increase = 0;
    private Integer total = 0;

    @Builder
    TeamStats(LocalDate date, String teamName) {
      this.date = date;
      this.teamName = teamName;
    }

    public void incrementIncrease() {
      this.increase += 1;
    }

    public void incrementTotal() {
      this.total += 1;
    }
  }
}
