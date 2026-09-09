# MES — Phân tích hồ sơ kỹ thuật và chuẩn hóa module v1.0

## 1. Kiểm soát tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Mã tài liệu | MES-TECH-MODULE-ANALYSIS-001 |
| Phiên bản | 1.0 |
| Ngày phân tích | 09/09/2026 |
| Nguồn phân tích | `1. BCNCKT-Mạo Khê 17122025 final nộp TKV lần 2.docx` |
| Phạm vi | Chỉ các nội dung liên quan đến xây dựng phần mềm MES |
| Trạng thái | **PHÂN TÍCH — CÁC ĐỀ XUẤT THAY ĐỔI CHỜ PHÊ DUYỆT** |
| Baseline đang có hiệu lực | `MES Solution Blueprint v1.0` |

Tài liệu nguồn là dữ liệu đầu vào để phân tích, không phải chỉ thị tự động sửa mã nguồn. Những đề xuất trong tài liệu này chưa thay đổi Blueprint, BA, backlog hoặc source MES cho tới khi chủ dự án phê duyệt.

> Cập nhật ngày 09/09/2026: đề xuất phạm vi 9 bounded context trong tài liệu này đã được mở rộng bởi quyết định MES toàn doanh nghiệp và Navigation Baseline `MES-NAV-001`. Khi có khác biệt về tên/phạm vi menu, `docs/21-mes-navigation-baseline-v1.0.md` được ưu tiên áp dụng.

## 2. Phạm vi đọc và phần bị loại trừ

### 2.1 Nội dung được sử dụng

- Hiện trạng nghiệp vụ sản xuất và báo cáo tại Công ty Than Mạo Khê.
- Mục tiêu hệ thống điều hành sản xuất.
- Kiến trúc tổng thể và quan hệ giữa MES, IOC, Data Lake, các hệ thống nguồn và TKV.
- Quy trình kế hoạch, sản xuất, kho, sàng tuyển, tiêu thụ, KCS và nghiệm thu.
- Danh sách chức năng phần mềm điều hành sản xuất tại mục V.4.6.
- Vai trò/tác nhân trong bảng use case tại mục V.4.7.
- Yêu cầu phi chức năng, bảo mật, tích hợp, kiểm thử và vận hành có ảnh hưởng trực tiếp tới MES.

### 2.2 Nội dung không đưa vào thiết kế MES

- Tổng mức đầu tư, chi phí, nguồn vốn, hiệu quả tài chính và thủ tục lựa chọn nhà thầu.
- Xây dựng phòng IOC, màn hình ghép, camera, hạ tầng mạng, thiết bị phần cứng và công trình phụ trợ.
- Cấu hình chi tiết máy chủ trong dự toán; chỉ giữ lại ý nghĩa về HA, sao lưu và phục hồi.
- Các chức năng bản đồ số, video call, camera trực tiếp và xử lý sự cố khẩn cấp thuộc IOC.
- Thiết kế nội bộ của Data Lake/Big Data/BI; MES chỉ định nghĩa hợp đồng tích hợp.
- Hệ thống an toàn thông tin cấp độ 2 ngoài các kiểm soát ứng dụng MES phải tuân thủ.

## 3. Kết luận điều hành

Hồ sơ kỹ thuật làm rõ thêm bốn trục nghiệp vụ cốt lõi của MES Than Mạo Khê:

1. **Kế hoạch và điều hành:** nhận/giao kế hoạch năm, quý, tháng, ngày; cập nhật thực hiện theo ngày và ca; điều chỉnh có kiểm soát.
2. **Dòng than và tồn kho:** nhập từ sản xuất, nhập/xuất sàng tuyển, điều chuyển, xuất tiêu thụ, kiểm kê, tồn đầu kỳ và tồn cuối.
3. **KCS, giám định và nghiệm thu:** xác nhận khối lượng, phẩm cấp/chất lượng, cập nhật số liệu giám định và chốt nghiệm thu tháng.
4. **Báo cáo và quyết định:** kế hoạch so với thực hiện, sản xuất, tiêu thụ, phẩm cấp, tồn kho và cảnh báo chậm tiến độ.

Hồ sơ liệt kê 94 dòng chức năng nhưng không nên triển khai thành 94 module hoặc 94 màn hình độc lập:

