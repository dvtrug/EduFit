package vn.edufit.storage.api;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cổng giao tiếp lưu trữ tài liệu chứng chỉ / bằng cấp (ADR-002 Decision 6).
 */
public interface CredentialStorageService {

  /**
   * Lưu trữ tệp tải lên từ người dùng sau khi kiểm tra an toàn.
   *
   * @param file tệp multipart được gửi lên
   * @return metadata tệp sau khi lưu
   */
  StoredCredentialFile store(MultipartFile file);

  /**
   * Tải tài nguyên tệp theo khóa lưu trữ.
   *
   * @param storageKey khóa định danh vị trí lưu
   * @return dữ liệu stream tệp
   */
  StoredCredentialResource load(String storageKey);

  /**
   * Xóa tệp vật lý theo khóa lưu trữ (dùng khi rollback hoặc thu hồi).
   *
   * @param storageKey khóa định danh vị trí lưu
   */
  void delete(String storageKey);
}

