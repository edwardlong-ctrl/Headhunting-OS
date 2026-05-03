package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Task 30 company-name generalization.
 *
 * <p>Maps a known exact company name (mostly semiconductor + adjacent tech
 * employers because that is the v1 industry pack focus) to a generalized
 * label such as {@code "Top semiconductor foundry"} that is safe to surface
 * in an anonymous client-safe summary.
 *
 * <p>This is intentionally a small, deterministic, hand-curated rule set.
 * v1 prioritizes the acceptance scenario in the productization roadmap:
 * "Top chip company + unique title + exact year + chip code name" must be
 * generalized or blocked before client display. Future tasks may move this
 * mapping behind an industry pack and add additional entries; for now this
 * is the minimum baseline that the regression tests rely on.
 *
 * <p>The class is pure — no Spring, no I/O, no logging side-effects — so it
 * may be reused from any layer (service, projection, audit, tests).
 */
public final class CompanyNameGeneralizationPolicy {

  /**
   * Generalization label returned when an exact company name is matched.
   */
  public record Generalization(
      String generalizedLabel,
      CompanyGeneralizationCategory category) {

    public Generalization {
      generalizedLabel = ClientSafeProjectionGuards.requireNonBlank(
          generalizedLabel, "generalizedLabel");
      Objects.requireNonNull(category, "category must not be null");
    }
  }

  /**
   * Category of the generalized label, used for explainability and audit.
   */
  public enum CompanyGeneralizationCategory {
    SEMICONDUCTOR_FOUNDRY,
    SEMICONDUCTOR_DESIGN,
    SEMICONDUCTOR_FABLESS,
    SEMICONDUCTOR_EQUIPMENT,
    SEMICONDUCTOR_IP,
    AI_CHIP_COMPANY,
    GPU_COMPANY,
    CONSUMER_ELECTRONICS,
    CLOUD_HYPERSCALER,
    GENERIC_TOP_TECH;
  }

  /**
   * Hand-curated exact-name → generalized-label mapping.
   *
   * <p>Keys are stored as lowercase strings; lookup is case-insensitive after
   * stripping. Common informal aliases are listed as separate keys so that a
   * resume that says either "TSMC" or "Taiwan Semiconductor" is generalized
   * the same way.
   *
   * <p>This list is intentionally small. It must NOT be treated as the
   * complete universe of "top chip companies"; any company not in this list
   * still has its raw name removed/redacted by the broader pipeline (see
   * {@code ClientSafeSummaryPipeline}). The policy here only adds a positive
   * generalized label when one is known with confidence.
   */
  private static final Map<String, Generalization> GENERALIZATIONS =
      buildGeneralizations();

  private CompanyNameGeneralizationPolicy() {
  }