- 9 chức năng chung/dashboard;
- 8 chức năng quản trị;
- 11 chức năng danh mục;
- 11 chức năng kế hoạch/điều hành;
- 45 chức năng kho, trong đó nhiều dòng chỉ là thêm/sửa/xóa/sao chép/xuất Excel của cùng một loại chứng từ;
- 5 chức năng báo cáo/cảnh báo;
- 5 chức năng mobile.

Sau chuẩn hóa, phạm vi hợp lý là **9 bounded context MES**, dùng lại **6 nhóm năng lực Core** và có **4 biên tích hợp**. Cách này phù hợp hơn với Java Core hiện tại, quy mô đội 5–7 người và mục tiêu phát hành hai tuần/lần.

## 4. Hiện trạng nghiệp vụ rút ra từ hồ sơ

### 4.1 Vấn đề cần giải quyết

- Số liệu đang đi qua giấy, Excel, email, báo cáo trực tiếp và nhiều phần mềm rời rạc.
- Các quy trình tại phân xưởng, kho, cảng và bộ phận nghiệp vụ chưa dùng chung một mô hình dữ liệu.
- Trung tâm điều hành chủ yếu theo dõi camera/SCADA, chưa có dashboard vận hành và cảnh báo sản xuất thống nhất.
- Lãnh đạo không theo dõi xuyên suốt được kế hoạch, sản lượng, tiêu thụ và tồn kho theo thời gian cần thiết để điều hành.
- Số liệu sản xuất ban đầu có thể thay đổi sau giám định, đo đạc và nghiệm thu nhưng chưa có version, lineage và lịch sử điều chỉnh tập trung.
- Dữ liệu nằm trên máy cá nhân hoặc hệ thống nhà cung cấp làm tăng nguy cơ mất dữ liệu, sai phiên bản và khó đối chiếu.

### 4.2 Chuỗi giá trị MES cần quản lý

```text
Kế hoạch TKV/Công ty
  → giao chỉ tiêu cho đơn vị
  → thực hiện theo ca/ngày
  → nhập than từ sản xuất
  → sàng tuyển/chế biến
  → luân chuyển kho
  → KCS/giám định/nghiệm thu
  → xuất tiêu thụ/giao vận
  → đối soát kế hoạch–thực hiện–tồn–chất lượng
  → dashboard và báo cáo điều hành
  → đầu ra Portal/TKV khi tích hợp sẵn sàng
```

### 4.3 Các chủ thể nghiệp vụ

| Chủ thể | Trách nhiệm MES chính |
|---|---|
| Phòng Kế hoạch | Nhận kế hoạch cấp trên, lập/giao/điều chỉnh kế hoạch, tổng hợp và theo dõi thực hiện |
| Ban điều hành/BGĐ | Theo dõi tiến độ, sản lượng, tồn kho, tiêu thụ, cảnh báo và drill-down |
| Phân xưởng sản xuất | Nhận kế hoạch, báo cáo chỉ tiêu và sản lượng theo ca/ngày |
| Phòng KCS | Xác nhận khối lượng, cập nhật chất lượng/giám định, theo dõi sàng tuyển và giao nhận |
| Kho/cảng | Lập chứng từ nhập, xuất, điều chuyển, kiểm kê và quản lý tồn |
| Trắc địa/KCM/nghiệp vụ liên quan | Cung cấp đo đạc, xác nhận và số liệu nghiệm thu |
| Tiêu thụ/giao vận | Lệnh giao, phương tiện, xuất tiêu thụ và xác nhận bàn giao |
| Quản trị dữ liệu | Quản lý danh mục, mã chuẩn, mapping và chất lượng dữ liệu |
| Quản trị hệ thống | Tài khoản, vai trò, quyền, cấu hình, audit và vận hành |

## 5. Mô hình module chuẩn hóa đề xuất

### 5.1 Năng lực dùng lại từ Core — không xây lại trong MES

| Năng lực | Chủ sở hữu | Cách MES sử dụng |
|---|---|---|
| Đăng nhập, phiên làm việc, đổi mật khẩu | Core Identity | MES chỉ khai báo permission và data scope |
| Người dùng, vai trò, nhóm quyền | Core Access Management | Ánh xạ vai trò MES vào policy; không tạo bảng user riêng |
| Cơ cấu tổ chức | Core Organization | MES bổ sung ánh xạ đơn vị vận hành/phân xưởng, không sao chép cây tổ chức |
| Audit và lịch sử thao tác | Core Audit | Mọi thay đổi kế hoạch, chứng từ, duyệt, khóa và điều chỉnh phải audit |
| Tệp tin và chống trùng | Core File | Lưu Excel/chứng từ đính kèm bất biến, checksum và lineage |
| Job, event và outbox | Core Runtime | Import, tổng hợp, cảnh báo và tích hợp Portal/IOC chạy bất đồng bộ an toàn |

