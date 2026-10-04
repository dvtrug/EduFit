package vn.edufit;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Lớp khởi chạy ứng dụng chính (Application Entrypoint) của EduFit
 * 
 * Quy chuẩn thời gian (NFR-18):
 * - Thiết lập TimeZone mặc định là Asia/Ho_Chi_Minh cho PostgreSQL 17 compatibility.
 */
@SpringBootApplication(scanBasePackages = "vn.edufit")
@ConfigurationPropertiesScan(basePackages = "vn.edufit")
public class EduFitApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    SpringApplication.run(EduFitApplication.class, args);
  }
}
