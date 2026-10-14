package vn.edufit.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.edufit.shared.exception.BaseBusinessException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.storage.infra.CredentialStorageProperties;
import vn.edufit.storage.infra.FileValidationService;
import vn.edufit.storage.infra.LocalCredentialStorageService;

@DisplayName("Kiểm thử Kịch bản 5.3 Master Test Plan: Quét Magic Bytes bằng Apache Tika & Bảo mật lưu trữ")
class CredentialFileValidationSecurityTest {

  @TempDir
  Path tempDir;

  private final Tika tika = new Tika();
  private CredentialStorageProperties properties;
  private FileValidationService fileValidationService;
  private LocalCredentialStorageService storageService;

  @BeforeEach
  void setUp() {
    properties = new CredentialStorageProperties();
    properties.setRoot(tempDir);
    fileValidationService = new FileValidationService(properties);
    storageService = new LocalCredentialStorageService(properties, fileValidationService);
  }

  @Test
  @DisplayName("NFR-08: Tệp có đuôi .pdf nhưng nội dung thực tế là bash script -> Tika phát hiện text/plain và chặn đứng")
  void shouldRejectSpoofedPdfContainingBashScript() {
    byte[] maliciousBytes = "#!/bin/bash\necho 'Hacked'".getBytes();
    MockMultipartFile spoofedFile = new MockMultipartFile(
        "file",
        "my_degree.pdf",
        "application/pdf",
        maliciousBytes
    );

    // Xác minh Apache Tika nhận diện chính xác application/x-sh dựa trên Magic Bytes shell script
    String detectedMimeType = tika.detect(maliciousBytes);
    assertEquals("application/x-sh", detectedMimeType);

    BaseBusinessException ex = assertThrows(
        BaseBusinessException.class,
        () -> storageService.store(spoofedFile)
    );
    assertEquals(ErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
  }

  @Test
  @DisplayName("NFR-08: Tệp có đuôi .png nhưng nội dung là Windows Executable (.exe PE Header) -> Bị chặn đứng")
  void shouldRejectSpoofedImageContainingExecutableBinary() {
    // Header PE của tệp thực thi Windows (MZ)
    byte[] exeBytes = new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
    MockMultipartFile spoofedImage = new MockMultipartFile(
        "file",
        "avatar_credential.png",
        "image/png",
        exeBytes
    );

    String detectedMimeType = tika.detect(exeBytes);
    assertEquals("application/x-msdownload", detectedMimeType);

    BaseBusinessException ex = assertThrows(
        BaseBusinessException.class,
        () -> storageService.store(spoofedImage)
    );
    assertEquals(ErrorCode.INVALID_FILE_TYPE, ex.getErrorCode());
  }

  @Test
  @DisplayName("NFR-08: Tệp PDF thật (header %PDF-1.4) -> Lưu trữ nhị phân an toàn và đọc lại chính xác")
  void shouldAcceptAndPersistValidPdf() throws IOException {
    byte[] validPdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".getBytes();
    MockMultipartFile validPdf = new MockMultipartFile(
        "file",
        "bang_dai_hoc.pdf",
        "application/octet-stream",
        validPdfBytes
    );

    StoredCredentialFile stored = storageService.store(validPdf);
    assertNotNull(stored);
    assertNotNull(stored.storageKey());
    assertEquals("application/pdf", stored.contentType());
    assertEquals("bang_dai_hoc.pdf", stored.fileName());

    // Xác minh file nhị phân thật nằm trong thư mục storage root
    Path storedPath = tempDir.resolve(stored.storageKey());
    assertTrue(Files.exists(storedPath));
    assertEquals(validPdfBytes.length, Files.size(storedPath));
  }

  @Test
  @DisplayName("Path Traversal Attack: Tên file chứa ../../etc/passwd -> Bị sanitize và cô lập bên trong root")
  void shouldSanitizePathTraversalAttempt() {
    byte[] validPdfBytes = "%PDF-1.4\n%test-content".getBytes();
    MockMultipartFile maliciousPathFile = new MockMultipartFile(
        "file",
        "../../etc/passwd.pdf",
        "application/pdf",
        validPdfBytes
    );

    StoredCredentialFile stored = storageService.store(maliciousPathFile);
    assertNotNull(stored);
    assertEquals("passwd.pdf", stored.fileName());
    assertTrue(stored.storageKey().endsWith(".pdf"));

    Path resolvedPath = tempDir.resolve(stored.storageKey()).normalize();
    assertTrue(resolvedPath.startsWith(tempDir.normalize()));
  }
}