### 5.2 Chín bounded context MES

| Mã | Bounded context | Phạm vi chuẩn hóa | Nhóm chức năng nguồn |
|---|---|---|---|
| MES-M01 | Danh mục vận hành | Ca, tổ sản xuất, địa điểm hầm lò/lộ thiên, kho/cảng, sản phẩm, đối tác, phương tiện, đơn vị tính và mã trạng thái | V.4.6: 11–12, 16–17, 21–28 |
| MES-M02 | Chỉ tiêu và công thức | Nhóm chỉ tiêu, định nghĩa chỉ tiêu, đơn vị đo, grain, công thức, chiều phân tích, ngưỡng và version | V.4.6: 18–20; dashboard/báo cáo |
| MES-M03 | Kế hoạch và điều hành | Kế hoạch chỉ tiêu/sản xuất/tiêu thụ theo kỳ, giao đơn vị, version, điều chỉnh, phê duyệt và so sánh thực hiện | V.4.6: 29–31, 37–39 |
| MES-M04 | Thực hiện sản xuất | Kết quả theo ca/ngày, địa điểm, phân xưởng, sản phẩm, nguồn ghi nhận và xác nhận sản lượng | Quy trình V.4.4.2 và chức năng 32–34, 36 |
| MES-M05 | Kho và dòng than | Sổ giao dịch thống nhất cho nhập sản xuất, sàng tuyển, điều chuyển, xuất tiêu thụ; tồn đầu kỳ, kiểm kê, balance và đối soát | V.4.6: 40–84 |
| MES-M06 | KCS, chất lượng và nghiệm thu | Lô/mẫu, chỉ tiêu chất lượng, kết quả giám định, điều chỉnh có lý do, xác nhận khối lượng và chốt nghiệm thu | Quy trình V.4.4; chức năng 70–72 |
| MES-M07 | Tiêu thụ và giao vận | Thực hiện/lệnh giao, khách hàng, phương tiện, cân/giao nhận, chuyến vận chuyển và xác nhận hoàn tất; kế hoạch tiêu thụ được tham chiếu từ MES-M03 | V.4.4.3; chức năng 35, 59–64 |
| MES-M08 | Trung tâm dữ liệu ngày | Template Registry, import Excel, nhập trực tiếp, validation, workflow bốn mắt, canonical release và lịch báo cáo | Nhu cầu thay Excel/email và Blueprint hiện hành |
| MES-M09 | Báo cáo, cảnh báo và quyết định | Dashboard, plan-vs-actual, tồn, phẩm cấp, tiêu thụ, cảnh báo chậm tiến độ, drill-down và xuất báo cáo | V.4.6: 3–9, 73–79, 85–89, 92–94 |

### 5.3 Bốn biên tích hợp

| Biên | Dữ liệu trao đổi | Nguyên tắc |
|---|---|---|
| Portal/TKV | Báo cáo đã duyệt, payload/file, biên nhận, trạng thái từ chối | Đọc cùng canonical release với báo cáo nội bộ; không có bộ số liệu riêng |
| IOC | KPI, cảnh báo vận hành, trạng thái xử lý, link drill-down | IOC hiển thị/điều phối; MES sở hữu giao dịch sản xuất và bằng chứng nghiệp vụ |
| Data Lake/BI | Canonical data, master data, lineage và lịch sử | Tích hợp sau; Data Lake không phải dependency để MES vận hành giai đoạn đầu |
| Hệ thống nguồn/OT | Ca lệnh, cân điện tử, SCADA, vật tư, nhân sự, thiết bị, KCS/LIMS | Adapter có idempotency, source identity, timestamp và chất lượng dữ liệu |

### 5.4 Mobile không phải một domain module

Các dòng 90–94 trong hồ sơ là kênh truy cập, không phải bounded context. Giai đoạn đầu dùng Next.js responsive/PWA và cùng API/RBAC với web. Chỉ xây ứng dụng mobile riêng khi có use case cần offline, chụp ảnh, GPS, quét QR/RFID hoặc push notification nền đã được phê duyệt.

