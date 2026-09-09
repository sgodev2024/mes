# MES — Slice 0 Data Contract & Pilot v1.0

**Trạng thái:** Baseline kỹ thuật và UAT cấu trúc đã đạt; chờ UAT dữ liệu có phát sinh và phê duyệt nghiệp vụ  
**Ngày cập nhật:** 08/09/2026  
**Tài liệu chi phối:** `13-mes-solution-blueprint-v1.0.md`

## 1. Mục tiêu

Slice 0 thiết lập một hợp đồng dữ liệu có kiểm soát giữa các file Excel đang dùng để nộp Portal và cơ sở dữ liệu MES. Giai đoạn này không yêu cầu Data Lake và không thay đổi biểu mẫu nghiệp vụ gốc.

Kết quả cần đạt:

- nhận diện đúng loại biểu mẫu trước khi đọc dữ liệu;
- lưu file gốc, checksum và lịch sử từng lần nhập;
- trả lỗi chính xác theo worksheet, dòng và ô;
- ngăn dữ liệu lỗi đi vào quy trình phê duyệt;
- bảo toàn khả năng truy vết từ báo cáo MES về file nguồn;
- tạo nền để mở rộng parser theo từng phiên bản biểu mẫu.

## 2. Phạm vi pilot

| Mã Data Contract | Biểu mẫu | Tệp chuẩn | Worksheet | Parser | Hạn nộp mặc định |
|---|---|---|---|---|---|
| `MES-DAY-PRODUCTION-PTCB` | Thực hiện pha trộn, chế biến ngày | `4100_TT_Ngay_PTCB_01.xlsx` | `Template_Import` | `MULTI_HEADER` | 17:00 |
| `MES-DAY-RAW-COAL-RECEIPT` | Nhập kho than nguyên khai ngày | `4100_TT_NhapKho_Ngay_thanNK.xlsx` | `Template_Import` | `MULTI_HEADER` | 17:00 |
| `MES-DAY-CLEAN-COAL-RECEIPT` | Nhập kho than sạch ngày | `4100_TT_NhapKho_Ngay_than_sach.xlsx` | `Template_Import` | `MULTI_HEADER` | 17:00 |
| `MES-DAY-STOCK` | Tồn kho than ngày | `4100_TT_Tonkho_Ngay.xlsx` | `Template_Import` | `TABULAR` | 17:00 |
| `MES-DAY-CLEAN-COAL-ISSUE` | Xuất kho than sạch ngày | `4100_TT_XuatKho_Ngay_than_sach.xlsx` | `Template_Import` | `TABULAR` | 17:00 |
| `MES-DAY-RAW-COAL-ISSUE` | Xuất kho than nguyên khai ngày | `4100_TT_XuatKho_Ngay_thanNK.xlsx` | `Template_Import` | `TABULAR` | 17:00 |

Vùng tiêu đề và field contract của cả sáu mẫu đã được đối chiếu với workbook nguồn. `MES-DAY-STOCK` đã chạy integration end-to-end; năm mẫu còn lại đã có mapping/validation cấp trường nhưng vẫn phải UAT tối thiểu ba file thực tế liên tiếp cho từng mẫu trước khi công nhận nghiệp vụ.

## 3. Nguyên tắc nhận diện

Một file được nhận diện theo ba lớp:

1. tên file khớp biểu thức chính quy của template;
2. tồn tại đúng worksheet được cấu hình;
3. tìm thấy toàn bộ header token bắt buộc sau khi chuẩn hóa chữ hoa/thường, khoảng trắng và dấu tiếng Việt.

Nếu nhiều template có số điểm nhận diện bằng nhau hoặc không có template hợp lệ, trạng thái là `UNKNOWN_TEMPLATE`. Hệ thống không tự đoán và không ghi số liệu nghiệp vụ.

## 4. Quy tắc parser và validation

