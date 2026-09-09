# MES Solution Blueprint v1.0

## 1. Kiểm soát tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Mã tài liệu | MES-SOLUTION-BLUEPRINT-001 |
| Phiên bản | 1.0 |
| Ngày chốt | 07/09/2026 |
| Trạng thái | **ĐÃ CHỐT — BASELINE TRIỂN KHAI** |
| Người phê duyệt | Chủ dự án |
| Nền tảng | Java Core Platform, Spring Boot, Next.js, PostgreSQL, Docker |
| Phạm vi triển khai đầu tiên | Excel/nhập trực tiếp → validation → duyệt → dashboard → theo dõi nộp Portal |

Mọi thay đổi làm khác ranh giới, dữ liệu, workflow, phân quyền hoặc tiêu chí nghiệm thu trong tài liệu này phải có quyết định thay đổi hoặc ADR trước khi sửa mã nguồn.

## 2. Mục tiêu sản phẩm

MES phải sử dụng được ngay để thu thập và quản trị dữ liệu vận hành sản xuất mà không phụ thuộc Data Lake. Hệ thống thay thế quy trình tổng hợp, in giấy, gửi email hoặc gửi Zalo bằng dữ liệu có cấu trúc, được kiểm tra, phê duyệt, truy vết và trình bày trên dashboard.

Trong giai đoạn đầu:

1. Excel theo mẫu Portal là nguồn nhập liệu chính thức.
2. Người dùng được phép nhập trực tiếp trên giao diện khi chưa có file hoặc cần bổ sung số liệu.
3. PostgreSQL MES là nguồn dữ liệu vận hành đã kiểm tra và phê duyệt.
4. Người dùng tiếp tục nộp file lên Portal Tập đoàn theo quy trình hiện hành.
5. MES ghi nhận trạng thái, thời gian, người thực hiện và biên nhận nộp Portal.
6. Data Lake, SCADA, cân điện tử, KCS/LIMS và ERP là nguồn tích hợp mở rộng, không phải điều kiện để nghiệm thu giai đoạn đầu.

## 3. Bối cảnh và vấn đề cần giải quyết

- Dữ liệu đang phân tán trong 98 workbook, 241 sheet và nhiều kỳ báo cáo.
- Cùng một danh mục được sao chép trong nhiều workbook.
- Người lập báo cáo phải tổng hợp thủ công và gửi qua nhiều kênh.
- Ban Giám đốc không có một nguồn dữ liệu điều hành thống nhất và cập nhật.
- Khó biết báo cáo nào còn thiếu, đang lỗi, chờ duyệt hoặc đã gửi Portal.
- Khó truy ngược một số liệu tổng hợp về file, dòng và người nhập.
- Điều chỉnh sau duyệt hoặc sau nộp chưa có lịch sử phiên bản tập trung.
- Data Lake hiện chưa đủ chuẩn hóa để làm nguồn đầu vào tin cậy.

## 4. Nguyên tắc kiến trúc

### 4.1 Nguyên tắc nghiệp vụ

- MES quản lý dữ liệu vận hành, workflow và quyết định; Excel là kênh nhập/xuất tương thích.
- Dashboard chỉ dùng dữ liệu đã xác nhận hoặc phê duyệt.
- Không ghi đè âm thầm dữ liệu đã duyệt hoặc đã nộp Portal.
- Mọi số liệu phải truy ngược được về nguồn.
- Một dữ liệu chuẩn được tái sử dụng cho báo cáo ngày, tháng, quý và năm.
- Không tạo 98 module hoặc 98 bảng tương ứng 98 workbook.

### 4.2 Nguyên tắc kỹ thuật

- Domain MES dùng mô hình code-first và sở hữu invariant, transaction, persistence.
- Mapping biểu mẫu dùng metadata có version; metadata không thay thế domain model.
- File gốc được lưu bất biến, có checksum và audit.
- Import chạy bất đồng bộ, idempotent, có preview và lỗi theo dòng/ô.
- Core Platform giữ nguyên shell, identity, RBAC, audit, file, event/outbox và vận hành.
- Module MES chỉ đăng ký navigation, permission, API và dữ liệu của dự án.
- Adapter nguồn dữ liệu có cùng hợp đồng để Excel hiện tại có thể được thay hoặc bổ sung bởi Data Lake về sau.

### 4.3 Khung tham chiếu

- ISA-95/IEC 62264 Part 1–2: ranh giới và thông tin trao đổi giữa sản xuất và doanh nghiệp.
- ISA-95/IEC 62264 Part 3–4: hoạt động và object model của Manufacturing Operations Management.
- ISA-95/IEC 62264 Part 5–6: giao dịch và messaging giữa các hệ thống.
- Core Platform Architecture Standard v1.1: bảo mật, module boundary, database, audit, event và deployment.

## 5. Phạm vi

### 5.1 Trong phạm vi MES

