package vn.edufit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "vn.edufit")
@ConfigurationPropertiesScan(basePackages = "vn.edufit")
public class EduFitApplication {

  public static void main(String[] args) {
    SpringApplication.run(EduFitApplication.class, args);
  }
}
