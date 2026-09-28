package vn.edufit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Cấu hình thực thi các tác vụ lập lịch ngầm tự động (Scheduled Background Tasks).
 *
 * <p>Mục đích thiết kế và liên kết yêu cầu chức năng (FR) trong SRS EduFit:
 * <ul>
 *   <li><b>FR-17 (Hết hạn đề xuất tự động):</b> Đề xuất buổi học (Session Proposal) nếu sau 48 giờ
 *       không được phản hồi sẽ tự động chuyển sang trạng thái {@code EXPIRED} bởi job ngầm.</li>
 *   <li><b>FR-20 / FR-31 (Nhắc nhở buổi học):</b> Tự động quét và kích hoạt thông báo nhắc lịch học
 *       trước 24 giờ và 2 giờ qua hệ thống thông báo.</li>
 *   <li><b>Tách biệt Thread Pool:</b> Cấu hình riêng một Task Scheduler với pool size hợp lý để các tác vụ ngầm
 *       không làm chiếm dụng luồng phục vụ HTTP Request chính của người dùng.</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(5);
    scheduler.setThreadNamePrefix("edufit-scheduler-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.initialize();
    return scheduler;
  }
}