MES giai đoạn 1 là nền tảng quản trị điều hành và dữ liệu của toàn bộ phòng ban. Hệ thống tiếp nhận dữ liệu bằng nhập trực tiếp/import Excel, chuẩn hóa và cung cấp cùng một canonical release cho quản trị nội bộ và Portal. Khi Data Lake hoạt động, adapter đầu vào được thay dần nhưng Dashboard, KPI, workflow, cảnh báo, lineage và Portal Gateway không đổi.

Phạm vi gồm:

1. Dashboard điều hành.
2. Kế hoạch và hiệu quả.
3. Sản xuất và điều hành.
4. Kho và dòng than.
5. Chất lượng và nghiệm thu.
6. Tiêu thụ và giao vận.
7. Vật tư và thiết bị.
8. An toàn lao động.
9. Tài chính và Kế toán quản trị.
10. Nhân sự và lao động.
11. Đầu tư và dự án.
12. Khoa học công nghệ và Chuyển đổi số.
13. Cảnh báo và chỉ đạo.
14. ESG.
15. Trung tâm báo cáo.
16. Portal/TKV.
17. Danh mục và ánh xạ dữ liệu.
18. Tích hợp và chất lượng dữ liệu.

Các module tổng hợp dữ liệu để điều hành; không mặc định thay thế phần mềm tác nghiệp hoặc sổ cái đang là nguồn dữ liệu gốc.

### 5.3 Ngoài phạm vi giai đoạn đầu

- Thay thế SCADA, PLC, DCS hoặc hệ thống điều khiển thiết bị.
- Thay thế ERP và sổ cái kế toán.
- Chuẩn hóa toàn bộ Data Lake.
- Tự động điều khiển thiết bị sản xuất.
- Tự động nộp Portal khi chưa xác định kênh tích hợp chính thức.
- Mô hình AI dự báo khi chưa có dữ liệu lịch sử đã kiểm định.

## 6. Cấu trúc điều hướng

Giữ nguyên `Trang chủ` và `Quản trị hệ thống` của Core. `MES` là menu cấp cao cùng cấp và không thay thế Trang chủ Core.

```text
Trang chủ

MES
  Dashboard điều hành
  Kế hoạch & hiệu quả
  Sản xuất & điều hành
  Kho & dòng than
  Chất lượng & nghiệm thu
  Tiêu thụ & giao vận
  Vật tư & thiết bị
  An toàn lao động
  Tài chính & Kế toán
  Nhân sự & lao động
  Đầu tư & dự án
  KHCN & Chuyển đổi số
  Cảnh báo & chỉ đạo
  ESG
  Trung tâm báo cáo
  Portal/TKV
  Danh mục & ánh xạ dữ liệu
  Tích hợp & chất lượng dữ liệu

Quản trị hệ thống
```

Menu được trả từ Navigation Registry, lọc theo permission và phạm vi đơn vị. Không hard-code danh sách menu trong frontend. Nhãn, thứ tự và số lượng menu là baseline bất biến theo `docs/21-mes-navigation-baseline-v1.0.md`; chức năng mới phải nằm bên trong các menu đã chốt.

## 7. Vai trò và phạm vi dữ liệu

| Vai trò | Quyền chính | Phạm vi |
|---|---|---|
| Ban Giám đốc | Xem dashboard, báo cáo, drill-down, cảnh báo | Toàn công ty |
| Điều hành sản xuất | Nhập/xem/xác nhận thực hiện sản xuất | Đơn vị được phân công |
| Phòng Kế hoạch | Kế hoạch, dự kiến, tổng hợp, đối chiếu | Toàn công ty hoặc phạm vi giao |
| Kho | Nhập, xuất, điều chuyển, kiểm kê | Kho/đơn vị được giao |
| KCS | Mẫu, kết quả chất lượng, nghiệm thu | Phạm vi KCS được giao |
| Tiêu thụ/Giao vận | Khách hàng, lệnh giao, lịch tàu, giao nhận | Phạm vi được giao |
| Vật tư | Nhu cầu, mua, sử dụng, tồn, phế liệu | Phạm vi vật tư được giao |
| Người xác nhận | Kiểm tra và xác nhận dữ liệu phòng ban | Báo cáo/đơn vị được giao |
| Người phê duyệt | Duyệt, từ chối, khóa, yêu cầu điều chỉnh | Báo cáo/đơn vị được giao |
| Quản trị dữ liệu | Danh mục, mapping, phiên bản biểu mẫu | Theo phân công |
| Quản trị hệ thống | Tài khoản, quyền, cấu hình, audit | Toàn hệ thống |

Permission phải kiểm tra ở API. Ẩn nút hoặc menu trên frontend không được coi là kiểm soát quyền.

## 8. Luồng vận hành hàng ngày

