package vn.edufit.storage.infra;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CredentialStorageProperties {

  private Path root = Path.of("var", "credentials");

  private long maxFileSizeBytes = 10 * 1024 * 1024;

  @Value("${edufit.storage.credentials.root:var/credentials}")
  public void setRoot(String root) {
    this.root = Path.of(root);
  }

  @Value("${edufit.storage.credentials.max-file-size-bytes:10485760}")
  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    this.maxFileSizeBytes = maxFileSizeBytes;
  }

  public Path getRoot() {
    return root;
  }

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }
}
