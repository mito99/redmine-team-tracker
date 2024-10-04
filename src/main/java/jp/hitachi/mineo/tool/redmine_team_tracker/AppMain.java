package jp.hitachi.mineo.tool.redmine_team_tracker;

import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "", mixinStandardHelpOptions = true, description = "Redmineチケットの集計", subcommands = {
    RedmineTeamTrackerCommand.class
})
public class AppMain implements Runnable {

  public static void main(String... args) {
    int exitCode = new CommandLine(new AppMain()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    val commands = Arrays.asList(
        "redmine-team-tracker");
    System.out.println("Use one of the subcommands: " +
        commands.stream().collect(Collectors.joining(" or ")));
  }
}
