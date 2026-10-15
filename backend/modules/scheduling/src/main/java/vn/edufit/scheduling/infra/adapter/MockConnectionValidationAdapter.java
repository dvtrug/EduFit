package vn.edufit.scheduling.infra.adapter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.edufit.scheduling.application.port.ConnectionValidationPort;

/**
 * Mock Outbound Adapter cho {@link ConnectionValidationPort}.
 *
 * <p>Mục đích kiến trúc (ADR-002 Decoupled Development):
 * Cho phép phân hệ {@code scheduling} hoạt động, phát triển và chạy test hoàn toàn độc lập
 * trong khi phân hệ kết nối (Connection / Engagement) đang trong quá trình hoàn thiện.
 *
 * <p>Được kích hoạt mặc định qua thuộc tính cấu hình {@code edufit.scheduling.use-mock-connection=true}
 * (hoặc khi thuộc tính này chưa được khai báo nhờ {@code matchIfMissing = true}).
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "edufit.scheduling",
    name = "use-mock-connection",
    havingValue = "true",
    matchIfMissing = true
)
public class MockConnectionValidationAdapter implements ConnectionValidationPort {

  @Override
  public boolean isConnectionActive(UUID classId, UUID tutorId, UUID studentId) {
    log.info(
        "[MOCK] Xác thực kết nối lớp học thành công: classId={}, tutorId={}, studentId={}",
        classId,
        tutorId,
        studentId
    );
    // Trong môi trường Mock, mặc định coi kết nối là hợp lệ nếu cả 3 ID đều khác null
    return classId != null && tutorId != null && studentId != null;
  }
}
