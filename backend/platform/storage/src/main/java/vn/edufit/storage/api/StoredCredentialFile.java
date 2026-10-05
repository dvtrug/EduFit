package vn.edufit.storage.api;

public final class StoredCredentialFile {

  private final String storageKey;
  private final String fileName;
  private final String contentType;
  private final long sizeBytes;

  public StoredCredentialFile(String storageKey, String fileName, String contentType, long sizeBytes) {
    this.storageKey = storageKey;
    this.fileName = fileName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
  }

  public String storageKey() {
    return storageKey;
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
