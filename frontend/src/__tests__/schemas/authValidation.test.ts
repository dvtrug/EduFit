import { describe, it, expect } from "vitest";
import { registerSchema, loginSchema, changePasswordSchema } from "@/lib/validation";
import authFixtures from "../fixtures/auth-validation-dataset.json";

describe("Auth Validation Schemas (Data-Driven Testing)", () => {
  describe("Password Policy Validation (BR-02)", () => {
    it.each(authFixtures.passwordCases)(
      "Kiểm tra mật khẩu: $desc ($password)",
      ({ password, valid, error }) => {
        const result = registerSchema.shape.password.safeParse(password);
        expect(result.success).toBe(valid);
        if (!valid && error) {
          expect(result.error?.issues[0].message).toBe(error);
        }
      }
    );
  });

  describe("Role Restriction Validation (BR-03)", () => {
    it.each(authFixtures.roleCases)(
      "Kiểm tra hạn chế vai trò (BR-03): $role -> $valid ($desc)",
      ({ role, valid }) => {
        const result = registerSchema.shape.role.safeParse(role);
        expect(result.success).toBe(valid);
      }
    );
  });

  describe("Email Format Validation (BR-01)", () => {
    it.each(authFixtures.emailCases)(
      "Kiểm tra định dạng email: $email -> $valid ($desc)",
      ({ email, valid, error }) => {
        const result = registerSchema.shape.email.safeParse(email);
        expect(result.success).toBe(valid);
        if (!valid && error) {
          expect(result.error?.issues[0].message).toBe(error);
        }
      }
    );
  });

  describe("Full Name Validation", () => {
    it.each(authFixtures.fullNameCases)(
      "Kiểm tra họ tên: $fullName -> $valid ($desc)",
      ({ fullName, valid, error }) => {
        const result = registerSchema.shape.fullName.safeParse(fullName);
        expect(result.success).toBe(valid);
        if (!valid && error) {
          expect(result.error?.issues[0].message).toBe(error);
        }
      }
    );
  });

  describe("Login Schema Validation", () => {
    it("Chấp nhận payload đăng nhập hợp lệ", () => {
      const result = loginSchema.safeParse({
        email: "student@edufit.vn",
        password: "Password123",
      });
      expect(result.success).toBe(true);
    });

    it("Từ chối khi email không đúng định dạng", () => {
      const result = loginSchema.safeParse({
        email: "invalid-email",
        password: "Password123",
      });
      expect(result.success).toBe(false);
    });

    it("Từ chối khi thiếu mật khẩu", () => {
      const result = loginSchema.safeParse({
        email: "student@edufit.vn",
        password: "",
      });
      expect(result.success).toBe(false);
    });
  });

  describe("Change Password Schema Validation", () => {
    it("Chấp nhận đổi mật khẩu khi mật khẩu mới hợp lệ", () => {
      const result = changePasswordSchema.safeParse({
        currentPassword: "OldPassword123",
        newPassword: "NewPassword456",
      });
      expect(result.success).toBe(true);
    });

    it("Từ chối khi mật khẩu mới không đáp ứng chính sách bảo mật", () => {
      const result = changePasswordSchema.safeParse({
        currentPassword: "OldPassword123",
        newPassword: "short",
      });
      expect(result.success).toBe(false);
    });
  });
});
