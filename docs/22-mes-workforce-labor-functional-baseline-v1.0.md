# MES – Baseline nghiệp vụ Nhân sự & lao động v1.0

Ngày chốt: 10/09/2026
Phạm vi: giai đoạn nhập liệu thủ công/Excel, sẵn sàng thay nguồn bằng HRM/Data Lake.

## 1. Nguyên tắc

- Không dùng một hành động “Thêm dữ liệu nhân sự” cho mọi tab.
- `Danh sách nhân sự` là dữ liệu gốc của module MES, độc lập với tài khoản đăng nhập Core.
- Hồ sơ và chứng chỉ phải tham chiếu một nhân sự gốc còn tồn tại.
- Ca công, năng suất, chức danh và định biên là các facts/danh mục riêng; không tạo thêm nhân sự.
- Mọi bản ghi mới bắt đầu ở `Nháp`, áp dụng workflow xác nhận, phân quyền `MES_OPERATION`, optimistic locking, audit log và outbox.

## 2. Ma trận tab và hành động

| Tab | Hành động chính | Mục đích | Dữ liệu bắt buộc |
|---|---|---|---|
| Danh sách nhân sự | Thêm nhân sự | Tạo hồ sơ nhân sự gốc phục vụ tổng hợp MES | Họ tên, đơn vị, chức danh, ngày bắt đầu |
| Hồ sơ nhân sự | Bổ sung hồ sơ | Bổ sung hồ sơ lao động/sức khỏe/bảo hiểm/khen thưởng | Nhân sự đã có, loại hồ sơ, loại hợp đồng, ngày cập nhật |
| Chức danh và đơn vị | Thêm chức danh và định biên | Quản lý cơ cấu vị trí và chênh lệch định biên | Chức danh, đơn vị, hiện có, định biên, phụ trách |
| Phân ca và ngày công | Ghi nhận ca và ngày công | Ghi nhận nguồn lực theo ngày và ca | Ngày, ca, đơn vị, kế hoạch, có mặt, phụ trách |
| Năng suất lao động | Ghi nhận năng suất | Tính năng suất từ sản lượng/ngày công | Kỳ, đơn vị, sản lượng, ngày công, đơn vị đo, phụ trách |
| Đào tạo và chứng chỉ | Thêm đào tạo hoặc chứng chỉ | Theo dõi năng lực và hạn chứng chỉ | Nhân sự đã có, khóa học/chứng chỉ, ngày cấp |

## 3. Quy tắc dữ liệu

- Mã được backend tự sinh theo loại: `NV-*`, `HS-*`, `CD-*`, `CA-*`, `NS-*`, `DT-*`.
- Ngày kết thúc/hết hạn được phép để trống; khi trống hiển thị `—`.
- Ngày kết thúc không được trước ngày bắt đầu/ngày cấp.
- Số nhân sự có mặt không được lớn hơn kế hoạch ca.
- Ngày công dùng tính năng suất phải lớn hơn `0`; năng suất = sản lượng / ngày công.
- Chức danh được chuẩn hóa chữ hoa khi nhập từ giao diện.
- Hồ sơ/chứng chỉ gửi mã nhân sự gốc; backend từ chối nếu mã không còn tồn tại.

## 4. Workflow và quyền

`Nháp → Đang xử lý → Chờ phê duyệt → Đã duyệt → Đã đóng`.

- `REJECT` đưa bản ghi chờ duyệt về `Bị trả lại`; `START` cho phép xử lý lại.
- `REOPEN` mở lại bản ghi đã đóng.
- Đọc: `MES_OPERATION:READ`; tạo: `MES_OPERATION:CREATE`; chuyển trạng thái: `MES_OPERATION:UPDATE`; duyệt/trả lại: `MES_OPERATION:APPROVE`.
- Mỗi thao tác tạo/chuyển trạng thái phát audit event và outbox event.

## 5. Contract kỹ thuật

- Đọc tab: `GET /api/v1/mes/modules/workforce-labor/overview?tab={tab}`.
- Tạo dữ liệu đúng tab: `POST /api/v1/mes/modules/workforce-labor/records`.
- Chuyển workflow: `PATCH /api/v1/mes/modules/workforce-labor/records/{id}/workflow`.
- Các trường dùng chung lưu ở cột canonical của `mes.operational_record`; thuộc tính theo tab lưu trong `details` JSONB để giữ khả năng mở rộng và ánh xạ Data Lake sau này.

## 6. Tiêu chí nghiệm thu

- Đổi tab phải đổi đồng thời nhãn nút, tiêu đề popup, trường nhập và validation.
- Hồ sơ/chứng chỉ chỉ chọn được nhân sự từ `Danh sách nhân sự`.
- Sau khi xác nhận, dữ liệu xuất hiện đúng tab mà không tải lại toàn trang.
- Dữ liệu vẫn tồn tại sau khi khởi động lại Docker.
- Lỗi nghiệp vụ trả về mã và thông báo cụ thể; không dùng thông báo chung chung.
- Audit và outbox có bản ghi tương ứng cho thao tác thành công.
