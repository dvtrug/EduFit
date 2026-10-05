package vn.edufit.storage.api;

import java.io.InputStream;

/**
 * Nguồn dữ liệu tệp bằng cấp để phục vụ đọc / stream.
 */
public record StoredCredentialResource(
    InputStream inputStream,
    String contentType,
    long contentLength,
    String fileName
) {}