- Chỉ nhận `.xlsx`; tối đa 25 MB.
- Tối đa 5.000 dòng dữ liệu và 128 cột cho một lần nhập pilot.
- Bật giới hạn an toàn đối với ZIP/XML của workbook.
- Từ chối workbook có external link; không chạy macro và không truy cập Internet trong parser.
- Giữ nguyên giá trị hiển thị của ô; công thức được đánh giá nếu có thể, lỗi công thức trở thành issue.
- Dòng không có dữ liệu không tạo record.
- Workbook thiếu worksheet, thiếu header hoặc không có dòng dữ liệu bị đánh dấu lỗi.
- Kiểm tra cột bắt buộc, kiểu số, miền giá trị, số âm và nhóm số lượng phải có ít nhất một giá trị.
- Dòng nhập mua phải có nhà cung cấp; dòng xuất bán phải có khách hàng.
- Mỗi issue lưu: severity, code, worksheet, số dòng, địa chỉ ô, trường, giá trị gốc và thông báo tiếng Việt.
- Mỗi record lưu JSON nguồn cùng `source_sheet`, `source_row`, `row_status` để không mất khả năng truy vết.
- Checksum SHA-256 phát hiện việc nhập lại cùng một file; bản trùng mang trạng thái `SUPERSEDED` và tham chiếu `duplicate_of`.

## 5. Mô hình dữ liệu

Schema PostgreSQL: `mes`.

| Bảng | Trách nhiệm |
|---|---|
| `template_definition` | Phiên bản Data Contract, field contract, SHA-256 contract hash, vòng đời, ngày hiệu lực, worksheet, header và hạn nộp |
| `import_batch` | Một lần tải file, checksum, trạng thái và tổng hợp kết quả kiểm tra |
| `import_record` | Dữ liệu theo từng dòng nguồn dưới dạng JSON có truy vết |
| `import_issue` | Danh sách lỗi/cảnh báo theo ô/dòng |
| `reporting_calendar_item` | Nghĩa vụ nộp một biểu mẫu tại một ngày báo cáo |
| `portal_submission` | Lịch sử ghi nhận việc nộp Portal và mã biên nhận |

Tất cả các bảng:

- có `tenant_id`;
- bật và ép buộc PostgreSQL Row-Level Security;
- chỉ cấp DML cho `core_app`, DDL thuộc `core_admin`;
- có khóa ngoại ghép `id + tenant_id` để không thể tham chiếu chéo tenant;
- Data Contract đã có nội dung là bất biến; thay đổi phải tạo phiên bản mới.

## 6. Máy trạng thái

```text
UPLOADED
   → IDENTIFYING → IDENTIFIED → VALIDATING
       ├─ UNKNOWN_TEMPLATE
       ├─ INVALID
       ├─ REJECTED (kỳ đã khóa)
       └─ PENDING_CONFIRMATION
              → PENDING_APPROVAL
                  ├─ REJECTED
                  └─ APPROVED → LOCKED + INTERNAL PUBLISHED
```

Quy tắc khóa:

- chỉ lô không có lỗi mới được xác nhận;
- xác nhận và phê duyệt là hai transition riêng;
- chỉ dữ liệu `APPROVED` mới được khóa;
- khóa batch và tạo bản phát hành canonical `PUBLISHED` chạy trong cùng transaction; một bước lỗi thì toàn bộ rollback;
- kỳ `LOCKED` không bị lô mới ghi đè;
- mọi transition quan trọng ghi audit log;
- phê duyệt và phát hành nội bộ phát sự kiện qua transactional outbox;
- Portal mặc định `DISABLED`; không tạo trạng thái `PORTAL_SUBMITTED`, lần gửi hoặc biên nhận khi chưa có hợp đồng tích hợp.

## 7. Phân quyền

| Resource | Action | Mục đích |
|---|---|---|
| `MES_REPORT` | `READ` | Xem template, lịch, lô và lỗi |
| `MES_REPORT` | `IMPORT` | Tải file Excel |
| `MES_REPORT` | `CONFIRM` | Xác nhận số liệu của bộ phận lập báo cáo |
| `MES_REPORT` | `APPROVE` | Phê duyệt, trả lại và khóa số liệu |
| `MES_REPORT` | `SUBMIT_PORTAL` | Quyền dự phòng cho integration boundary; không khả dụng khi Portal `DISABLED` |