1. Scheduler tạo kỳ và danh sách báo cáo phải nộp.
2. MES gửi nhắc việc cho bộ phận chịu trách nhiệm.
3. Người dùng tải Excel hoặc nhập trực tiếp.
4. Hệ thống lưu file gốc và tạo import batch.
5. Template Registry nhận diện biểu mẫu và phiên bản.
6. Parser đọc dữ liệu vào staging.
7. Validation kiểm tra file, cấu trúc, dữ liệu, danh mục và nghiệp vụ.
8. Người dùng xem preview và sửa lỗi chặn.
9. Bộ phận phụ trách xác nhận số liệu.
10. MES đối soát chéo giữa sản xuất, kho, KCS, tiêu thụ và kế toán khi có dữ liệu.
11. Người có thẩm quyền phê duyệt và khóa phiên bản.
12. Dữ liệu đã duyệt cập nhật dashboard và báo cáo điều hành.
13. Người dùng tải file gốc/phiên bản được phép để gửi Portal.
14. Người dùng ghi nhận đã gửi, mã biên nhận và kết quả.
15. Khi Portal từ chối, MES tạo lần gửi mới và giữ lịch sử cũ.

## 9. Máy trạng thái

### 9.1 Import batch

```text
UPLOADED
  → IDENTIFYING
  → IDENTIFIED
  → VALIDATING
  → VALIDATED
  → PENDING_CONFIRMATION
  → PENDING_APPROVAL
  → APPROVED
  → LOCKED
```

Nhánh ngoại lệ:

```text
UNKNOWN_TEMPLATE | INVALID | REJECTED | SUPERSEDED | CANCELLED
```

### 9.2 Portal submission

```text
NOT_SUBMITTED
  → READY
  → SUBMITTING
  → SUBMITTED
  → ACCEPTED
```

Nhánh ngoại lệ:

```text
FAILED | PORTAL_REJECTED | RESUBMISSION_REQUIRED
```

Trong giai đoạn gửi thủ công, `SUBMITTING` và `SUBMITTED` được ghi nhận bởi người có quyền; khi có API, adapter cập nhật tự động.

### 9.3 Kỳ dữ liệu

```text
OPEN → CONFIRMING → APPROVING → LOCKED → ADJUSTED → RELOCKED
```

Mở lại kỳ bắt buộc có quyền, lý do, người phê duyệt và audit.

## 10. Import Excel

### 10.1 Template Registry

Mỗi biểu mẫu có:

- Mã và tên biểu mẫu.
- Phòng ban/chủ dữ liệu.
- Tần suất và hạn nộp.
- Tên sheet, vùng dữ liệu và chữ ký tiêu đề.
- Kịch bản kế hoạch/dự kiến/thực hiện.
- Phiên bản và ngày hiệu lực.
- Parser type và parser version.
- Mapping version.
- Validation rules.
- Trạng thái hoạt động.

Nhận diện không chỉ dựa vào tên file; phải dùng tên sheet, tiêu đề, số cột, ô mã cố định và chữ ký cấu trúc.

### 10.2 Nhóm parser

1. Tabular parser: dữ liệu dạng dòng/cột.
2. Multi-header parser: tiêu đề nhiều tầng.
3. Matrix parser: báo cáo chéo nhiều cột.
4. Master/reference parser: danh mục và bảng ánh xạ.

Không tạo một controller/service riêng cho từng workbook.

### 10.3 Lưu file và lineage

File gốc được lưu bất biến với:

- Checksum SHA-256.
- Kích thước, MIME type, tên gốc.
- Người tải, thời điểm, đơn vị và kỳ.
- Template/version được nhận diện.
- Trạng thái kiểm tra và phê duyệt.

Mỗi giá trị chuẩn hóa phải truy ngược được bằng `source_file_id`, `source_sheet`, `source_row`, `source_cell` hoặc `source_range`, `import_batch_id` và `mapping_version`.

### 10.4 Kiểm tra an toàn

- Chỉ nhận định dạng được allowlist.
- Từ chối macro và workbook đặt mật khẩu trong giai đoạn đầu.
- Chặn external link, DDE, embedded object nguy hiểm và zip bomb.
- Scan malware trước khi parse.
- Parser chạy với giới hạn kích thước, CPU, RAM và timeout.
- Không gọi Internet trong worker xử lý workbook.

### 10.5 Công thức

- Đọc cả công thức và cached value.
- Phát hiện cached value thiếu hoặc có dấu hiệu cũ.
- Không chạy macro.
- Khi cần tính lại, dùng worker LibreOffice cô lập; không chạy trong API process.
- File được tính lại chỉ phục vụ parse, không thay file gốc.

### 10.6 Validation

1. File: định dạng, dung lượng, an toàn.
2. Template: sheet, tiêu đề, phiên bản, kỳ.
3. Data type: số, ngày, mã, trường bắt buộc.
4. Master data: sản phẩm, chỉ tiêu, đơn vị, đối tác, kho.
5. Business rule: dấu số, kỳ, miền giá trị, cân đối.
6. Cross-report: sản xuất–kho–KCS–tiêu thụ–kế toán.

Lỗi hiển thị tối thiểu: file, sheet, dòng/ô, trường, giá trị nhận được, mã lỗi, mô tả và cách xử lý.

### 10.7 Chống nhập trùng

Idempotency business key gồm:

```text
template_code + reporting_entity + organization_unit
+ period + scenario + approved_version
```

