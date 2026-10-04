package vn.edufit;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Lớp khởi chạy ứng dụng chính (Application Entrypoint) của EduFit
 * 
 * Quy chuẩn thời gian (NFR-18):
 * - Toàn bộ hệ thống Backend lưu trữ và xử lý thời gian theo chuẩn UTC (timestamptz).
 * - Thiết lập TimeZone mặc định của JVM là UTC giúp tránh lỗi tương thích múi giờ
 *   trên Windows (`FATAL: invalid value for parameter "TimeZone": "Asia/Saigon"` khi kết nối PostgreSQL).
 */
@SpringBootApplication(scanBasePackages = "vn.edufit")
@ConfigurationPropertiesScan(basePackages = "vn.edufit")
public class EduFitApplication {

  static {
    // Đồng bộ múi giờ JVM sang UTC theo chuẩn NFR-18
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(EduFitApplication.class, args);
  }
}