## 6. Chuẩn hóa 45 chức năng kho thành một mô hình giao dịch

Hồ sơ tách riêng thao tác thêm, sửa, xóa, sao chép và xuất Excel cho từng loại phiếu. Nếu triển khai trực tiếp sẽ sinh nhiều bảng/service/controller lặp lại. Đề xuất dùng aggregate thống nhất:

```text
InventoryDocument
  id, code, type, status, businessDate, unitId
  sourceLocationId, destinationLocationId, partnerId
  transportRef, weighingRef, qualityRef
  version, submittedBy, approvedBy, lockedAt

InventoryDocumentLine
  productId, lotId, quantity, unitCode
  qualitySnapshotId, sourceEvidenceId
```

`type` được kiểm soát bằng enum/reference data:

- `PRODUCTION_RECEIPT` — nhập từ sản xuất;
- `PROCESSING_RECEIPT` — nhập sau sàng tuyển/chế biến;
- `PROCESSING_ISSUE` — xuất đưa đi sàng tuyển/chế biến;
- `INTERNAL_TRANSFER` — điều chuyển kho/cảng;
- `SALES_ISSUE` — xuất tiêu thụ/giao vận;
- `OPENING_BALANCE` — số dư đầu kỳ;
- `STOCKTAKE_ADJUSTMENT` — điều chỉnh từ kiểm kê;
- `QUALITY_ADJUSTMENT` — điều chỉnh sau giám định/nghiệm thu, nếu quy tắc nghiệp vụ cho phép.

Mỗi loại dùng chung lifecycle nhưng có validation riêng:

```text
DRAFT → SUBMITTED → CONFIRMED → APPROVED → POSTED → LOCKED
                      ↘ REJECTED

POSTED/LOCKED không xóa vật lý.
Sai sót được đảo bằng reversal hoặc revision có lý do và liên kết chứng từ gốc.
```

Tồn kho phải được tính từ ledger đã `POSTED`, không cập nhật một ô tồn có thể sửa trực tiếp. Snapshot phục vụ hiệu năng có thể tái tạo từ ledger và phải giữ khóa theo `warehouse + product + lot + business_date`.

## 7. Mô hình dữ liệu nghiệp vụ mục tiêu

### 7.1 Master/reference data

- `operational_unit_mapping`, `production_team`, `work_shift`, `production_location`;
- `warehouse`, `warehouse_location`, `stockpile`, `port`;
- `metric_group`, `metric_definition`, `metric_formula_version`, `unit_of_measure`;
- `product_type`, `product_group`, `product`;
- `partner_type`, `partner_group`, `partner`, `vehicle`;
- `reason_code`, `status_code`, `source_system`, `data_quality_rule`.

Master data phải có mã bất biến, tên hiển thị, ngày hiệu lực, trạng thái, version và mapping tới mã Portal/TKV/hệ thống nguồn. Không hard-delete master đã phát sinh giao dịch.

### 7.2 Kế hoạch và thực hiện

- `plan`, `plan_version`, `plan_line`, `plan_assignment`;
- `production_shift_report`, `production_result`, `daily_operating_report`;
- `plan_adjustment`, `result_confirmation`, `monthly_acceptance`.

Grain tối thiểu cần được xác định theo từng chỉ tiêu: kỳ, ngày/ca, đơn vị, địa điểm, sản phẩm và phương pháp sản xuất. Kế hoạch đã duyệt bất biến; điều chỉnh tạo version mới, không ghi đè.

### 7.3 Kho, chất lượng và giao vận

- `inventory_document`, `inventory_document_line`, `inventory_ledger_entry`;
- `inventory_balance_snapshot`, `stocktake`, `stocktake_line`;
- `quality_lot`, `quality_sample`, `quality_result`, `quality_acceptance`;
- `delivery_order`, `transport_trip`, `weighing_event`, `delivery_confirmation`.

Khối lượng trước và sau giám định phải giữ thành các giá trị có nguồn và trạng thái riêng. Không được thay số ban đầu mà mất dấu vết.

### 7.4 Reporting/canonical

Các bảng `report_template`, `import_batch`, `source_record`, `validation_issue`, `internal_dataset_release` và `canonical_daily_metric` hiện có tiếp tục là lớp ingestion/canonical. Các domain transaction không bị thay bằng JSON staging; canonical là read model đã phát hành cho dashboard, Portal và tích hợp.