- Trùng checksum: không tạo batch mới.
- Trùng kỳ khi bản cũ còn nháp: cho phép thay thế có audit.
- Bản đã duyệt: tạo revision, không ghi đè.
- Bản đã gửi Portal: điều chỉnh cần lý do và lần gửi mới.

## 11. Nhập trực tiếp

- Form được sinh theo use case nghiệp vụ, không sao chép nguyên bố cục Excel.
- Cùng validation, permission, workflow và database với import Excel.
- Có autosave bản nháp và cảnh báo rời trang.
- Cho phép sao chép dữ liệu kỳ trước khi nghiệp vụ cho phép.
- Cho phép nhập hàng loạt theo bảng với paste từ Excel.
- Ghi rõ nguồn `MANUAL_ENTRY` và người nhập.

## 12. Capability map

### 12.1 Dashboard điều hành

- KPI sản lượng than nguyên khai, than sạch, mét lò, đất bóc và giao tuyển.
- Tiến độ ngày/tháng và lũy kế so với kế hoạch.
- Tiêu thụ, tồn kho, hàng đang đi đường.
- Chất lượng và chỉ tiêu vượt ngưỡng.
- Báo cáo thiếu/chậm/lỗi/chờ duyệt.
- Cảnh báo cần quyết định.
- Drill-down đến đơn vị, sản phẩm, kỳ và nguồn.

### 12.2 Trung tâm báo cáo ngày

- Lịch báo cáo.
- Hộp việc theo trạng thái.
- Upload đơn lẻ và hàng loạt.
- Preview, validation và sửa lỗi.
- Xác nhận, duyệt, từ chối, khóa.
- Ghi nhận nộp Portal và biên nhận.
- Lịch sử phiên bản và audit.

### 12.3 Kế hoạch và dự báo

- Kế hoạch Tập đoàn giao, kế hoạch công ty và kế hoạch điều hành.
- Kế hoạch năm/quý/tháng.
- Dự kiến thực hiện.
- Giao kế hoạch theo đơn vị.
- Quản lý version và revision.
- So sánh kế hoạch–thực hiện.

### 12.4 Điều hành sản xuất

- Than nguyên khai, than sạch, mét lò, đất bóc, giao tuyển và pha trộn/chế biến.
- Kết quả ngày/ca nếu nguồn có dữ liệu ca.
- Tổng hợp tháng/quý/năm.
- Xác nhận theo đơn vị.
- Tiến độ và sai lệch.

### 12.5 Kho và luân chuyển

- Nhập từ sản xuất, nhập mua, nhập chế biến.
- Xuất tiêu thụ, xuất chế biến, điều chuyển.
- Tồn đầu/cuối, hàng đang đi đường.
- Kiểm kê và điều chỉnh.
- Truy vết sản phẩm/lô.

### 12.6 KCS và chất lượng

- Mẫu, lô, nguồn mẫu.
- Độ ẩm, độ tro, nhiệt trị, chất bốc, tỷ lệ cục/đá/kẹp sít và chỉ tiêu mở rộng.
- Tiêu chuẩn áp dụng và giới hạn.
- Xác nhận, nghiệm thu và cảnh báo không đạt.
- Liên kết giao dịch kho và giao vận.

### 12.7 Tiêu thụ và giao vận

- Kế hoạch tiêu thụ.
- Lệnh giao và dòng hàng.
- Khách hàng, sản phẩm, tàu/phương tiện và nhà máy.
- Ngày dự kiến/thực tế, khối lượng và trạng thái giao.
- Đối soát xuất kho và chất lượng lô giao.

### 12.8 Vật tư

- Nhu cầu, kế hoạch mua, nhập, xuất, sử dụng và tồn.
- Luân chuyển và vật tư thu hồi.
- Phế liệu.
- Số lượng, đơn giá và giá trị.
- Theo dõi định mức và sai lệch.

### 12.9 Báo cáo và đối soát

- Báo cáo điều hành trên web.
- Xuất Excel/PDF.
- Report definition và mapping có version.
- Đối soát ngày–tháng–quý–năm.
- Đối soát sản xuất–kho–KCS–tiêu thụ–kế toán.
- Theo dõi SLA báo cáo và Portal submission.

## 13. Frontend blueprint

### 13.1 Application shell

- Next.js App Router chính thức.
- Kế thừa shell, màu xanh chuyển đổi, đăng nhập và quản trị Core.
- Desktop-first; responsive cho tablet.
- Tab làm việc giữ trạng thái khi chuyển module.
- Route trực tiếp phải refresh không lỗi.

### 13.2 Page patterns

1. Dashboard: KPI, biểu đồ, cảnh báo, drill-down.
2. Work queue: dữ liệu chờ nhập/xác nhận/duyệt/xử lý lỗi.
3. Data grid: server pagination, filter, sort, saved view, export.
4. Detail: header, trạng thái, tab nghiệp vụ, lịch sử, file và audit.
5. Import wizard: chọn mẫu, upload, preview, lỗi, xác nhận.
6. Form: create/edit theo permission và trạng thái.
7. Reconciliation workspace: hai nguồn, chênh lệch, nguyên nhân, xử lý.
8. Report viewer: filter, chart/table, drill-down và export.

