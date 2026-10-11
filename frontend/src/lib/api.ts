/**
 * Cấu trúc phản hồi chuẩn (Response Envelope) từ Backend EduFit
 * Phù hợp với đặc tả tại docs/authentication-contract.md
 */
export interface ApiValidationError {
  field: string;
  message: string;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  message?: string;
  data?: T;
  code?: string;
  details?: ApiValidationError[];
  timestamp?: string;
}

/**
 * Lớp lỗi đại diện cho phản hồi thất bại từ API Backend
 */
export class ApiError extends Error {
  status: number;
  code?: string;
  details?: ApiValidationError[];

  constructor(
    message: string,
    status: number,
    code?: string,
    details?: ApiValidationError[]
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

/**
 * Hàm gọi API dùng chung, tự động đính kèm Cookie Session và xử lý lỗi chuẩn hóa
 */
export async function fetchApi<T = unknown>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const headers = new Headers(options.headers || {});
  
  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const config: RequestInit = {
    ...options,
    headers,
    // BẮT BUỘC: Cho phép gửi và nhận cookie phiên Stateful EDUFIT_SESSION
    credentials: "include",
  };

  try {
    const response = await fetch(endpoint, config);
    const data: ApiResponse<T> = await response.json().catch(() => ({
      success: response.ok,
      message: response.statusText,
    }));

    if (!response.ok || data.success === false) {
      throw new ApiError(
        data.message || "Đã xảy ra lỗi khi kết nối với máy chủ",
        response.status,
        data.code,
        data.details
      );
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(
      error instanceof Error ? error.message : "Mất kết nối mạng hoặc máy chủ không phản hồi",
      0,
      "NETWORK_ERROR"
    );
  }
}

/**
 * Tiện ích gọi API dạng phương thức RESTful (GET, POST, PUT, DELETE)
 */
export const apiClient = {
  get: <T>(url: string, init?: RequestInit) => fetchApi<T>(url, { ...init, method: "GET" }),
  post: <T>(url: string, body?: unknown, init?: RequestInit) =>
    fetchApi<T>(url, {
      ...init,
      method: "POST",
      body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
    }),
  put: <T>(url: string, body?: unknown, init?: RequestInit) =>
    fetchApi<T>(url, {
      ...init,
      method: "PUT",
      body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
    }),
  delete: <T>(url: string, init?: RequestInit) => fetchApi<T>(url, { ...init, method: "DELETE" }),
};

