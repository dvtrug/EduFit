package vn.edufit.storage.infra;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

/**
 * Kiểm tra tính hợp lệ và an toàn của tệp tải lên (NFR-08).
 * Quét Magic Bytes bằng Apache Tika để ngăn chặn việc đổi tên file nguy hiểm.
 */
@Service
public class FileValidationService {

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "application/pdf",
      "image/jpeg",
      "image/png"
  );

  private final Tika tika;

  public FileValidationService() {
    this(new Tika());
  }

  public FileValidationService(Tika tika) {
    this.tika = tika;
  }

  public void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Tệp tải lên không được để trống"
      );
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Kích thước tệp vượt quá giới hạn cho phép (tối đa 10MB)"
      );
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Tên tệp không hợp lệ"
      );
    }

    // Chống tấn công Path Traversal
    if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Tên tệp chứa ký tự đường dẫn không hợp lệ"
      );
    }

    // Kiểm tra Magic Bytes chống tấn công giả mạo đuôi file
    String detectedType;
    try (InputStream inputStream = file.getInputStream()) {
      detectedType = tika.detect(inputStream, originalFilename);
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Không thể đọc nội dung tệp để xác thực định dạng"
      );
    }

    if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_FILE_TYPE,
          "Định dạng tệp không được hỗ trợ. Chỉ chấp nhận PDF, JPEG, PNG"
      );
    }
  }

  public String detectContentType(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      return tika.detect(inputStream, file.getOriginalFilename());
    } catch (IOException ex) {
      return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    }
  }
}

