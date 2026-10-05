package vn.edufit.storage.api;

/**
 * Metadata của tệp bằng cấp đã được lưu trữ an toàn.
 */
public record StoredCredentialFile(
    String fileName,
    String storageKey,
    String contentType,
    long sizeBytes
) {}

