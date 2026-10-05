package vn.edufit.verification.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import vn.edufit.verification.infra.persistence.entity.Credential;

public record SubmitVerificationRequest(
    @NotEmpty(message = "Danh sách bằng cấp không được để trống")
    @Valid
    List<CredentialItem> credentials
) {

  public record CredentialItem(
      @NotNull(message = "Loại bằng cấp là bắt buộc")
      Credential.Type type,

      @NotNull(message = "Tên đơn vị cấp bằng là bắt buộc")
      @Size(min = 1, max = 200, message = "Tên đơn vị cấp bằng phải từ 1 đến 200 ký tự")
      String institution,

      Short year,

      @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
      String note,

      @NotEmpty(message = "Mỗi bằng cấp cần ít nhất một file đính kèm")
      List<Integer> fileIndexes
  ) {
  }
}
