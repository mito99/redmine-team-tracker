package jp.hitachi.mineo.tool.redmine_team_tracker;

import org.junit.jupiter.api.Test;

public class AppMainTest {

  @Test
  void 一部() {
    AppMain.main(
        "redmine-team-tracker",
        "-s", "2024-05-07",
        "-e", "2024-05-08");
  }

}
