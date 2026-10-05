package vn.edufit.storage.infra;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class FileValidationService {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "application/pdf",
      "image/jpeg",
      "image/png"
  );

  private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
      "application/pdf", ".pdf",
      "image/jpeg", ".jpg",
      "image/png", ".png"
  );

  private final CredentialStorageProperties properties;
  private final Tika tika;

  public FileValidationService(CredentialStorageProperties properties) {
    this(properties, new Tika());
  }

  FileValidationService(CredentialStorageProperties properties, Tika tika) {
    this.properties = properties;
    this.tika = tika;
  }

  public ValidatedFile validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "File bằng cấp không được để trống"
      );
    }
    if (file.getSize() > properties.getMaxFileSizeBytes()) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "File bằng cấp vượt quá dung lượng cho phép"
      );
    }

    String detectedContentType = detectContentType(file);
    if (!ALLOWED_CONTENT_TYPES.contains(detectedContentType)) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_FILE_TYPE,
          "Định dạng file bằng cấp không hợp lệ"
      );
    }

    return new ValidatedFile(
        sanitizeFileName(file.getOriginalFilename(), extensionFor(detectedContentType)),
        detectedContentType,
        file.getSize()
    );
  }

  public String extensionFor(String contentType) {
    String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
    if (extension == null) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_FILE_TYPE,
          "Định dạng file bằng cấp không hợp lệ"
      );
    }
    return extension;
  }

  private String detectContentType(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      return tika.detect(inputStream);
    } catch (IOException ex) {
      throw new InvalidOperationException(
          ErrorCode.INVALID_OPERATION,
          "Không thể đọc file bằng cấp để kiểm tra định dạng"
      );
    }
  }

  private String sanitizeFileName(String originalFileName, String expectedExtension) {
    String fileName = originalFileName == null || originalFileName.isBlank()
        ? "credential" + expectedExtension
        : PathName.onlyFileName(originalFileName);

    int extensionIndex = fileName.lastIndexOf('.');
    String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[^A-Za-z0-9._-]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_+|_+$", "");

    if (normalized.isBlank()) {
      normalized = "credential";
    }
    return normalized + expectedExtension;
  }

  public static final class ValidatedFile {

    private final String fileName;
    private final String contentType;
    private final long sizeBytes;

    public ValidatedFile(String fileName, String contentType, long sizeBytes) {
      this.fileName = fileName;
      this.contentType = contentType;
      this.sizeBytes = sizeBytes;
    }

    public String fileName() {
      return fileName;
    }

    public String contentType() {
      return contentType;
    }

    public long sizeBytes() {
      return sizeBytes;
    }
  }

  private static final class PathName {
    private PathName() {
    }

    private static String onlyFileName(String rawName) {
      String normalized = rawName.replace('\\', '/');
      int separatorIndex = normalized.lastIndexOf('/');
      return separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
    }
  }
}
