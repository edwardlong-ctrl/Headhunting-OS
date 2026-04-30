package com.recruitingtransactionos.coreapi;

import com.recruitingtransactionos.coreapi.identityauth.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SecurityConfig.class)
public class CoreApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoreApiApplication.class, args);
  }
}
