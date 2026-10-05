package vn.edufit.storage.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edufit.storage")
public class CredentialStorageProperties {

  private String localRoot = "uploads/credentials";

  public String getLocalRoot() {
    return localRoot;
  }

  public void setLocalRoot(String localRoot) {
    this.localRoot = localRoot;
  }
}