### 13.3 Yêu cầu danh sách dữ liệu

- STT theo trang và tổng số bản ghi.
- Pagination/filter/sort phía server.
- Sticky header và cột thao tác.
- Chọn cột và lưu bộ lọc cá nhân.
- Bulk selection/bulk action theo quyền.
- Detail drawer và mở thành tab.
- Loading, empty, error, unauthorized và stale state đầy đủ.
- Không tải toàn bộ dữ liệu về browser để lọc.

### 13.4 Trạng thái dữ liệu

UI phải phân biệt rõ dữ liệu demo, nháp, chờ xác nhận, chờ duyệt, đã duyệt, đã khóa và đã gửi Portal. Dữ liệu demo không được tồn tại trong profile production.

## 14. Backend blueprint

### 14.1 Bounded contexts/package đề xuất

```text
vn.coreplatform.mes.dashboard
vn.coreplatform.mes.reportingcalendar
vn.coreplatform.mes.template
vn.coreplatform.mes.ingestion
vn.coreplatform.mes.masterdata
vn.coreplatform.mes.planning
vn.coreplatform.mes.production
vn.coreplatform.mes.inventory
vn.coreplatform.mes.quality
vn.coreplatform.mes.logistics
vn.coreplatform.mes.material
vn.coreplatform.mes.reconciliation
vn.coreplatform.mes.portal
```

Không cho context truy cập repository nội bộ của context khác. Trao đổi bằng application service contract hoặc event/outbox.

### 14.2 API groups

```text
/api/v1/mes/dashboard
/api/v1/mes/reporting-calendar
/api/v1/mes/templates
/api/v1/mes/import-batches
/api/v1/mes/master-data
/api/v1/mes/plans
/api/v1/mes/production
/api/v1/mes/inventory-movements
/api/v1/mes/stock-balances
/api/v1/mes/quality-inspections
/api/v1/mes/shipments
/api/v1/mes/materials
/api/v1/mes/reconciliations
/api/v1/mes/reports
/api/v1/mes/portal-submissions
```

API dùng pagination, problem details, correlation ID, permission enforcement, idempotency key và optimistic locking theo Core standard.

### 14.3 Pipeline import

```text
Upload API
  → File Service/quarantine
  → Malware scan
  → Template identification
  → Async parsing job
  → Staging
  → Validation
  → Preview API
  → Confirmation command
  → Domain transaction
  → Audit + outbox
  → Dashboard/read-model refresh
```

## 15. Mô hình dữ liệu

### 15.1 Master Data

- `mes_reporting_entity`, `mes_organization_unit_ref`.
- `mes_product`, `mes_product_category`, `mes_product_hierarchy`.
- `mes_indicator`, `mes_indicator_hierarchy`.
- `mes_unit_of_measure`, `mes_unit_conversion`.
- `mes_customer`, `mes_supplier`, `mes_counterparty`.
- `mes_warehouse`, `mes_stock_location`.
- `mes_mining_method`, `mes_material_origin`, `mes_transaction_type`.
- `mes_vessel`, `mes_transport_asset`, `mes_plant`.
- `mes_quality_parameter`, `mes_quality_specification`.
- `mes_calendar_period`, `mes_data_scenario`.

### 15.2 Import và Template

- `mes_template_definition`, `mes_template_version`.
- `mes_template_mapping`, `mes_validation_rule`.
- `mes_import_batch`, `mes_import_file`.
- `mes_import_row`, `mes_import_value`, `mes_import_error`.

### 15.3 Nghiệp vụ

- `mes_plan_header`, `mes_plan_line`, `mes_plan_revision`.
- `mes_production_event`, `mes_production_output`.
- `mes_inventory_movement`, `mes_inventory_movement_line`.
- `mes_stock_snapshot`, `mes_stock_in_transit`.
- `mes_quality_sample`, `mes_quality_result`, `mes_quality_acceptance`.
- `mes_shipment`, `mes_shipment_line`, `mes_vessel_schedule`.
- `mes_material_demand_plan`, `mes_material_movement`, `mes_material_snapshot`.

### 15.4 Báo cáo và Portal

- `mes_report_definition`, `mes_report_version`, `mes_report_mapping`.
- `mes_report_snapshot`.
- `mes_reconciliation_run`, `mes_reconciliation_issue`.
- `mes_portal_submission`, `mes_portal_submission_attempt`, `mes_portal_receipt`.

Các bảng kế thừa tenant, audit, version, created/updated metadata và soft-state theo Core convention. Dữ liệu đã tham gia báo cáo không hard-delete.

## 16. Dashboard và KPI

### 16.1 Dashboard Ban Giám đốc

