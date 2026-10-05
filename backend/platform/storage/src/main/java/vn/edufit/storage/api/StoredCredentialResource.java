package vn.edufit.storage.api;

import org.springframework.core.io.Resource;

public final class StoredCredentialResource {

  private final String storageKey;
  private final Resource resource;

  public StoredCredentialResource(String storageKey, Resource resource) {
    this.storageKey = storageKey;
    this.resource = resource;
  }

  public String storageKey() {
    return storageKey;
  }

  public Resource resource() {
    return resource;
  }
}
