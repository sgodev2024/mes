# MES — Data Discovery Plan v1.0

- Trạng thái: Chờ BA và nguồn dữ liệu thực tế

## Nội dung cần khảo sát

- Chủ sở hữu, nguồn, định dạng, tần suất và dung lượng dữ liệu.
- Khóa định danh, dữ liệu trùng, thiếu, sai và quy tắc đối soát.
- Dữ liệu cá nhân/nhạy cảm, retention, masking, encryption và quyền truy cập.
- Mapping dữ liệu nguồn sang domain model; chiến lược import, idempotency và lỗi theo dòng.
- Yêu cầu migration, cut-over, rollback, RPO 15 phút và RTO 1 giờ.

Không tạo schema nghiệp vụ hoặc dữ liệu demo trước khi mapping và tiêu chí chất lượng được duyệt.