## 8. Ranh giới MES, IOC, Data Lake và Portal

| Năng lực | MES sở hữu | IOC sở hữu | Data Lake/BI sở hữu | Portal/TKV sở hữu |
|---|---:|---:|---:|---:|
| Kế hoạch, kết quả ca/ngày | Có | Xem | Nhận lịch sử | Nhận báo cáo cần nộp |
| Chứng từ kho và ledger | Có | Xem KPI/cảnh báo | Nhận lịch sử | Nhận chỉ tiêu tổng hợp nếu yêu cầu |
| KCS/giám định/nghiệm thu | Có | Xem cảnh báo | Nhận dữ liệu | Nhận biểu mẫu nếu yêu cầu |
| Cảnh báo vận hành từ quy tắc MES | Tạo và quản lý bằng chứng | Tổng hợp/điều phối | Phân tích lịch sử | Không |
| Camera, bản đồ, điều phối sự cố khẩn cấp | Không | Có | Có thể lưu metadata | Không |
| Chuẩn hóa liên hệ thống và phân tích dài hạn | Cung cấp canonical domain | Tiêu thụ | Có | Không |
| Nộp và biên nhận báo cáo cấp Tập đoàn | Chuẩn bị/gửi qua adapter | Chỉ xem trạng thái | Nhận bản sao khi cần | Có |

Điểm điều chỉnh so với hồ sơ nguồn: trong kiến trúc hiện tại, danh mục MES không chờ Data Lake cấp ngược. MES quản lý master vận hành cục bộ có mapping/version; khi Data Lake sẵn sàng mới đồng bộ theo cơ chế có chủ dữ liệu rõ ràng.

## 9. Đối chiếu với MES đang triển khai

| Năng lực chuẩn hóa | Hiện trạng source MES | Mức độ | Khoảng trống chính |
|---|---|---|---|
| Core identity/RBAC/audit/file/job | Kế thừa Core | Tốt | Cần hoàn thiện role/data-scope MES thật trong UAT |
| Trung tâm dữ liệu ngày | API/workflow/canonical Slice 0–1.2 | Có baseline kỹ thuật | Workbook có dữ liệu thật, lịch chủ động, revision/mở khóa, antivirus |
| Danh mục sản phẩm/đối tác | Data Steward API | Một phần | Kho/cảng, ca, tổ, địa điểm, chỉ tiêu, phương tiện, UOM và mapping |
| Kế hoạch và điều hành | Prototype | Chưa có domain | Plan version, giao kế hoạch, điều chỉnh, phê duyệt, plan-vs-actual |
| Thực hiện sản xuất | Prototype | Chưa có domain | Kết quả ca/ngày, location/workshop, xác nhận và liên kết kế hoạch |
| Kho và dòng than | API đối soát canonical | Một phần | Ledger, chứng từ, workflow, kiểm kê, tồn đầu kỳ, reversal |
| KCS/chất lượng/nghiệm thu | Prototype | Chưa có domain | Lot/sample/result, giám định, xác nhận khối lượng, nghiệm thu tháng |
| Tiêu thụ/giao vận | Prototype | Chưa có domain | Lệnh giao, chuyến xe/tàu, cân, bàn giao và trạng thái |
| Cảnh báo | Prototype | Chưa có engine | Rule/version, threshold, SLA, assignment, resolution và outbox |
| Dashboard/BGĐ | Đọc canonical thật | Một phần | KPI kế hoạch, ca, chất lượng, tiêu thụ và cảnh báo chưa có nguồn domain |
| Portal/TKV | Adapter bị khóa an toàn | Chờ contract | API, auth, payload, idempotency, biên nhận và đối soát |
| IOC/Data Lake/OT | Chưa triển khai | Tương lai | Hợp đồng event/API và ownership dữ liệu |
| Mobile | Web responsive | Đủ cho giai đoạn đầu | Chỉ đánh giá native app khi có use case hiện trường |

Kết luận: hồ sơ kỹ thuật không phủ nhận Blueprint hiện hành. Nó bổ sung chi tiết mạnh nhất cho ba lát cắt còn thiếu: `Kế hoạch → Thực hiện`, `Kho/dòng than` và `KCS/giám định/nghiệm thu`.

## 10. Đánh giá công nghệ và kiến trúc

### 10.1 Java và PostgreSQL

