# Kiến trúc backend EduFit

EduFit là một ứng dụng Spring Boot duy nhất được tổ chức theo kiến trúc modular monolith. Spring Modulith chỉ phát hiện các module được đánh dấu tường minh. Test `ModularArchitectureTest` sẽ từ chối dependency không hợp lệ giữa các package và chu trình phụ thuộc giữa các module.

| Module | Trách nhiệm theo SRS |
| --- | --- |
| `identity` | Tài khoản và hồ sơ Học sinh, Phụ huynh, Gia sư |
| `verification` | Chứng chỉ của Gia sư và quy trình xác minh của Quản trị viên |
| `discovery` | Tìm kiếm, lọc, ghép cặp Gia sư và giải thích kết quả gợi ý |
| `relationship` | Liên kết Phụ huynh–Học sinh và kết nối Gia sư–Học sinh |
| `scheduling` | Đề xuất buổi học, xử lý trùng lịch, đổi lịch và kết quả buổi học |
| `learning` | Kế hoạch học tập, cột mốc, ghi chú buổi học và tiến độ |
| `feedback` | Đánh giá Gia sư và điểm đánh giá tổng hợp |
| `notification` | Thông báo trong ứng dụng được giới hạn theo người nhận |

Mỗi module sở hữu domain model, application service, persistence adapter và inbound adapter của chính nó. Module khác chỉ được sử dụng các kiểu dữ liệu được công khai trực tiếp từ package gốc của module hoặc từ named interface công khai. Không được import package nội bộ xuyên qua ranh giới module.

Ứng dụng sử dụng một cơ sở dữ liệu PostgreSQL. Schema `edufit` là ranh giới dữ liệu của ứng dụng. Sau migration nền ban đầu, Flyway là cơ chế duy nhất được phép dùng để phát triển schema.
