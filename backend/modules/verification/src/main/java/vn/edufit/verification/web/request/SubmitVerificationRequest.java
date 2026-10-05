package vn.edufit.verification.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import vn.edufit.verification.infra.persistence.entity.CredentialType;

public record SubmitVerificationRequest(
    @NotEmpty(message = "Danh sách văn bằng, chứng chỉ không được để trống")
    @Valid
    List<CredentialItem> credentials
) {

  public record CredentialItem(
      @NotNull(message = "Loại chứng chỉ không được để trống")
      CredentialType type,

      @NotBlank(message = "Tên trường hoặc đơn vị cấp chứng chỉ không được để trống")
      @Size(max = 200, message = "Tên cơ sở cấp không quá 200 ký tự")
      String institution,

      Short year,

      @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
      String note,

      @NotEmpty(message = "Mỗi văn bằng phải gắn với ít nhất một file đính kèm")
      List<Integer> fileIndexes
  ) {}
}

