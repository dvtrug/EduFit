package vn.edufit.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.storage.api.StoredCredentialResource;
import vn.edufit.storage.infra.CredentialStorageProperties;
import vn.edufit.storage.infra.FileValidationService;
import vn.edufit.storage.infra.LocalCredentialStorageService;

class LocalCredentialStorageServiceTest {

  @TempDir
  Path tempDir;

  private LocalCredentialStorageService storageService;

  @BeforeEach
  void setUp() {
    CredentialStorageProperties properties = new CredentialStorageProperties();
    properties.setLocalRoot(tempDir.toString());
    FileValidationService validationService = new FileValidationService();
    storageService = new LocalCredentialStorageService(properties, validationService);
  }

  @Test
  @DisplayName("Lưu trữ và nạp lại file thành công")
  void shouldStoreAndLoadFileSuccessfully() throws IOException {
    byte[] content = "%PDF-1.4 sample content".getBytes();
    MockMultipartFile multipartFile = new MockMultipartFile(
        "file", "degree.pdf", "application/pdf", content
    );

    StoredCredentialFile stored = storageService.store(multipartFile);
    assertNotNull(stored.storageKey());
    assertEquals("degree.pdf", stored.fileName());

    StoredCredentialResource resource = storageService.load(stored.storageKey());
    assertNotNull(resource);
    byte[] loadedBytes = resource.inputStream().readAllBytes();
    assertEquals(content.length, loadedBytes.length);

    storageService.delete(stored.storageKey());
    assertTrue(Files.notExists(tempDir.resolve(stored.storageKey())));
  }
}

