package vn.edufit.storage.infra;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.storage.api.CredentialStorageService;
import vn.edufit.storage.api.StoredCredentialFile;
import vn.edufit.storage.api.StoredCredentialResource;

/**
 * Hiện thực lưu trữ tệp trên đĩa cục bộ (Local Disk Storage) theo ADR-001 / ADR-002.
 */
@Service
public class LocalCredentialStorageService implements CredentialStorageService {

  private final Path storageDirectory;
  private final FileValidationService fileValidationService;

  public LocalCredentialStorageService(
      CredentialStorageProperties properties,
      FileValidationService fileValidationService
  ) {
    this.storageDirectory = Paths.get(properties.getLocalRoot()).toAbsolutePath().normalize();
    this.fileValidationService = fileValidationService;
    initializeDirectory();
  }

  private void initializeDirectory() {
    try {
      Files.createDirectories(this.storageDirectory);
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.INTERNAL_SERVER_ERROR,
          "Không thể khởi tạo thư mục lưu trữ tệp: " + ex.getMessage()
      );
    }
  }

  @Override
  public StoredCredentialFile store(MultipartFile file) {
    fileValidationService.validate(file);

    String originalFilename = file.getOriginalFilename();
    String extension = extractExtension(originalFilename);
    String detectedContentType = fileValidationService.detectContentType(file);

    String storageKey = UUID.randomUUID() + extension;
    Path targetPath = resolvePath(storageKey);

    try (InputStream inputStream = file.getInputStream();
         OutputStream outputStream = Files.newOutputStream(
             targetPath,
             StandardOpenOption.CREATE_NEW,
             StandardOpenOption.WRITE
         )) {
      inputStream.transferTo(outputStream);
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.INTERNAL_SERVER_ERROR,
          "Lỗi khi ghi tệp bằng cấp vào hệ thống lưu trữ: " + ex.getMessage()
      );
    }

    return new StoredCredentialFile(
        originalFilename,
        storageKey,
        detectedContentType,
        file.getSize()
    );
  }

  @Override
  public StoredCredentialResource load(String storageKey) {
    Path targetPath = resolvePath(storageKey);
    if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
      throw EntityNotFoundException.of("CredentialFile", storageKey);
    }

    try {
      long size = Files.size(targetPath);
      String contentType = Files.probeContentType(targetPath);
      if (contentType == null) {
        contentType = "application/octet-stream";
      }
      InputStream inputStream = Files.newInputStream(targetPath, StandardOpenOption.READ);
      return new StoredCredentialResource(
          inputStream,
          contentType,
          size,
          storageKey
      );
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.INTERNAL_SERVER_ERROR,
          "Lỗi khi đọc tệp từ hệ thống lưu trữ: " + ex.getMessage()
      );
    }
  }

  @Override
  public void delete(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      return;
    }
    try {
      Path targetPath = resolvePath(storageKey);
      Files.deleteIfExists(targetPath);
    } catch (IOException ignored) {
      // Bỏ qua lỗi nếu không thể xóa để tránh làm gián đoạn transaction
    }
  }

  private Path resolvePath(String storageKey) {
    Path resolved = storageDirectory.resolve(storageKey).normalize();
    if (!resolved.startsWith(storageDirectory)) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Đường dẫn lưu trữ không an toàn"
      );
    }
    return resolved;
  }

  private String extractExtension(String filename) {
    if (filename == null || filename.isBlank()) {
      return "";
    }
    int lastDot = filename.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < filename.length() - 1) {
      return filename.substring(lastDot);
    }
    return "";
  }
}

