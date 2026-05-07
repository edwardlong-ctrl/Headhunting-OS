package com.recruitingtransactionos.coreapi.pilotdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PilotDataCommandTest {

  @Test
  void parsesSupportedCommands() {
    assertThat(PilotDataCommand.parse("rebuild")).isEqualTo(PilotDataCommand.REBUILD);
    assertThat(PilotDataCommand.parse("reset")).isEqualTo(PilotDataCommand.RESET);
    assertThat(PilotDataCommand.parse("import")).isEqualTo(PilotDataCommand.IMPORT);
    assertThat(PilotDataCommand.parse("validate")).isEqualTo(PilotDataCommand.VALIDATE);
    assertThat(PilotDataCommand.parse("export")).isEqualTo(PilotDataCommand.EXPORT);
  }

  @Test
  void rejectsUnknownCommands() {
    assertThatThrownBy(() -> PilotDataCommand.parse("seed-shortcut"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported pilot data command");
  }

  @Test
  void resetRequiresExplicitAllowFlag() {
    PilotDataCommandOptions resetWithoutFlag = new PilotDataCommandOptions(PilotDataCommand.RESET, false);
    PilotDataCommandOptions resetWithFlag = new PilotDataCommandOptions(PilotDataCommand.RESET, true);

    assertThatThrownBy(resetWithoutFlag::requireResetAllowed)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("pilot_data_reset_requires_RTO_PILOT_DATA_ALLOW_RESET_true");
    resetWithFlag.requireResetAllowed();
  }

  @Test
  void rebuildDoesNotRequireExplicitAllowFlagBecauseItIsTheDocumentedOneCommandSeedRebuild() {
    PilotDataCommandOptions rebuildWithoutFlag = new PilotDataCommandOptions(PilotDataCommand.REBUILD, false);

    rebuildWithoutFlag.requireResetAllowed();
  }

  @Test
  void pilotDataProductionCodeOnlyUsesDirectSqlForIdentityBootstrapAndResetCleanup()
      throws IOException {
    Pattern writePattern = Pattern.compile(
        "(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\s+([a-z_]+)\\.([a-z_]+)\\b",
        Pattern.CASE_INSENSITIVE);
    List<String> disallowedWrites = new ArrayList<>();

    for (Path file : Files.walk(Path.of("src/main/java/com/recruitingtransactionos/coreapi/pilotdata"))
        .filter(path -> path.toString().endsWith(".java"))
        .toList()) {
      Matcher matcher = writePattern.matcher(Files.readString(file));
      while (matcher.find()) {
        String schema = matcher.group(2);
        String table = matcher.group(3);
        boolean identityBootstrap = schema.equals("identity")
            && List.of("organization", "user_account", "role_assignment").contains(table);
        boolean resetCleanup = matcher.group(1).toUpperCase().startsWith("DELETE");
        boolean resetCurrentProfileBreak = matcher.group().contains("UPDATE recruiting.candidate");
        boolean resetReviewCycleBreak = matcher.group().contains("UPDATE governance.claim_ledger_item");
        if (!identityBootstrap && !resetCleanup && !resetCurrentProfileBreak && !resetReviewCycleBreak) {
          disallowedWrites.add(file + " contains " + matcher.group());
        }
      }
    }

    assertThat(disallowedWrites).isEmpty();
  }

  @Test
  void resetCleanupTableListIncludesEveryOrganizationScopedTableFromMigrations()
      throws IOException {
    Pattern createTablePattern = Pattern.compile(
        "CREATE TABLE\\s+([a-z_]+\\.[a-z_]+)\\s*\\((.*?)(?:\\n\\);|\\);)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Set<String> organizationScopedTables = new LinkedHashSet<>();

    for (Path file : Files.walk(Path.of("src/main/resources/db/migration"))
        .filter(path -> path.toString().endsWith(".sql"))
        .sorted()
        .toList()) {
      Matcher matcher = createTablePattern.matcher(Files.readString(file));
      while (matcher.find()) {
        if (matcher.group(2).matches("(?is).*\\borganization_id\\b.*")) {
          organizationScopedTables.add(matcher.group(1));
        }
      }
    }

    String pilotDataService = Files.readString(
        Path.of("src/main/java/com/recruitingtransactionos/coreapi/pilotdata/PilotDataService.java"));
    Matcher resetTablesMatcher = Pattern.compile(
        "String\\[\\] tables = \\{(.*?)\\};",
        Pattern.DOTALL).matcher(pilotDataService);
    assertThat(resetTablesMatcher.find()).isTrue();
    Matcher tableMatcher = Pattern.compile("\"([a-z_]+\\.[a-z_]+)\"")
        .matcher(resetTablesMatcher.group(1));
    Set<String> resetTables = new LinkedHashSet<>();
    while (tableMatcher.find()) {
      resetTables.add(tableMatcher.group(1));
    }

    assertThat(resetTables).containsAll(organizationScopedTables);
  }
}