- Sản lượng than nguyên khai và than sạch ngày/tháng.
- Mét lò, đất bóc và giao tuyển.
- Tiến độ kế hoạch theo chỉ tiêu/đơn vị.
- Tiêu thụ và lũy kế.
- Tồn kho, hàng đang đi đường và số ngày tồn ước tính.
- Chất lượng bình quân và số lô không đạt.
- Báo cáo còn thiếu, trễ, lỗi hoặc chờ duyệt.
- Cảnh báo cần quyết định.

### 16.2 KPI contract

Mỗi KPI bắt buộc có:

- Mã, tên và định nghĩa.
- Công thức và đơn vị tính.
- Hạt dữ liệu và kỳ tổng hợp.
- Nguồn và chủ dữ liệu.
- Điều kiện dữ liệu được tính.
- Ngưỡng cảnh báo.
- Drill-down target.
- Phiên bản/ngày hiệu lực.

Không đưa KPI lên production khi chưa có contract và kiểm thử đối chiếu độc lập.

## 17. Quy tắc nghiệp vụ tối thiểu

- Mã danh mục phải tồn tại, hoạt động và còn hiệu lực tại kỳ dữ liệu.
- Không trộn đơn vị tính khi chưa có conversion rule.
- Tồn cuối = tồn đầu + nhập − xuất ± điều chỉnh.
- Điều chỉnh tồn bắt buộc có lý do và phê duyệt.
- Lô KCS phải liên kết sản phẩm, nguồn mẫu và thời điểm.
- Xuất tiêu thụ phải liên kết khách hàng, sản phẩm và giao vận khi có.
- Tổng hợp tháng/quý/năm phải chỉ ra nguồn ngày hoặc adjustment.
- Kỳ đã khóa không cho sửa trực tiếp.
- Bản ghi lỗi không được cập nhật dashboard đã duyệt.
- Mọi transition phải kiểm tra permission, trạng thái hiện tại và version.

## 18. An toàn, audit và quản trị dữ liệu

- Đăng nhập nội bộ và session theo Core.
- RBAC kết hợp data scope theo đơn vị, báo cáo và trạng thái.
- File upload qua kiểm tra an toàn trước parse.
- Mã hóa TLS khi truyền; secret không nằm trong source.
- Audit các hành động upload, validate, xác nhận, duyệt, từ chối, mở/khóa kỳ, xuất và nộp Portal.
- Audit chứa correlation ID, actor, thời gian, đối tượng, trước/sau và lý do.
- Outbox phát event sau khi transaction nghiệp vụ commit.
- Backup đáp ứng RPO 15 phút, RTO 1 giờ và availability mục tiêu 99% theo baseline Core.

## 19. Yêu cầu phi chức năng

| Hạng mục | Baseline |
|---|---|
| Availability | 99% |
| RPO | 15 phút |
| RTO | 1 giờ |
| Dashboard | p95 không quá 3 giây với bộ lọc thông thường |
| Danh sách | p95 không quá 2 giây, dùng server pagination |
| Import | Bất đồng bộ, có progress; không khóa request HTTP dài |
| File limit | Cấu hình theo môi trường; baseline đề xuất 25 MB/file |
| Concurrency | Optimistic locking cho chỉnh sửa/phê duyệt |
| Observability | Structured log, metrics, health, correlation ID, job status |
| Compatibility | Chrome/Edge phiên bản được công ty hỗ trợ |
| Deployment | Docker local/on-premise; Kubernetes-ready về sau |

Các con số hiệu năng phải được xác nhận lại bằng dữ liệu khối lượng thực tế trước production.

## 20. Lát cắt triển khai

### Slice 0 — Data contract và pilot

- Lập ma trận 98 biểu mẫu: owner, kỳ, deadline, approver, nguồn và mức ưu tiên.
- Chốt từ điển mã `_00` đến `_05`, `T_TCKT_*` và danh mục Portal.
- Chọn chuỗi báo cáo ngày pilot.
- Chốt KPI BGĐ dùng trong pilot.

### Slice 1 — Trung tâm báo cáo ngày

- Template Registry.
- Upload, file storage, identification.
- Import batch, staging, validation, preview.
- Reporting calendar và work queue.
- Xác nhận/phê duyệt/khóa.
- Audit và lineage.

### Slice 2 — Dashboard BGĐ

- KPI contract.
- Read model lấy dữ liệu đã duyệt.
- Filter, drill-down và cảnh báo.
- Xuất báo cáo điều hành.

Baseline ngày 08/09/2026 đã triển khai read model canonical, bộ lọc ngày, cảnh báo master data và đối soát nhập-xuất-tồn. KPI chuyên ngành, drill-down đầy đủ và xuất báo cáo điều hành chỉ được nghiệm thu sau UAT tối thiểu ba ngày dữ liệu có phát sinh.

### Slice 3 — Chuỗi sản xuất ngày

```text
Thực hiện sản xuất
→ nhập kho than nguyên khai
→ KCS
→ tồn kho ngày
→ xuất kho than sạch
→ tiêu thụ/giao vận
→ đối soát
```

Đối soát canonical hiện bao phủ tồn đầu, nhập, xuất, tồn tính toán và tồn báo cáo theo sản phẩm. Kết quả thiếu contract hoặc master data chưa xác nhận phải mang trạng thái riêng, không được tự động coi là cân.

