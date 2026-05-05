package com.recruitingtransactionos.coreapi;

import com.recruitingtransactionos.coreapi.identityauth.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import(SecurityConfig.class)
public class CoreApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoreApiApplication.class, args);
  }
}