Hồ sơ đánh giá Java phù hợp cho backend lớn, bảo mật và mở rộng; PostgreSQL là lựa chọn cơ sở dữ liệu quan hệ mã nguồn mở. Hai điểm này khớp baseline Java 21/Spring Boot/PostgreSQL hiện tại.

### 10.2 Angular so với Next.js

Phần lựa chọn Angular trong hồ sơ là một đánh giá công nghệ, không phải yêu cầu chức năng hoặc ràng buộc tích hợp. MES hiện đã chốt Next.js/React/TypeScript và dùng chung shell Core. Không có lợi ích nghiệp vụ đủ lớn để viết lại frontend sang Angular; việc đổi framework lúc này làm tăng chi phí, phân tách đội ngũ và mất kiểm thử hiện có.

**Đề xuất:** giữ Next.js. Chỉ xem xét lại khi có tiêu chí bắt buộc trong hồ sơ mời thầu/hợp đồng đã phê duyệt.

### 10.3 Microservices so với modular monolith

Hồ sơ đề xuất microservices ở mức kiến trúc tổng thể. Với đội 5–7 người, chưa có SRE chuyên trách và phát hành hai tuần/lần, tách sớm thành nhiều service sẽ làm tăng chi phí CI/CD, quan sát, xử lý giao dịch phân tán, version API và vận hành on-premise.

**Đề xuất:** tiếp tục modular monolith, nhưng bắt buộc:

- package/module boundary rõ;
- domain không truy cập bảng của module khác trực tiếp;
- tích hợp qua application service, event và outbox;
- schema/table ownership rõ;
- đo coupling và chỉ tách service khi có nhu cầu tải, bảo mật hoặc vòng đời phát hành độc lập đã chứng minh.

Đây là cách giữ khả năng tách microservice về sau mà không trả chi phí phân tán quá sớm.

### 10.4 Kiến trúc dữ liệu

Hồ sơ yêu cầu tách dữ liệu tác nghiệp và dữ liệu báo cáo. Kiến trúc hiện tại đã đi đúng hướng:

```text
Domain transaction/Excel source
  → staging + validation
  → domain ledger/workflow
  → canonical release bất biến
  → dashboard/read model/Portal/IOC/Data Lake
```

Không để dashboard truy vấn trực tiếp file Excel, JSON staging hoặc bảng mutable chưa duyệt.

## 11. Yêu cầu phi chức năng áp dụng cho MES

| Nhóm | Yêu cầu rút ra | Cách áp dụng đề xuất |
|---|---|---|
| Khả dụng | Web, đa trình duyệt, dễ học, responsive | Next.js responsive; hướng dẫn theo vai trò và nghiệp vụ |
| Hiệu năng | Chức năng cơ bản phản hồi nhanh; báo cáo có SLA riêng | API CRUD/query P95 ≤ 2 giây; báo cáo đơn giản ≤ 30 giây; báo cáo nặng chạy job bất đồng bộ |
| Lưu trữ | Dữ liệu nghiệp vụ tối thiểu 3 năm | Đưa thành retention baseline tối thiểu sau khi nghiệp vụ và pháp chế phê duyệt |
| Tin cậy | Chịu lỗi, phục hồi dữ liệu, backup | Giữ baseline chặt hơn: RPO 15 phút, RTO 1 giờ; diễn tập restore định kỳ |
| Bảo mật | Nhiều lớp, RBAC/data scope, mã hóa, audit | Dùng Core PEP/PDP, TLS, secret ngoài Git, audit bất biến; SSO/MFA integration-ready |
| Session | Hồ sơ nêu tự đăng xuất sau 10–15 phút | Không hard-code; cấu hình theo nhóm người dùng/thiết bị và đánh giá thực tế ca sản xuất |
| Mật khẩu | Tối thiểu 8 ký tự, history và blacklist | Dùng chính sách Identity Core nếu chặt hơn; không hạ chuẩn Core theo văn bản cũ |
| Tích hợp | API, IPv6-ready, hệ thống bên thứ ba | Versioned API/event, idempotency key, retry/outbox, observability và contract test |
| Kiểm thử | Function, accuracy, integration, performance, security | Test pyramid, UAT workbook thật, reconciliation, SAST/DAST và backup/restore rehearsal |
| Thông báo lỗi | Chi tiết, tiếng Việt | Trả mã lỗi ổn định + thông báo Việt hóa + correlation ID; không lộ stack trace |
| Chuyển giao | Có thể mở rộng từ source | Tài liệu kiến trúc, migration, runbook, test và full source theo baseline công ty |