### Slice 4 — Kế hoạch và dự báo

- Plan version/scenario.
- Kế hoạch năm/quý/tháng.
- Kế hoạch điều hành và dự kiến.
- So sánh kế hoạch–thực hiện.

### Slice 5 — Portal

- Theo dõi submission thủ công trước.
- Report Mapping Engine và sinh file khi mapping đã được xác nhận.
- API/SFTP/RPA chỉ triển khai sau khi khảo sát kênh Portal.

### Slice 6 — Vật tư và module mở rộng

- Vật tư/MRO.
- Tài chính điều hành.
- Đầu tư, KHCN và CNTT theo dự án/module riêng.

### Slice 7 — Nguồn tự động

- Cân, SCADA, KCS/LIMS, ERP và Data Lake qua adapter.
- Không thay đổi workflow, canonical model và dashboard đã triển khai.

## 21. Tiêu chí nghiệm thu giai đoạn đầu

1. Upload được biểu mẫu pilot và giữ đúng file gốc.
2. Nhận diện đúng template/version; template lạ không được tự ghi dữ liệu.
3. Lỗi hiển thị đúng sheet/dòng/ô/trường và cách khắc phục.
4. Import lại không tạo dữ liệu trùng.
5. Xác nhận, duyệt, từ chối và khóa kỳ đúng ma trận quyền.
6. Dữ liệu đã duyệt cập nhật dashboard; bản nháp/lỗi không cập nhật.
7. KPI drill-down đến bản ghi và file nguồn.
8. Điều chỉnh tạo revision và giữ lịch sử cũ.
9. Ghi nhận được trạng thái và biên nhận nộp Portal.
10. Audit đầy đủ cho thao tác quan trọng.
11. Refresh URL/module không lỗi; tab giữ trạng thái hợp lý.
12. Docker local khởi động và chạy được test backend/frontend/database.

## 22. Definition of Done cho một biểu mẫu

Một biểu mẫu chỉ được coi là hoàn thành khi có đủ:

- Template definition/version.
- Mapping và parser được review.
- Master data dependency.
- Validation kỹ thuật và nghiệp vụ.
- Import preview và lỗi có thể xử lý.
- Commit vào domain đúng transaction.
- Permission và data scope.
- Audit và lineage.
- API contract và OpenAPI.
- UI loading/empty/error/success.
- Unit, integration, authorization và duplicate tests.
- Đối chiếu kết quả với file mẫu độc lập.
- Hướng dẫn người sử dụng.

## 23. Các quyết định baseline đã chốt

| Mã | Quyết định |
|---|---|
| MES-BP-01 | MES phải dùng được ngay, không chờ Data Lake. |
| MES-BP-02 | Excel Portal và nhập trực tiếp là nguồn đầu vào giai đoạn đầu. |
| MES-BP-03 | PostgreSQL MES lưu dữ liệu chuẩn đã kiểm tra/phê duyệt. |
| MES-BP-04 | File gốc được lưu bất biến và truy vết bằng checksum. |
| MES-BP-05 | Dashboard chỉ dùng dữ liệu đã xác nhận/phê duyệt. |
| MES-BP-06 | Không dựng 98 module/bảng tương ứng 98 workbook. |
| MES-BP-07 | Template Registry và Mapping Engine có version. |
| MES-BP-08 | Chỉnh sửa sau duyệt dùng revision, không ghi đè. |
| MES-BP-09 | Giai đoạn đầu tiếp tục nộp Portal theo cách hiện hành và ghi nhận kết quả trong MES. |
| MES-BP-10 | Kết nối Portal tự động chỉ thực hiện sau khi xác định API/SFTP/import/RPA. |
| MES-BP-11 | Data Lake, SCADA, cân, LIMS và ERP là adapter tương lai dùng chung ingestion contract. |
| MES-BP-12 | Giữ nguyên Trang chủ, Quản trị hệ thống và shell của Core. |
| MES-BP-13 | MES là menu cấp cao cùng cấp, nghiệp vụ con nằm trong MES. |
| MES-BP-14 | Frontend dùng Next.js, server pagination, work queue, tabs và drill-down. |
| MES-BP-15 | Permission kiểm tra ở backend và giới hạn theo phạm vi dữ liệu. |
| MES-BP-16 | Lát cắt đầu tiên là trung tâm báo cáo ngày và dashboard BGĐ. |
| MES-BP-17 | Chuỗi pilot nghiệp vụ là sản xuất → kho → KCS → tồn → xuất → tiêu thụ → đối soát. |
| MES-BP-18 | Dữ liệu demo không được nạp trong profile production. |
| MES-BP-19 | ISA-95/IEC 62264 là khung tham chiếu, quy trình TMK là nguồn BA quyết định. |
| MES-BP-20 | Mọi thay đổi baseline phải có quyết định/ADR và cập nhật tài liệu cùng mã nguồn. |

## 24. Hạng mục discovery còn phải hoàn thiện

