package com.recruitingtransactionos.coreapi.aitaskrunner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AITaskRunnerConfigurationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void registryIncludesCompanyAndJobIntakeDefinitionsWithLoadableResources() throws Exception {
    AITaskRunnerConfiguration configuration = new AITaskRunnerConfiguration();
    AITaskDefinitionRegistry registry = configuration.aiTaskDefinitionRegistry();
    AITaskPromptRegistry promptRegistry = configuration.aiTaskPromptRegistry();
    AITaskSchemaValidator schemaValidator = configuration.aiTaskSchemaValidator(OBJECT_MAPPER);

    AITaskDefinition companyDefinition = registry.findRequired("company-intake", "company-intake.v1");
    AITaskDefinition jobDefinition = registry.findRequired("job-intake", "job-intake.v1");

    assertThat(promptRegistry.loadPrompt(companyDefinition)).contains("Company Intake Parser");
    assertThat(promptRegistry.loadPrompt(jobDefinition)).contains("Job Intake Parser");

    schemaValidator.validate(
        companyDefinition.inputSchemaResourcePath(),
        OBJECT_MAPPER.readTree("""
            {"sourceSummary":"Series B infrastructure hiring","consultantNotes":"Need validated market context"}
            """),
        "input");
    schemaValidator.validate(
        companyDefinition.outputSchemaResourcePath(),
        OBJECT_MAPPER.readTree("""
            {
              "normalizedSummary":"Cloud infrastructure company with active hiring pressure.",
              "claimCandidates":[
                {
                  "fieldName":"company.industry",
                  "fieldValue":"Cloud infrastructure",
                  "rationale":"Directly stated in the briefing materials.",
                  "evidenceQuote":"cloud infrastructure"
                }
              ],
              "followUpQuestions":["What is the exact team headcount for this hiring group?"]
            }
            """),
        "output");
    schemaValidator.validate(
        jobDefinition.inputSchemaResourcePath(),
        OBJECT_MAPPER.readTree("""
            {"jobDescriptionText":"Senior backend engineer building workflow systems"}
            """),
        "input");
    schemaValidator.validate(
        jobDefinition.outputSchemaResourcePath(),
        OBJECT_MAPPER.readTree("""
            {
              "roleSummary":"Senior backend role focused on workflow systems.",
              "claimCandidates":[
                {
                  "fieldName":"job.title",
                  "fieldValue":"Senior Backend Engineer",
                  "rationale":"Title appears explicitly in the job description.",
                  "evidenceQuote":"Senior backend engineer"
                }
              ],
              "followUpQuestions":["Which requirements are truly must-have versus trainable?"]
            }
            """),
        "output");
  }
}
