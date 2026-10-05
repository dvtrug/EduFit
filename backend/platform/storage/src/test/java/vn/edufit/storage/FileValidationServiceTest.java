package vn.edufit.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.storage.infra.FileValidationService;

class FileValidationServiceTest {

  private FileValidationService validationService;

  @BeforeEach
  void setUp() {
    validationService = new FileValidationService();
  }

  @Test
  @DisplayName("Từ chối khi file rỗng")
  void shouldRejectEmptyFile() {
    MockMultipartFile emptyFile = new MockMultipartFile(
        "file", "empty.pdf", "application/pdf", new byte[0]
    );

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> validationService.validate(emptyFile)
    );
    assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
  }

  @Test
  @DisplayName("Từ chối khi tên file có dấu hiệu path traversal")
  void shouldRejectPathTraversal() {
    MockMultipartFile file = new MockMultipartFile(
        "file", "../../secret.pdf", "application/pdf", "%PDF-1.4 test".getBytes()
    );

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> validationService.validate(file)
    );
    assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
  }

  @Test
  @DisplayName("Từ chối file không đúng định dạng cho phép qua Magic Bytes")
  void shouldRejectUnsupportedMimeType() {
    MockMultipartFile exeFile = new MockMultipartFile(
        "file", "malicious.pdf", "application/pdf", "MZ fake executable header".getBytes()
    );

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> validationService.validate(exeFile)
    );
    assertEquals(ErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
  }

  @Test
  @DisplayName("Chấp nhận file PDF hợp lệ với magic bytes %PDF-")
  void shouldAcceptValidPdf() {
    MockMultipartFile pdfFile = new MockMultipartFile(
        "file", "degree.pdf", "application/pdf", "%PDF-1.5 fake content".getBytes()
    );

    assertDoesNotThrow(() -> validationService.validate(pdfFile));
  }
}