  public static Generalization generalize(String exactCompanyName) {
    String normalized = normalize(exactCompanyName);
    if (normalized.isEmpty()) {
      return null;
    }
    Generalization direct = GENERALIZATIONS.get(normalized);
    if (direct != null) {
      return direct;
    }
    // Try a soft, conservative substring match against the curated keys so
    // that "Taiwan Semiconductor Manufacturing Company Limited" still matches
    // the curated "taiwan semiconductor". This intentionally only matches
    // when the full curated key appears as a whole-word region of the input.
    for (Map.Entry<String, Generalization> entry : GENERALIZATIONS.entrySet()) {
      String key = entry.getKey();
      if (key.length() < 4) {
        // Avoid overly aggressive matching on very short keys.
        continue;
      }
      if (containsWholeWord(normalized, key)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Returns true when the exact company name is a known top-tier name that
   * by itself is enough to constitute a re-identification risk when paired
   * with a rare title or exact year.
   */
  public static boolean isKnownTopTierCompany(String exactCompanyName) {
    return generalize(exactCompanyName) != null;
  }

  static Map<String, Generalization> registeredGeneralizationsForTesting() {
    return Map.copyOf(GENERALIZATIONS);
  }

  private static Map<String, Generalization> buildGeneralizations() {
    LinkedHashMap<String, Generalization> map = new LinkedHashMap<>();

    // Semiconductor foundries.
    addEntry(map, "tsmc",
        "Top semiconductor foundry",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY);
    addEntry(map, "taiwan semiconductor",
        "Top semiconductor foundry",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY);
    addEntry(map, "smic",
        "Top semiconductor foundry",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY);
    addEntry(map, "samsung foundry",
        "Top semiconductor foundry",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY);
    addEntry(map, "globalfoundries",
        "Top semiconductor foundry",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY);
    addEntry(map, "umc",
        "Top semiconductor foundry",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY);

    // Integrated semiconductor / design houses.
    addEntry(map, "intel",
        "Top integrated semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_DESIGN);
    addEntry(map, "samsung electronics",
        "Top integrated semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_DESIGN);
    addEntry(map, "samsung",
        "Top integrated semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_DESIGN);
    addEntry(map, "sk hynix",
        "Top memory semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_DESIGN);
    addEntry(map, "micron",
        "Top memory semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_DESIGN);

    // Fabless / chip design.
    addEntry(map, "qualcomm",
        "Top wireless chip design company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FABLESS);
    addEntry(map, "mediatek",
        "Top wireless chip design company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FABLESS);
    addEntry(map, "broadcom",
        "Top fabless semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FABLESS);
    addEntry(map, "marvell",
        "Top fabless semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FABLESS);
    addEntry(map, "amd",
        "Top fabless semiconductor company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_FABLESS);

    // GPU / AI accelerator.
    addEntry(map, "nvidia",
        "Top GPU and AI accelerator company",
        CompanyGeneralizationCategory.GPU_COMPANY);
    addEntry(map, "graphcore",
        "Top AI accelerator company",
        CompanyGeneralizationCategory.AI_CHIP_COMPANY);
    addEntry(map, "cerebras",
        "Top AI accelerator company",
        CompanyGeneralizationCategory.AI_CHIP_COMPANY);
    addEntry(map, "groq",
        "Top AI accelerator company",
        CompanyGeneralizationCategory.AI_CHIP_COMPANY);
    addEntry(map, "huawei hisilicon",
        "Top AI and wireless chip company",
        CompanyGeneralizationCategory.AI_CHIP_COMPANY);
    addEntry(map, "hisilicon",
        "Top AI and wireless chip company",
        CompanyGeneralizationCategory.AI_CHIP_COMPANY);
    addEntry(map, "cambricon",
        "Top AI accelerator company",
        CompanyGeneralizationCategory.AI_CHIP_COMPANY);

    // Semiconductor equipment.
    addEntry(map, "asml",
        "Top semiconductor equipment supplier",
        CompanyGeneralizationCategory.SEMICONDUCTOR_EQUIPMENT);
    addEntry(map, "applied materials",
        "Top semiconductor equipment supplier",
        CompanyGeneralizationCategory.SEMICONDUCTOR_EQUIPMENT);
    addEntry(map, "lam research",
        "Top semiconductor equipment supplier",
        CompanyGeneralizationCategory.SEMICONDUCTOR_EQUIPMENT);
    addEntry(map, "kla",
        "Top semiconductor equipment supplier",
        CompanyGeneralizationCategory.SEMICONDUCTOR_EQUIPMENT);

    // Semiconductor IP.
    addEntry(map, "arm",
        "Top semiconductor IP company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_IP);
    addEntry(map, "synopsys",
        "Top EDA and IP company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_IP);
    addEntry(map, "cadence",
        "Top EDA and IP company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_IP);

    // Consumer electronics with internal chip teams.
    addEntry(map, "apple",
        "Top consumer electronics company with in-house silicon",
        CompanyGeneralizationCategory.CONSUMER_ELECTRONICS);
    addEntry(map, "huawei",
        "Top consumer electronics company with in-house silicon",
        CompanyGeneralizationCategory.CONSUMER_ELECTRONICS);
    addEntry(map, "xiaomi",
        "Top consumer electronics company with in-house silicon",
        CompanyGeneralizationCategory.CONSUMER_ELECTRONICS);

    // Cloud hyperscalers with custom silicon teams.
    addEntry(map, "google",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "alphabet",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "amazon",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "amazon web services",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "aws",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "microsoft",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "meta",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);
    addEntry(map, "facebook",
        "Top cloud hyperscaler with custom silicon",
        CompanyGeneralizationCategory.CLOUD_HYPERSCALER);

    // Synthetic test placeholder used by integration tests so they do not
    // need to ship a real third-party brand name in test data.
    addEntry(map, "nebulachip systems",
        "Top semiconductor design company",
        CompanyGeneralizationCategory.SEMICONDUCTOR_DESIGN);

    return Map.copyOf(map);
  }

  private static void addEntry(
      LinkedHashMap<String, Generalization> map,
      String exactName,
      String generalizedLabel,
      CompanyGeneralizationCategory category) {
    map.put(
        normalize(exactName),
        new Generalization(generalizedLabel, category));
  }

  private static String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.strip().toLowerCase(Locale.ROOT);
  }

  private static boolean containsWholeWord(String haystack, String needle) {
    int index = haystack.indexOf(needle);
    while (index >= 0) {
      boolean leftBoundary = index == 0
          || !Character.isLetterOrDigit(haystack.charAt(index - 1));
      int rightIndex = index + needle.length();
      boolean rightBoundary = rightIndex == haystack.length()
          || !Character.isLetterOrDigit(haystack.charAt(rightIndex));
      if (leftBoundary && rightBoundary) {
        return true;
      }
      index = haystack.indexOf(needle, index + 1);
    }
    return false;
  }
}
