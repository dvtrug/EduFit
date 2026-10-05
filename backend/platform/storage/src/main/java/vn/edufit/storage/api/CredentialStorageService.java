package vn.edufit.storage.api;

import org.springframework.web.multipart.MultipartFile;

public interface CredentialStorageService {

  StoredCredentialFile store(MultipartFile file);

  StoredCredentialResource load(String storageKey);

  void delete(String storageKey);
}
