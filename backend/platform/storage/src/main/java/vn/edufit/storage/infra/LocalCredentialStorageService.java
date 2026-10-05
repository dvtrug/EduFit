package vn.edufit.storage.infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.storage.api.CredentialStorageService;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.storage.api.StoredCredentialResource;

@Service
public class LocalCredentialStorageService implements CredentialStorageService {

  private final CredentialStorageProperties properties;
  private final FileValidationService validationService;

  public LocalCredentialStorageService(
      CredentialStorageProperties properties,
      FileValidationService validationService
  ) {
    this.properties = properties;
    this.validationService = validationService;
  }

  @Override
  public StoredCredentialFile store(MultipartFile file) {
    FileValidationService.ValidatedFile validated = validationService.validate(file);
    String storageKey = UUID.randomUUID() + validationService.extensionFor(validated.contentType());
    Path target = resolveStorageKey(storageKey);

    try {
      Files.createDirectories(root());
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_OPERATION,
          "Không thể lưu file bằng cấp vào local storage"
      );
    }

    return new StoredCredentialFile(
        storageKey,
        validated.fileName(),
        validated.contentType(),
        validated.sizeBytes()
    );
  }

  @Override
  public StoredCredentialResource load(String storageKey) {
    Path path = resolveStorageKey(storageKey);
    if (!Files.isRegularFile(path)) {
      throw EntityNotFoundException.of("CredentialFile", storageKey);
    }
    return new StoredCredentialResource(storageKey, new FileSystemResource(path));
  }

  @Override
  public void delete(String storageKey) {
    Path path = resolveStorageKey(storageKey);
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_OPERATION,
          "Không thể xóa file bằng cấp khỏi local storage"
      );
    }
  }

  private Path resolveStorageKey(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_OPERATION,
          "Storage key của file bằng cấp không hợp lệ"
      );
    }

    Path resolved = root().resolve(storageKey).normalize();
    if (!resolved.startsWith(root())) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_OPERATION,
          "Storage key của file bằng cấp không hợp lệ"
      );
    }
    return resolved;
  }

  private Path root() {
    return properties.getRoot().toAbsolutePath().normalize();
  }
}
