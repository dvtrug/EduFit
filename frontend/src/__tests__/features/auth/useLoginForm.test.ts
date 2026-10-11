import { describe, it, expect } from "vitest";
import { loginSchema, evaluatePasswordCriteria, resetPasswordSchema } from "@/features/auth/schemas/authSchemas";

describe("Auth Headless Logic & Schemas Unit Tests", () => {
  describe("loginSchema", () => {
    it("chấp nhận email và password hợp lệ", () => {
      const result = loginSchema.safeParse({
        email: "student@edufit.vn",
        password: "Password123@",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.email).toBe("student@edufit.vn");
      }
    });

    it("chuẩn hóa email về lowercase và trim khoảng trắng", () => {
      const result = loginSchema.safeParse({
        email: "  STUDENT@edufit.vn  ",
        password: "Password123@",
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.email).toBe("student@edufit.vn");
      }
    });

    it("từ chối khi email sai định dạng", () => {
      const result = loginSchema.safeParse({
        email: "invalid-email-format",
        password: "Password123@",
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe("Email không hợp lệ");
      }
    });

    it("từ chối khi mật khẩu rỗng", () => {
      const result = loginSchema.safeParse({
        email: "student@edufit.vn",
        password: "",
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe("Vui lòng nhập mật khẩu");
      }
    });
  });

  describe("evaluatePasswordCriteria (BR-02 real-time evaluator)", () => {
    it("đánh giá đúng khi mật khẩu chưa đủ 8 ký tự", () => {
      const criteria = evaluatePasswordCriteria("Pass1");
      expect(criteria.hasMinLength).toBe(false);
      expect(criteria.hasLetter).toBe(true);
      expect(criteria.hasDigit).toBe(true);
      expect(criteria.isValid).toBe(false);
    });

    it("đánh giá đúng khi mật khẩu thiếu chữ số", () => {
      const criteria = evaluatePasswordCriteria("PasswordOnly");
      expect(criteria.hasMinLength).toBe(true);
      expect(criteria.hasLetter).toBe(true);
      expect(criteria.hasDigit).toBe(false);
      expect(criteria.isValid).toBe(false);
    });

    it("đánh giá đúng khi mật khẩu thiếu chữ cái", () => {
      const criteria = evaluatePasswordCriteria("12345678");
      expect(criteria.hasMinLength).toBe(true);
      expect(criteria.hasLetter).toBe(false);
      expect(criteria.hasDigit).toBe(true);
      expect(criteria.isValid).toBe(false);
    });

    it("đánh giá đúng khi mật khẩu thỏa mãn toàn bộ BR-02", () => {
      const criteria = evaluatePasswordCriteria("Password123@");
      expect(criteria.hasMinLength).toBe(true);
      expect(criteria.hasLetter).toBe(true);
      expect(criteria.hasDigit).toBe(true);
      expect(criteria.isValid).toBe(true);
    });
  });

  describe("resetPasswordSchema refine match", () => {
    it("từ chối khi confirmPassword không khớp", () => {
      const result = resetPasswordSchema.safeParse({
        token: "valid-token-32-chars",
        newPassword: "Password123@",
        confirmPassword: "DifferentPassword123@",
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toBe("Mật khẩu xác nhận không khớp!");
      }
    });

    it("chấp nhận khi confirmPassword khớp hoàn toàn", () => {
      const result = resetPasswordSchema.safeParse({
        token: "valid-token-32-chars",
        newPassword: "Password123@",
        confirmPassword: "Password123@",
      });
      expect(result.success).toBe(true);
    });
  });
});