Các mục sau không làm thay đổi baseline nhưng phải hoàn thành trước khi code biểu mẫu tương ứng:

- Chủ sở hữu, deadline, người xác nhận và người duyệt của từng biểu mẫu.
- Quy tắc chính thức của các mã loại dữ liệu và mã báo cáo.
- Nguồn thực tế và hạt dữ liệu của từng chỉ tiêu.
- Công thức KPI được BGĐ phê duyệt.
- Ngưỡng sai số và quy tắc đối soát.
- Hình thức Portal hỗ trợ và dữ liệu biên nhận.
- Thời gian lưu dữ liệu/file theo quy định doanh nghiệp.
- Khối lượng người dùng, file và bản ghi để kiểm thử tải.

## 25. Quan hệ với tài liệu khác

- `01-business-analysis-v1.0.md`: BA gate và trạng thái phê duyệt.
- `12-mes-bi-portal-assessment-v1.0.md`: kiểm kê, phân tích bộ Excel và cơ sở đề xuất.
- `04-data-and-integration-contracts-v1.0.md`: nguyên tắc contract nguồn dữ liệu.
- `11-mes-frontend-implementation-v1.0.md`: lát cắt frontend prototype hiện tại.
- `05-implementation-status-v1.0.md`: hiện trạng triển khai.
- `19-mes-demo-current-module-gap-analysis-v1.0.md`: ma trận đối chiếu demo cũ với module/API hiện tại.
- `20-mes-technical-feasibility-module-standardization-analysis-v1.0.md`: phân tích hồ sơ kỹ thuật BCNCKT, chuẩn hóa module và các đề xuất chờ duyệt.
- `21-mes-navigation-baseline-v1.0.md`: cấu trúc điều hướng MES bất biến đã được chủ dự án chốt.
- `core-platform-architecture-standard-v1.1.md`: baseline Core bắt buộc.

## 26. Phụ lục quyết định ngày 08/09/2026 — ưu tiên dữ liệu nội bộ

Phụ lục này không xóa baseline đã duyệt mà điều chỉnh thứ tự triển khai theo quyết định mới nhất:

1. `MES-BP-09` tạm thời **superseded trong giai đoạn hiện tại**. Cả tự động gửi lẫn ghi nhận thủ công kết quả nộp Portal đều pending; runtime dùng `MES_PORTAL_MODE=DISABLED` và không được tạo trạng thái/biên nhận Portal giả.
2. Đích nghiệm thu hiện tại là `LOCKED + INTERNAL PUBLISHED`: thao tác khóa và tạo bản phát hành canonical diễn ra nguyên tử, có lineage và chỉ sinh một release cho mỗi batch.
3. Dashboard và báo cáo nội bộ chỉ đọc canonical release `PUBLISHED`, không đọc trực tiếp file Excel hoặc JSON staging.
4. Chống trùng sử dụng khóa nghiệp vụ `tenant_id + reporting_date + checksum_sha256`. Cùng file trong cùng ngày tạo batch audit `SUPERSEDED`; cùng file ở ngày báo cáo khác được tiếp nhận.
5. Các bước vận hành Portal 13–15, máy trạng thái Portal và tiêu chí nghiệm thu số 9 được chuyển sang **Slice 5 — Portal**; chúng không còn là gate của Slice 1/Slice 1.1.
6. Khi Portal cung cấp hợp đồng tích hợp được phê duyệt, adapter Portal phải đọc đúng bản phát hành canonical mà hệ thống nội bộ đang dùng. Không xây thêm mô hình dữ liệu song song.

Chi tiết triển khai và gate xem tại `16-mes-internal-data-and-portal-readiness-v1.0.md`.

## 27. Phụ lục quyết định ngày 09/09/2026 — MES hai đầu ra

1. MES có hai trách nhiệm sản phẩm ngang hàng: cung cấp dữ liệu quản trị nội bộ khi chưa có Data Lake và tự động gửi dữ liệu báo cáo sang Portal khi API sẵn sàng.
2. Hai đầu ra phải dùng chung ingestion, validation, approval, master data, đối soát và canonical release. Không duy trì một bộ số nội bộ và một bộ số Portal độc lập.
3. Giai đoạn chuyển tiếp vẫn yêu cầu nhân sự đăng nhập Portal và import Excel thủ công. MES tiếp nhận cùng biểu mẫu để xây dữ liệu nội bộ; hệ thống chưa được ghi nhận `DELIVERED` nếu không có biên nhận Portal thật.
4. Kiến trúc đích: người dùng nhập một lần vào MES, dữ liệu qua bốn mắt và khóa phát hành, sau đó Portal Gateway tự sinh payload/file, gửi bằng API, nhận biên nhận và đối soát.
5. Slice 5 — Portal vẫn pending triển khai kỹ thuật cho tới khi nhận đủ hợp đồng API, nhưng là phạm vi bắt buộc của sản phẩm MES hoàn chỉnh.
6. Chi tiết vận hành và readiness checklist nằm tại `18-mes-dual-output-internal-portal-operating-model-v1.0.md`.
