package vn.edufit.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.storage.api.StoredCredentialResource;
import vn.edufit.storage.infra.CredentialStorageProperties;
import vn.edufit.storage.infra.FileValidationService;
import vn.edufit.storage.infra.LocalCredentialStorageService;

@DisplayName("Kiểm thử LocalCredentialStorageService")
class LocalCredentialStorageServiceTest {

  @TempDir
  private Path tempDir;

  private LocalCredentialStorageService storageService;

  @BeforeEach
  void setUp() {
    CredentialStorageProperties properties = new CredentialStorageProperties();
    properties.setRoot(tempDir);
    FileValidationService validationService = new FileValidationService(properties);
    storageService = new LocalCredentialStorageService(properties, validationService);
  }

  @Test
  @DisplayName("STO-LOC-001: Lưu, load và xóa file bằng storage key an toàn")
  void shouldStoreLoadAndDeleteCredentialFile() throws IOException {
    byte[] content = "%PDF-1.4\ncredential".getBytes();
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "credential.pdf",
        "application/pdf",
        content
    );

    StoredCredentialFile stored = storageService.store(file);
    StoredCredentialResource loaded = storageService.load(stored.storageKey());

    assertEquals("credential.pdf", stored.fileName());
    assertEquals("application/pdf", stored.contentType());
    assertEquals(content.length, stored.sizeBytes());
    assertTrue(stored.storageKey().endsWith(".pdf"));
    assertArrayEquals(content, loaded.resource().getContentAsByteArray());

    storageService.delete(stored.storageKey());

    assertFalse(Files.exists(tempDir.resolve(stored.storageKey())));
  }

  @Test
  @DisplayName("STO-LOC-002: Chặn storage key path traversal khi load file")
  void shouldRejectPathTraversalStorageKeyWhenLoading() {
    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> storageService.load("../secret.pdf")
    );

    assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
  }

  @Test
  @DisplayName("STO-LOC-003: Chặn storage key path traversal khi xóa file")
  void shouldRejectPathTraversalStorageKeyWhenDeleting() {
    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> storageService.delete("../secret.pdf")
    );

    assertEquals(ErrorCode.INVALID_OPERATION, ex.getErrorCode());
  }
}
