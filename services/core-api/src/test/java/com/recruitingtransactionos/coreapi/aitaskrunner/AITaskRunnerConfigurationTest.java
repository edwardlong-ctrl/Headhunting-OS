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

  @Test
  void candidateProfileParserInputSchemaAllowsNullOptionalTextChannels() throws Exception {
    AITaskRunnerConfiguration configuration = new AITaskRunnerConfiguration();
    AITaskDefinitionRegistry registry = configuration.aiTaskDefinitionRegistry();
    AITaskSchemaValidator schemaValidator = configuration.aiTaskSchemaValidator(OBJECT_MAPPER);

    AITaskDefinition candidateDefinition = registry.findRequired(
        "candidate-profile-parser",
        "candidate-profile-parser.v1");

    schemaValidator.validate(
        candidateDefinition.inputSchemaResourcePath(),
        OBJECT_MAPPER.readTree("""
            {
              "sourceSummary": "Uploaded consultant CV text",
              "resumeText": "Skills: SystemVerilog, UVM, PCIe",
              "linkedInText": null,
              "portfolioText": null,
              "consultantNotes": null
            }
            """),
        "input");
  }

  @Test
  void deterministicPilotProviderReturnsSchemaValidOutputsForLocalE2E() throws Exception {
    AITaskRunnerConfiguration configuration = new AITaskRunnerConfiguration();
    AITaskDefinitionRegistry registry = configuration.aiTaskDefinitionRegistry();
    AITaskPromptRegistry promptRegistry = configuration.aiTaskPromptRegistry();
    AITaskSchemaValidator schemaValidator = configuration.aiTaskSchemaValidator(OBJECT_MAPPER);
    AITaskProvider provider = configuration.deterministicPilotAITaskProvider(OBJECT_MAPPER);

    assertThat(provider.providerKey()).isEqualTo("deterministic");

    AITaskDefinition candidateDefinition = registry.findRequired(
        "candidate-profile-parser",
        "candidate-profile-parser.v1");
    AITaskProviderResponse candidateResponse = provider.execute(new AITaskProviderRequest(
        "candidate-profile-parser",
        candidateDefinition.promptVersion(),
        "pilot-local-v1",
        promptRegistry.loadPrompt(candidateDefinition),
        OBJECT_MAPPER.readTree("""
            {
              "sourceSummary": "candidate resume upload",
              "resumeText": "Role family: ASIC verification\\nSkills: SystemVerilog, UVM, PCIe, low-power verification\\nLocation: Shanghai",
              "linkedInText": null,
              "portfolioText": null,
              "consultantNotes": null
            }
            """)));
    schemaValidator.validate(
        candidateDefinition.outputSchemaResourcePath(),
        candidateResponse.outputPayload(),
        "output");

    AITaskDefinition authenticityDefinition = registry.findRequired(
        "authenticity-risk-assessor",
        "authenticity-risk-assessor.v1");
    AITaskProviderResponse authenticityResponse = provider.execute(new AITaskProviderRequest(
        "authenticity-risk-assessor",
        authenticityDefinition.promptVersion(),
        "pilot-local-v1",
        promptRegistry.loadPrompt(authenticityDefinition),
        OBJECT_MAPPER.readTree("""
            {"resumeText":"UVM verification evidence","linkedInText":null,"portfolioText":null,"interviewNotesText":null}
            """)));
    schemaValidator.validate(
        authenticityDefinition.outputSchemaResourcePath(),
        authenticityResponse.outputPayload(),
        "output");

    AITaskDefinition feedbackDefinition = registry.findRequired(
        "interview-feedback-structurer",
        "interview-feedback-structurer.v1");
    AITaskProviderResponse feedbackResponse = provider.execute(new AITaskProviderRequest(
        "interview-feedback-structurer",
        feedbackDefinition.promptVersion(),
        "pilot-local-v1",
        promptRegistry.loadPrompt(feedbackDefinition),
        OBJECT_MAPPER.readTree("""
            {
              "decision":"hold",
              "outcome":"yes",
              "rejectReasonTaxonomy":null,
              "interviewerName":"Client interviewer",
              "interviewerRole":"Engineering Manager",
              "interviewRound":1,
              "interviewDate":"2026-05-09T00:00:00Z",
              "ratings":"{}",
              "strengths":"Strong technical evidence.",
              "concerns":"Compensation expectation needs role-specific review.",
              "notes":"Strong technical evidence, but compensation expectation needs review.",
              "scorecardSnapshot":"{}",
              "sourceEvidence":["Strong technical evidence, but compensation expectation needs review."]
            }
            """)));
    schemaValidator.validate(
        feedbackDefinition.outputSchemaResourcePath(),
        feedbackResponse.outputPayload(),
        "output");
    assertThat(feedbackResponse.outputPayload().path("outcomeLabel").asText())
        .isEqualTo("compensation_mismatch");
  }
}
