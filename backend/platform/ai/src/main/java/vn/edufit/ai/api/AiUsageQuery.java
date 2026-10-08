package vn.edufit.ai.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Cổng truy vấn tần suất sử dụng dịch vụ AI (AI Usage Query Port).
 *
 * <p>Mục đích thiết kế & Ranh giới trách nhiệm:
 * <ul>
 *   <li><b>Hỗ trợ kiểm soát hạn ngạch (Quota Enforcement):</b> Cho phép các module nghiệp vụ
 *       kiểm tra xem người dùng đã thực hiện bao nhiêu yêu cầu AI trong một khoảng thời gian nhất định.</li>
 *   <li><b>Tách biệt quy tắc nghiệp vụ:</b>
 *     <ul>
 *       <li>Quy tắc <b>BR-10</b> (Tối đa 5 lần tạo Bio trong 1 phiên sửa hồ sơ) thuộc sở hữu của module {@code profile}.</li>
 *       <li>Quy tắc <b>BR-25</b> (Giới hạn số lần giải thích ghép đôi trong 1 ngày) thuộc sở hữu của module {@code discovery}.</li>
 *     </ul>
 *     Phân hệ {@code platform/ai} <b>không tự ý chặn request</b> mà chỉ cung cấp số liệu thực tế trung thực
 *     từ bảng {@code ai_request_log}. Quyết định cho phép hay chặn thuộc thẩm quyền của module nghiệp vụ gọi tới.</li>
 * </ul>
 *
 * <p>Ví dụ kiểm tra BR-10 từ {@code ProfileService}:
 * <pre>{@code
 * long bioRequestsInSession = aiUsageQuery.countRequestsSince(tutorId, AiFeature.TUTOR_BIO, sessionStartTime);
 * if (bioRequestsInSession >= 5) {
 *     throw new BaseBusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "Đã đạt giới hạn 5 lần tạo bio trong phiên (BR-10).");
 * }
 * }</pre>
 */
public interface AiUsageQuery {

  /**
   * Đếm số lượt yêu cầu AI (cả thành công, lỗi và timeout) mà người dùng đã thực hiện
   * cho một tính năng cụ thể kể từ một mốc thời gian nhất định.
   *
   * @param userId  Định danh tài khoản người dùng cần tra cứu.
   * @param feature Tính năng AI cần thống kê (TUTOR_BIO hoặc MATCH_EXPLANATION).
   * @param since   Thời điểm bắt đầu tính toán (ví dụ: thời điểm mở phiên chỉnh sửa hoặc đầu ngày).
   * @return Số lượng lượt gọi ghi nhận được trong cơ sở dữ liệu (luôn &gt;= 0).
   */
  long countRequestsSince(UUID userId, AiFeature feature, Instant since);
}
