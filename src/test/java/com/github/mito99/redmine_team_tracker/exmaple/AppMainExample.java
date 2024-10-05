package com.github.mito99.redmine_team_tracker.exmaple;

import org.junit.jupiter.api.Test;
import com.github.mito99.redmine_team_tracker.AppMain;

public class AppMainExample {

  @Test
  void 一部() {
    AppMain.main("redmine-team-tracker", "-s", "2024-05-07", "-e", "2024-05-08");
  }

  @Test
  void 全部() {
    AppMain.main("redmine-team-tracker", "-s", "2017-02-06", "-e", "2024-07-22");
  }
}