Yêu cầu “vận hành 24/7” là kỳ vọng thời gian phục vụ, không tự động đồng nghĩa một mức availability mới. Availability 99%, RPO 15 phút và RTO 1 giờ đang là baseline đã duyệt; muốn nâng phải có sizing, chi phí và phương án trực vận hành tương ứng.

## 12. Hồ sơ triển khai và HA

Hồ sơ nguồn đề xuất hai application server và hai database server active/standby. Đây là topology mục tiêu HA, không nên ép mọi môi trường dùng cùng cấu hình.

| Profile | Mục đích | Topology đề xuất |
|---|---|---|
| Local/Dev | Lập trình, kiểm thử chức năng | Docker Compose: frontend, backend, PostgreSQL riêng |
| UAT | Kiểm thử người dùng/dữ liệu thật | Docker/VM riêng, backup tự động, cấu hình gần production |
| Production cơ bản | On-premise ngân sách giới hạn, SLA 99% | Reverse proxy + app container có khả năng restart/rollback + PostgreSQL backup/PITR + giám sát |
| Production HA | Khi nghiệp vụ và ngân sách yêu cầu | ≥2 app instance, load balancer, PostgreSQL primary/standby, backup ngoài máy, failover runbook |

Không dùng thông số phần cứng trong hồ sơ làm sizing cố định. Sizing phải dựa vào số người dùng đồng thời, số giao dịch/ca, kích thước workbook, số năm dữ liệu, khối lượng báo cáo và RTO/RPO.

## 13. Backlog điều chỉnh đề xuất

| Thứ tự | Lát cắt | Kết quả kiểm thử được |
|---:|---|---|
| 1 | Master vận hành | CRUD/version cho ca, tổ, địa điểm, kho/cảng, UOM, chỉ tiêu; mapping Core organization |
| 2 | Thực hiện sản xuất ngày/ca | Ghi nhận, xác nhận và tổng hợp kết quả theo đơn vị/ca/ngày; dashboard sản lượng thật |
| 3 | Kế hoạch và điều hành | Plan version, giao chỉ tiêu, điều chỉnh, duyệt và plan-vs-actual |
| 4 | Ledger kho và dòng than | Chứng từ thống nhất, posted ledger, tồn, điều chuyển, kiểm kê và reversal |
| 5 | KCS/giám định/nghiệm thu | Mẫu/kết quả chất lượng, xác nhận khối lượng và chốt nghiệm thu có lineage |
| 6 | Tiêu thụ/giao vận | Lệnh giao, phương tiện/chuyến, cân và bàn giao |
| 7 | Cảnh báo và dashboard điều hành | Rule/version, cảnh báo chậm tiến độ/chênh lệch/chất lượng, SLA và drill-down |
| 8 | Báo cáo chuẩn | Kế hoạch, sản xuất, sàng tuyển, tiêu thụ, tồn và phẩm cấp từ canonical |
| 9 | Portal Gateway | Gửi cùng canonical release, idempotency, retry, biên nhận và đối soát |
| 10 | IOC/Data Lake/OT adapters | Event/API có contract test, source identity và observability |

`Trung tâm dữ liệu ngày` tiếp tục chạy xuyên suốt các lát cắt làm kênh nhập liệu tạm thời và nguồn canonical cho tới khi từng hệ thống nguồn được tích hợp.

## 14. Mười hai đề xuất chờ duyệt

