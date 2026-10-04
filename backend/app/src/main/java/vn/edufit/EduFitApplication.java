package vn.edufit;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "vn.edufit")
@ConfigurationPropertiesScan(basePackages = "vn.edufit")
public class EduFitApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    SpringApplication.run(EduFitApplication.class, args);
  }
}