Platform Administrator được phép toàn bộ nhờ wildcard của Core. Migration V21 đã seed bốn vai trò `mes-report-preparer`, `mes-report-approver`, `mes-portal-operator`, `mes-report-viewer` và policy tương ứng. Việc gán người dùng, data scope theo bộ phận và kiểm chứng nguyên tắc bốn mắt vẫn chờ ma trận nghiệp vụ; mặc định hệ thống fail-closed.

## 8. API contract

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/api/v1/mes/templates` | Danh sách Data Contract |
| `GET` | `/api/v1/mes/reporting-calendar?date=...` | Lịch báo cáo ngày có lọc trạng thái |
| `GET` | `/api/v1/mes/reporting-calendar/summary?date=...` | Tổng hợp trạng thái trong ngày |
| `POST` | `/api/v1/mes/import-batches?reportingDate=...` | Tải file multipart và đưa vào background job |
| `GET` | `/api/v1/mes/import-batches` | Lịch sử lô nhập |
| `GET` | `/api/v1/mes/import-batches/{id}` | Chi tiết lô |
| `GET` | `/api/v1/mes/import-batches/{id}/records` | Preview dữ liệu theo dòng |
| `GET` | `/api/v1/mes/import-batches/{id}/issues` | Lỗi/cảnh báo theo ô |
| `POST` | `/api/v1/mes/import-batches/{id}/confirm` | Xác nhận số liệu |
| `POST` | `/api/v1/mes/import-batches/{id}/approve` | Phê duyệt |
| `POST` | `/api/v1/mes/import-batches/{id}/reject` | Trả lại kèm lý do |
| `POST` | `/api/v1/mes/import-batches/{id}/lock` | Khóa phiên bản đã duyệt |
| `GET` | `/api/v1/mes/internal-data/summary?from=&to=` | Tổng hợp dữ liệu canonical đã phát hành |
| `GET` | `/api/v1/mes/internal-data/metrics?from=&to=&domain=&page=&size=` | Tra cứu metric và lineage nguồn |
| `GET` | `/api/v1/mes/integrations/portal/status` | Capability tích hợp Portal, chỉ đọc |
| `POST` | `/api/v1/mes/import-batches/{id}/portal-submissions` | Integration boundary; trả `409 MES_PORTAL_INTEGRATION_DISABLED` khi Portal bị tắt |

## 9. Slice 1 — Trung tâm báo cáo ngày

Màn hình `Trung tâm báo cáo ngày` là màn hình dữ liệu thật đầu tiên của MES, được gắn vào shell và Navigation Registry hiện có. Màn hình gồm:

- các chỉ số vận hành báo cáo theo ngày;
- bộ lọc ngày báo cáo và trạng thái;
- tab Lịch báo cáo;
- tab Lô nhập gần đây;
- popup nhập `.xlsx`;
- popup chi tiết lô, preview 25 dòng dữ liệu và danh sách issue;
- tab Dữ liệu chuẩn hóa đọc API canonical summary/metrics và drill-down về batch nguồn;
- các hành động workflow theo trạng thái;
- polling ngắn trong thời gian background job đang xử lý;
- tự thử refresh access token một lần khi API trả `401`, giữ đúng cơ chế session của Core;
- trạng thái loading/error được tách riêng cho trang, upload và popup chi tiết.

Các màn hình MES còn lại vẫn là prototype có nhãn `Dữ liệu mô phỏng`; Slice 1 không biến dữ liệu demo thành dữ liệu vận hành.

## 10. Tiêu chí nghiệm thu pilot

- [x] Sáu Data Contract pilot được đăng ký theo tenant.
- [x] Danh mục kỹ thuật đủ 98/98 workbook và từ điển mã suy luận được lập tại `15-mes-report-catalog-and-dictionaries-v1.0.md`.
- [x] Sáu field contract có hash, ngày hiệu lực, vòng đời và cơ chế bất biến theo phiên bản.
- [x] Bootstrap idempotent seed/backfill contract cho mọi tenant trong transaction có TenantContext/RLS khi runtime khởi động; không phụ thuộc lần mở UI đầu tiên.
- [x] File tồn kho hợp lệ được nhận diện và sinh record.
- [x] Validation bắt buộc/kiểu số/miền giá trị và external link có unit test.
- [x] File không xác định bị chặn và trả issue rõ ràng.
- [x] Dữ liệu lỗi không thể chuyển sang bước xác nhận.
- [x] Workflow xác nhận → phê duyệt → khóa và phát hành canonical nội bộ chạy qua API.
- [x] Upload cùng file trong cùng ngày tạo lô audit `SUPERSEDED`, không tạo record/job import thứ hai; cùng file ở ngày báo cáo khác vẫn hợp lệ.
- [x] Lô mới không thể thay thế kỳ đã khóa; lịch tiếp tục trỏ lô đã khóa.
- [x] Audit log và outbox được ghi trong transition quan trọng.
- [x] Application User không có quyền bị trả `403 PERMISSION_DENIED`.
- [x] Migration V20–V23, Java 21 compile, 11/11 test MES/hồi quy mục tiêu, Next.js production build và 9/9 frontend test đạt ngày 08/09/2026.
- [x] Sáu workbook nguồn thật được nhận diện đúng contract, parser trả `NO_DATA`, không làm thay đổi byte nguồn; đây là UAT cấu trúc vì cả sáu file không có dòng nghiệp vụ.
- [ ] UAT với ít nhất ba file thực tế liên tiếp cho mỗi biểu mẫu.
- [ ] Chốt Data Owner, Data Steward và người phê duyệt theo bộ phận.
- [ ] Chốt deadline và KPI sản xuất/BGĐ; các KPI hiện có mới là KPI vận hành báo cáo.
- [ ] Đối chiếu nhập – xuất – tồn và KPI nội bộ trên ba ngày dữ liệu thực liên tiếp.

## 11. Giới hạn đã biết và bước kế tiếp

- Toàn bộ delivery Portal đang pending; runtime mặc định `DISABLED`, không ghi trạng thái hoặc mã biên nhận thủ công.
- Sáu contract pilot đã có mapping vào canonical release/dimension/daily metric; Data Steward đã có API xác nhận sản phẩm/đối tác nhưng vẫn cần chủ dữ liệu nghiệp vụ xác nhận alias/danh mục.
- Đã có baseline đối soát nhập-xuất-tồn canonical và kiểm tra trạng thái master data; chưa được công nhận số liệu nghiệp vụ cho đến khi chạy đủ ba ngày dữ liệu có phát sinh.
- Chưa có scheduler chủ động sinh lịch; lịch ngày hiện được tạo lười khi API ngày đó được gọi.
- Chưa có workflow revision/mở khóa cho kỳ đã khóa.
- Antivirus adapter hiện là adapter phát triển chấp nhận file; phải thay bằng scanner thật trước production.
- Chưa có Portal contract, mapping, adapter, delivery history hoặc trạng thái biên nhận `ACCEPTED`/`REJECTED`.
- Vai trò đã seed nhưng gán người dùng, data scope và UAT bốn mắt chưa hoàn tất.
- Chưa có Data Quality Dashboard theo xu hướng.
- Chưa hỗ trợ `.xls`, file có macro hoặc định dạng ngoài danh sách pilot.
- Dashboard BGĐ đã đọc canonical release/metric và kết quả đối soát; runtime chưa có số liệu thật vì sáu workbook UAT đang trống.

Bước tiếp theo là nạp ít nhất ba ngày workbook có phát sinh, xác nhận master data bằng Data Steward và ký biên bản đối soát nhập-xuất-tồn/KPI. Portal chỉ được đưa trở lại backlog sau khi có contract được phê duyệt.