| Mã | Đề xuất |
|---|---|
| MES-TA-01 | Chuẩn hóa phạm vi thành 9 bounded context MES; không triển khai 94 dòng chức năng thành 94 module |
| MES-TA-02 | Dùng lại Identity, Organization, RBAC, Audit, File, Job/Event/Outbox của Core; MES không xây bản sao |
| MES-TA-03 | Chuẩn hóa kho bằng một aggregate chứng từ + ledger, phân biệt loại giao dịch bằng type và validation |
| MES-TA-04 | Kế hoạch, kết quả, chứng từ và canonical release đã duyệt phải có version/bất biến; sửa bằng revision/reversal |
| MES-TA-05 | Tách rõ MES, IOC, Data Lake và Portal theo bảng ownership; không đưa camera/bản đồ/sự cố IOC vào domain MES |
| MES-TA-06 | MES tiếp tục vận hành độc lập Data Lake; master vận hành cục bộ có mapping/version, đồng bộ về sau qua adapter |
| MES-TA-07 | Giữ Java 21/Spring Boot/PostgreSQL và Next.js; không đổi sang Angular chỉ vì khuyến nghị trong hồ sơ |
| MES-TA-08 | Giữ modular monolith; thiết kế boundary/event để tách service về sau khi có bằng chứng cần thiết |
| MES-TA-09 | Responsive web/PWA là kênh mobile giai đoạn đầu; native app chỉ làm khi có use case hiện trường đặc thù |
| MES-TA-10 | Giữ baseline Availability 99%, RPO 15 phút, RTO 1 giờ; HA active/standby là profile theo nhu cầu và ngân sách |
| MES-TA-11 | Dữ liệu nghiệp vụ tối thiểu 3 năm và session timeout theo cấu hình là hai yêu cầu cần BA/pháp chế/an toàn thông tin duyệt |
| MES-TA-12 | Ưu tiên tiếp theo: master vận hành → thực hiện ngày/ca → kế hoạch → ledger kho → KCS/nghiệm thu |

## 15. Các điểm cần xác nhận nghiệp vụ trước khi code lát cắt mới

Không cần trả lời từng câu riêng lẻ; nhóm nghiệp vụ có thể xác nhận theo một biên bản workshop:

1. Cây đơn vị thực tế, phân xưởng, tổ, ca và địa điểm sản xuất; quan hệ với Core organization.
2. Bộ chỉ tiêu: mã, tên, đơn vị đo, grain, công thức, kỳ, owner và nguồn bằng chứng.
3. Lifecycle phê duyệt kế hoạch; ai được giao, điều chỉnh, duyệt và khóa từng cấp năm/quý/tháng/ngày.
4. Chứng từ kho thật, quy tắc đánh số, các loại nhập/xuất/điều chuyển, thời điểm hạch toán tồn và quyền đảo chứng từ.
5. Cách định danh lô than xuyên suốt sản xuất → kho → sàng tuyển → giao vận.
6. Quy tắc KCS: chỉ tiêu phẩm cấp, lấy mẫu, giám định, sai số, nghiệm thu và cách thay số tạm bằng số chính thức.
7. Quy trình cân/giao nhận, dữ liệu phương tiện và bằng chứng bàn giao.
8. Công thức cảnh báo chậm tiến độ và người chịu trách nhiệm xử lý.
9. Danh sách báo cáo BGĐ ưu tiên cùng công thức KPI đã ký xác nhận.
10. Volume và retention: giao dịch/ngày, người dùng đồng thời, workbook/ngày, dung lượng file và số năm lưu.

## 16. Tiêu chí hoàn thành bước phân tích

- Phân tách được nội dung phần mềm MES khỏi IOC, Data Lake, phần cứng và đầu tư.
- Chuyển 94 dòng chức năng thành bounded context/capability có ownership rõ.
- Đối chiếu được hồ sơ kỹ thuật với Blueprint và source MES hiện tại.
- Nêu rõ phần khớp, phần bổ sung và phần không nên áp dụng máy móc.
- Có backlog lát cắt kiểm thử được và danh sách quyết định chờ duyệt.
- Không sửa BA baseline, kiến trúc hoặc mã nguồn khi chưa có phê duyệt.

## 17. Kết luận

Hồ sơ kỹ thuật củng cố đúng hướng đang triển khai: MES phải quản lý được kế hoạch, thực hiện, dòng than, chất lượng và báo cáo mà không chờ Data Lake; dữ liệu sau kiểm tra/phê duyệt mới cấp cho dashboard và Portal. Giá trị bổ sung lớn nhất của hồ sơ là chuỗi nghiệp vụ kế hoạch–ca/ngày–kho–giám định–nghiệm thu, không phải phần đề xuất Angular, microservices hay cấu hình phần cứng.

Phương án phù hợp nhất là giữ nguyên nền Java Core/Next.js/PostgreSQL và Blueprint v1.0, sau đó bổ sung có kiểm soát 12 đề xuất `MES-TA-01` đến `MES-TA-12`. Chỉ sau khi các đề xuất được duyệt mới cập nhật BA/Blueprint/backlog và bắt đầu lát cắt code tiếp theo.
