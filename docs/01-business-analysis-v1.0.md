# MES — Business Analysis v1.0

- Trạng thái: **Đã phê duyệt baseline ngày 07/09/2026**
- Phạm vi hiện tại: MES vận hành bằng Excel/nhập trực tiếp, workflow kiểm tra-phê duyệt, dữ liệu canonical nội bộ, quản trị master data, đối soát nhập-xuất-tồn và Dashboard BGĐ canonical; không phụ thuộc Data Lake. Tích hợp Portal đang pending và mặc định bị tắt.
- Tài liệu chuẩn chi phối triển khai: `13-mes-solution-blueprint-v1.0.md`

## BA gate bắt buộc

Baseline sản phẩm, phạm vi, vai trò, quy trình mục tiêu, module, dữ liệu, bảo mật, acceptance criteria và thứ tự triển khai đã được chốt trong MES Solution Blueprint v1.0.

Mỗi biểu mẫu/use case vẫn phải hoàn thành discovery cụ thể về owner, deadline, trường dữ liệu, mapping, công thức, validation, người xác nhận, người duyệt và đối soát trước khi code. Không được suy diễn chi tiết còn thiếu chỉ từ tên file hoặc tên `MES`.

## Kết luận BA đã chốt

- MES phải dùng được ngay mà không chờ Data Lake.
- Excel Portal và nhập trực tiếp là nguồn dữ liệu giai đoạn đầu.
- MES có hai mục tiêu ngang hàng: tạo dữ liệu quản trị nội bộ và làm đầu mối tự động gửi dữ liệu đã duyệt sang Portal. Hai mục tiêu dùng chung một pipeline canonical, không tạo hai bộ số liệu.
- Dữ liệu được chuẩn hóa vào PostgreSQL trước khi dùng cho dashboard.
- File gốc, lịch sử import, revision, approval và bản phát hành dữ liệu nội bộ phải truy vết được. Delivery/biên nhận Portal chỉ bổ sung sau khi hợp đồng tích hợp được duyệt.
- Trong giai đoạn chưa có API Portal, nhân sự tiếp tục nộp Excel trực tiếp trên Portal theo quy trình hiện hành; đây là vận hành chuyển tiếp, không phải kiến trúc đích.
- Giữ nguyên shell và quản trị Core; MES là module dự án độc lập.
- Lát cắt đầu tiên là Trung tâm báo cáo ngày và Dashboard BGĐ.
- Phạm vi MES bao phủ dữ liệu quản trị của toàn bộ phòng ban trong giai đoạn 1 bằng nhập trực tiếp/import Excel; khi Data Lake sẵn sàng chỉ thay adapter đầu vào.
- Cấu trúc điều hướng `Trang chủ → MES (18 menu đã chốt) → Quản trị hệ thống` là baseline bất biến tại `21-mes-navigation-baseline-v1.0.md`.
