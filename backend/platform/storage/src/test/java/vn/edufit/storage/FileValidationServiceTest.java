package vn.edufit.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.storage.infra.CredentialStorageProperties;
import vn.edufit.storage.infra.FileValidationService;

@DisplayName("Kiểm thử xác thực file bằng cấp bằng Magic Bytes")
class FileValidationServiceTest {

  private final CredentialStorageProperties properties = new CredentialStorageProperties();
  private final FileValidationService validationService = new FileValidationService(properties);

  @Test
  @DisplayName("STO-VAL-001: Chấp nhận PDF thật theo magic bytes và sanitize tên file")
  void shouldAcceptPdfByMagicBytesAndSanitizeFileName() {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "../Bang cap tot nghiep.pdf",
        "application/octet-stream",
        "%PDF-1.4\n%test".getBytes()
    );

    FileValidationService.ValidatedFile validated = validationService.validate(file);

    assertEquals("Bang_cap_tot_nghiep.pdf", validated.fileName());
    assertEquals("application/pdf", validated.contentType());
    assertEquals(file.getSize(), validated.sizeBytes());
  }

  @Test
  @DisplayName("STO-VAL-002: Từ chối file có content type khai báo là PDF nhưng magic bytes không phải PDF")
  void shouldRejectDisguisedPdfFile() {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "credential.pdf",
        "application/pdf",
        "<script>alert('xss')</script>".getBytes()
    );

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> validationService.validate(file)
    );

    assertEquals(ErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
  }

  @Test
  @DisplayName("STO-VAL-003: Từ chối file vượt quá giới hạn dung lượng cấu hình")
  void shouldRejectFileLargerThanConfiguredLimit() {
    properties.setMaxFileSizeBytes(4);
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "credential.pdf",
        "application/pdf",
        "%PDF-1.4\n%test".getBytes()
    );

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> validationService.validate(file)
    );

    assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
  }
}
