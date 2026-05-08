package com.recruitingtransactionos.coreapi.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DeploymentEnvironmentConfigurationTest {

  @Test
  void activeProfilePrefersProductionWhenBothDeploymentProfilesAreActive() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("staging", "production");

    assertThat(DeploymentEnvironmentConfiguration.activeProfile(environment)).isEqualTo("production");
  }
}
