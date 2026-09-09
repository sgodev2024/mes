# ĐÁNH GIÁ BỘ BIỂU MẪU BI/PORTAL VÀ KIẾN TRÚC DỮ LIỆU MES TMK

**Mã tài liệu:** MES-BI-PORTAL-ASSESSMENT  
**Phiên bản:** 1.0  
**Ngày lập:** 07/09/2026  
**Trạng thái:** Đề xuất BA/kiến trúc, chưa phải phê duyệt triển khai  
**Phạm vi:** 98 workbook Excel trong gói `mau bao cao BI TMK.rar`

## 1. Mục tiêu

Tài liệu này phân tích bộ biểu mẫu Excel hiện đang dùng để nhập báo cáo vào Portal, xác định bản chất dữ liệu, ranh giới nghiệp vụ và kiến trúc mục tiêu cho MES Than Mạo Khê.

Mục tiêu không phải là dựng lại từng file Excel thành một màn hình. Mục tiêu là hình thành một nguồn dữ liệu chuẩn duy nhất, từ đó MES có thể:

1. Nhận dữ liệu từ hệ thống nguồn hoặc nhập bổ sung.
2. Kiểm tra, đối soát, duyệt và khóa số liệu.
3. Sinh lại đúng biểu mẫu Portal theo từng phiên bản.
4. Gửi dữ liệu, nhận kết quả và theo dõi lịch sử nộp báo cáo.
5. Tái sử dụng cùng một dữ liệu cho dashboard ngày, tháng, quý và năm.

## 2. Kết quả kiểm kê

| Nhóm hồ sơ | Workbook | Sheet | Số dòng vật lý | Ô có dữ liệu | Công thức |
|---|---:|---:|---:|---:|---:|
| Ban KCL: CNTT, khoa học và công nghệ | 7 | 16 | 1.191 | 2.754 | 1.729 |
| Ban Kế hoạch | 24 | 51 | 9.015 | 27.226 | 6.995 |
| Ban Đầu tư | 2 | 2 | 13 | 36 | 0 |
| Ban Sản xuất - Tiêu thụ than | 48 | 151 | 23.953 | 80.269 | 22.069 |
| Ban Vật tư thương mại | 7 | 11 | 676 | 2.247 | 793 |
| Ban Kế toán - Tài chính | 10 | 10 | 1.633 | 7.694 | 3.115 |
| **Tổng** | **98** | **241** | **36.481** | **120.226** | **34.701** |

Các đặc điểm kỹ thuật đáng chú ý:

- 91 sheet mang tên `Template_Import`, 7 sheet mang tên `Template`.
- 77/98 workbook có công thức.
- 25.559 công thức tham chiếu chéo sheet.
- Các hàm xuất hiện nhiều nhất: `IFERROR` 26.164 lần, `VLOOKUP` 25.551 lần, `IF` 5.240 lần và `SUM` 2.108 lần.
- Không có workbook nào định nghĩa Excel Table chính thức. Các vùng dữ liệu phụ thuộc ô và vị trí dòng.
- Danh mục sản phẩm `DSSP` xuất hiện trong 48 workbook; `CONGTY` và `DM` cùng xuất hiện trong 19 workbook; `DSCT` xuất hiện trong 15 workbook.
- Có 4 sheet ẩn. Chỉ có một số rất ít quy tắc data validation có thể đọc trực tiếp; một phần validation sử dụng extension riêng của Excel.
- Không có file trùng hoàn toàn. Có một cặp cùng tên và cùng cấu trúc `4100_KH_SX_Nam_Giaotuyen_03.xlsx` nằm đồng thời trong thư mục kế hoạch năm và kế hoạch quý; cần xác nhận có phải đặt nhầm biểu mẫu hay không.
- Ngoài 98 workbook, gói còn có 11 file DOCX và 2 PDF. Chúng không thuộc phạm vi phân tích Excel này; các tài liệu có dấu hiệu chứa thông tin tài khoản không được đọc hoặc đưa vào báo cáo.

## 3. Kết luận nghiệp vụ

### 3.1 Đây không chỉ là bộ báo cáo MES

Bộ file bao phủ nhiều bounded context:

1. **MES vận hành:** kế hoạch sản xuất, thực hiện sản xuất, nhập/xuất/tồn, chất lượng, tiêu thụ và giao vận.
2. **Lập kế hoạch và quản trị hiệu suất:** kế hoạch năm/tháng/quý, dự kiến, thực hiện, chỉ tiêu điều hành.
3. **Vật tư/MRO:** nhu cầu vật tư, mua sắm, luân chuyển, tồn và phế liệu.
4. **Tài chính - kế toán:** giá trị, giá bình quân, doanh thu nội bộ, nhập/xuất/tồn theo giá trị và các báo cáo tài chính quản trị.
5. **Đầu tư/CAPEX:** kế hoạch đầu tư, thực hiện và giải ngân.
6. **Khoa học công nghệ/CNTT:** nhiệm vụ, hợp đồng, tài sản, sản phẩm, tiến độ và nhu cầu CNTT/chuyển đổi số.

Do đó không nên đưa toàn bộ 98 biểu mẫu vào một module MES đơn khối. MES V1 nên quản lý dữ liệu vận hành cốt lõi; tài chính, đầu tư, KHCN và CNTT là module mở rộng hoặc nguồn tích hợp.

### 3.2 Excel hiện là đồng thời ba lớp

Mỗi workbook hiện đang kiêm nhiệm:

- Lớp nhập dữ liệu.
- Lớp danh mục tham chiếu.
- Lớp công thức tổng hợp và định dạng nộp Portal.

Thiết kế này phù hợp với thao tác thủ công nhưng không phù hợp làm mô hình dữ liệu hệ thống. Khi danh mục thay đổi, công thức và mã tham chiếu trong nhiều file có thể lệch nhau.

### 3.3 Dữ liệu có nhiều chiều dùng chung

Các biểu mẫu lặp lại các chiều sau:

- Công ty/đơn vị báo cáo, hiện chủ yếu là mã `4100`.
- Kỳ dữ liệu: ngày, tháng, quý, năm.
- Kịch bản dữ liệu: kế hoạch, TKV giao kế hoạch, kế hoạch điều hành, dự kiến và thực hiện.
- Sản phẩm và phân cấp sản phẩm.
- Chỉ tiêu và phân cấp chỉ tiêu.
- Đơn vị tính, thang tiền và hệ số quy đổi.
- Khách hàng, nhà cung cấp, công ty đối tác.
- Kho, luồng nhập/xuất/điều chuyển, hàng đang đi đường.
- Phương pháp khai thác, xuất xứ, loại giao dịch.
- Tàu, nhà máy, đơn vị vận chuyển.
- Chỉ tiêu chất lượng như độ ẩm, độ tro, nhiệt trị, chất bốc, tỷ lệ cục/đá/kẹp sít.

Các chiều này phải được quản trị tập trung và có hiệu lực theo thời gian; không sao chép danh mục vào từng báo cáo.

## 4. Kiến trúc mục tiêu

```text
SCADA / Cân / KCS-LIMS / ERP-Kế toán / Kho / Nhập Excel bổ sung
                              |
                              v
                 Staging + Data Import Batch
                              |
                              v
       Kiểm tra schema -> đối chiếu danh mục -> kiểm tra nghiệp vụ
                    |                         |
                    | lỗi                     | hợp lệ
                    v                         v
             Hàng đợi xử lý lỗi       Duyệt số liệu theo đơn vị
                                              |
                                              v
                    Kho dữ liệu nghiệp vụ chuẩn MES
                                              |
                   +--------------------------+------------------+
                   |                          |                  |
                   v                          v                  v
            Dashboard/KPI             Đối soát kỳ       Report Mapping Engine
                                                                |
                                                                v
                                          Excel/API/SFTP/RPA Adapter Portal
                                                                |
                                                                v
                                         Biên nhận + lỗi + gửi lại + audit
```

### 4.1 Nguyên tắc bắt buộc

1. **Code-first cho nghiệp vụ cốt lõi:** sản xuất, tồn kho, chất lượng và giao vận có aggregate, validation và trạng thái rõ ràng.
2. **Metadata cho báo cáo đầu ra:** cấu hình hàng/cột, mã báo cáo, ánh xạ, công thức trình bày và phiên bản biểu mẫu; không dùng metadata để thay thế toàn bộ domain model.
3. **Một nguồn sự thật:** dữ liệu ngày là nền để tổng hợp tháng/quý/năm, không nhập lại cùng một chỉ tiêu ở nhiều file.
4. **Có lineage:** mọi số liệu báo cáo truy ngược được về nguồn, lô import, giao dịch và người duyệt.
5. **Có version:** kế hoạch và biểu mẫu Portal phải có phiên bản, ngày hiệu lực và trạng thái.
6. **Không cập nhật trực tiếp số dư:** tồn kho và giá trị được hình thành từ sổ giao dịch; snapshot chỉ dùng tối ưu đọc và đối soát.
7. **Khóa kỳ:** số liệu đã duyệt/nộp không được sửa âm thầm. Điều chỉnh phải tạo phiên bản hoặc bút toán điều chỉnh.

## 5. Cấu trúc module đề xuất

### 5.1 MES cốt lõi

| Module | Phạm vi |
|---|---|
| Dashboard điều hành | KPI sản lượng, tiến độ kế hoạch, tồn, chất lượng, cảnh báo |
| Kế hoạch và dự báo | Kế hoạch năm/quý/tháng, TKV giao, điều hành, dự kiến; quản lý phiên bản |
| Điều hành sản xuất | Sản lượng theo ngày/ca/đơn vị/sản phẩm, mét lò, đất bóc, thuê ngoài |
| Kho và luân chuyển | Nhập, xuất, điều chuyển, chế biến, tồn cuối ngày, hàng đang đi đường |
| KCS và chất lượng | Mẫu kiểm tra, kết quả chỉ tiêu, xác nhận chất lượng, sai lệch ngưỡng |
| Tiêu thụ và giao vận | Khách hàng, giao hàng, tàu/nhà máy, lịch dự kiến/thực tế, khối lượng |
| Vật tư | Nhu cầu, mua sắm, nhập/xuất, sử dụng, tồn và phế liệu |
| Báo cáo Portal | Danh mục báo cáo, kỳ, bản nháp, validation, duyệt, gửi và biên nhận |
| Đối soát và cảnh báo | Chênh lệch kế hoạch/thực hiện; sản xuất/kho/tiêu thụ/kế toán |
| Danh mục MES | Sản phẩm, chỉ tiêu, đơn vị, đối tác, kho, phương tiện, quy tắc ánh xạ |

### 5.2 Module ngoài lõi MES

- Quản lý đầu tư và giải ngân.
- Quản lý nhiệm vụ KHCN, hợp đồng, tài sản và kết quả.
- Quản lý nhu cầu CNTT/chuyển đổi số.
- Báo cáo tài chính - kế toán chuyên sâu.

Các module này có thể dùng chung Core Platform, phân quyền, audit, file, workflow và Portal Gateway nhưng không nên ghép bảng nghiệp vụ vào MES cốt lõi.

## 6. Mô hình dữ liệu chuẩn

### 6.1 Danh mục dùng chung

- `reporting_entity`, `organization_unit`.
- `product`, `product_category`, `product_hierarchy`.
- `indicator`, `indicator_hierarchy`.
- `unit_of_measure`, `unit_conversion`, `currency_scale`.
- `customer`, `supplier`, `counterparty`.
- `warehouse`, `stock_location`.
- `mining_method`, `material_origin`, `transaction_type`.
- `transport_asset`, `vessel`, `plant`.
- `quality_parameter`, `quality_specification`.
- `calendar_period`, `data_scenario`, `report_code`.

Mọi danh mục cần có mã ngoài hệ thống, trạng thái, ngày hiệu lực, nguồn đồng bộ và lịch sử thay đổi.

### 6.2 Kế hoạch

- `plan_header`: đơn vị, kỳ, kịch bản, phiên bản, trạng thái, người lập/duyệt.
- `plan_line`: chỉ tiêu, sản phẩm, đơn vị tính, đối tác/đơn vị thực hiện và giá trị.
- `plan_revision`: lý do điều chỉnh và chênh lệch giữa các phiên bản.

Không tạo bảng riêng cho kế hoạch năm, tháng, quý hoặc từng mã `_02`, `_03`, `_04`. Chúng là các giá trị của kỳ, kịch bản và phiên bản.

### 6.3 Sản xuất, kho và chất lượng

- `production_event`, `production_output`.
- `inventory_movement`, `inventory_movement_line`.
- `stock_balance_snapshot`, `stock_in_transit`.
- `quality_sample`, `quality_result`, `quality_acceptance`.
- `processing_batch`, `product_conversion` khi có nghiệp vụ chế biến/pha trộn.

Khóa nghiệp vụ tối thiểu gồm đơn vị, thời điểm, địa điểm, sản phẩm/lô, loại giao dịch và nguồn dữ liệu.

### 6.4 Tiêu thụ và giao vận

- `shipment`, `shipment_line`.
- `sales_delivery`, `sales_delivery_line`.
- `vessel_schedule`, `transport_event`.
- `delivery_quality_reference` để liên kết chất lượng lô giao.

### 6.5 Vật tư

- `material_demand_plan`.
- `material_procurement_receipt`.
- `material_issue` và `material_consumption`.
- `material_balance_snapshot`.
- `scrap_recovery`.

### 6.6 Portal Gateway

- `report_definition`, `report_version`.
- `report_dimension_mapping`, `report_cell_mapping`.
- `submission_batch`, `submission_item`.
- `validation_result`, `submission_error`.
- `portal_receipt`, `submission_attempt`.
- `import_batch`, `import_row`, `import_error`.

Portal Gateway chịu trách nhiệm chuyển dữ liệu chuẩn sang Excel/API của Portal; không chứa logic tính tồn kho hay logic sản xuất.

### 6.7 Nền tảng dùng chung

- RBAC/ABAC theo chức năng, đơn vị và phạm vi dữ liệu.
- Audit log không sửa được.
- Outbox/event cho tác vụ bất đồng bộ.
- File attachment và checksum.
- Workflow, approval, notification.
- Idempotency cho import và gửi Portal.

## 7. Ánh xạ nhóm biểu mẫu vào mô hình mục tiêu

| Nhóm biểu mẫu hiện tại | Đích chuẩn | Hạt dữ liệu chính |
|---|---|---|
| Kế hoạch năm/tháng/quý; TKV giao; điều hành | Plan | Đơn vị + kỳ + phiên bản + chỉ tiêu/sản phẩm |
| Dự kiến thực hiện tháng | Forecast | Đơn vị + tháng + phiên bản dự kiến + sản phẩm |
| Thực hiện ngày/tháng/quý | Production/Inventory/Sales fact | Thời điểm + đơn vị + sản phẩm + loại giao dịch |
| Nhập kho than nguyên khai | Inventory movement + Quality | Ngày + nhà cung cấp + sản phẩm/lô + nguồn + chất lượng |
| Xuất kho than sạch | Sales delivery | Ngày + khách hàng + sản phẩm + tàu + mục đích xuất |
| Tồn kho ngày | Stock snapshot/Reconciliation | Ngày + kho + sản phẩm + tại kho/đang đi đường |
| Theo dõi lịch tàu | Vessel schedule/Shipment | Tàu + nhà máy + khách hàng/công ty + ngày + sản phẩm |
| Nhu cầu và luân chuyển vật tư | Material plan/ledger | Kỳ + vật tư + nguồn/đích + số lượng/giá trị |
| Báo cáo TCKT | Finance reporting adapter | Kỳ + sản phẩm/chỉ tiêu + đối tác + số lượng/giá trị |
| Đầu tư và giải ngân | CAPEX module | Dự án/chỉ tiêu + kỳ + thực hiện/giải ngân |
| Nhiệm vụ KHCN/CNTT | Project/R&D module | Nhiệm vụ + hợp đồng + đơn vị + tiến độ + tài sản/sản phẩm |

## 8. Luồng vận hành báo cáo hàng ngày

1. Scheduler tạo kỳ báo cáo và danh sách biểu mẫu phải nộp trong ngày.
2. Adapter nhận số liệu tự động từ hệ thống nguồn; dữ liệu chưa số hóa được nhập bằng màn hình hoặc file mẫu.
3. Hệ thống kiểm tra định dạng, mã danh mục, kỳ, trùng dữ liệu, dấu số, đơn vị tính và cân đối đầu-cuối.
4. Bản ghi lỗi vào hàng đợi có người chịu trách nhiệm; không ghi đè dữ liệu hợp lệ.
5. Bộ phận phụ trách xác nhận số liệu.
6. Hệ thống đối soát chéo: sản xuất với nhập kho; nhập-xuất với tồn; giao hàng với tiêu thụ; khối lượng với giá trị kế toán.
7. Người có thẩm quyền duyệt và khóa phiên bản.
8. Report Engine sinh payload hoặc file đúng phiên bản Portal.
9. Adapter gửi dữ liệu. Ưu tiên API chính thức; sau đó là SFTP/file import; RPA chỉ dùng khi Portal không có kênh máy-máy.
10. Hệ thống lưu biên nhận, lỗi, thời điểm, payload hash và người thực hiện.
11. Báo cáo lỗi được sửa theo phiên bản và gửi lại, không xóa lịch sử lần gửi trước.

## 9. Quy tắc kiểm tra tối thiểu

- Mã công ty, sản phẩm, chỉ tiêu, khách hàng, nhà cung cấp phải tồn tại và còn hiệu lực.
- Đơn vị tính phải đúng với chỉ tiêu/sản phẩm hoặc có quy tắc chuyển đổi.
- Không chấp nhận cùng một nguồn, kỳ, đơn vị và khóa nghiệp vụ bị ghi nhận hai lần.
- Tồn cuối = tồn đầu + nhập - xuất +/- điều chỉnh, theo từng kho/sản phẩm/lô.
- Khối lượng xuất bán phải đối chiếu được với giao vận và khách hàng.
- Chỉ tiêu chất lượng phải nằm trong miền giá trị hợp lệ và gắn với mẫu/lô.
- Kỳ đã khóa chỉ được điều chỉnh bằng chứng từ hoặc phiên bản điều chỉnh.
- Báo cáo tháng/quý/năm phải chỉ ra nguồn tổng hợp từ ngày hoặc lý do nhập điều chỉnh.
- Tất cả lần import, duyệt, khóa, xuất và gửi Portal phải có audit.

## 10. Cấu trúc menu đề xuất

Giữ nguyên `Trang chủ` và `Quản trị hệ thống` của Core. Thêm menu cấp cao `MES` cùng cấp, không đặt dashboard MES vào Trang chủ Core.

```text
Trang chủ
MES
  Dashboard điều hành
  Kế hoạch và dự báo
  Điều hành sản xuất
  Kho và luân chuyển
  KCS và chất lượng
  Tiêu thụ và giao vận
  Vật tư
  Báo cáo Portal
  Đối soát và cảnh báo
  Danh mục MES
Quản trị hệ thống
```

Đầu tư, KHCN/CNTT và tài chính chuyên sâu chỉ xuất hiện khi module tương ứng được bật và người dùng có quyền.

## 11. Đánh giá khoảng trống so với MES hiện tại

Giao diện MES hiện tại đã có khung điều hướng gần đúng, nhưng để thay thế quy trình Excel/Portal còn thiếu các năng lực chính:

1. Mô hình dữ liệu giao dịch thật cho kế hoạch, sản xuất, kho, KCS, giao vận và vật tư.
2. Danh mục dùng chung có version/effective date và ánh xạ mã Portal.
3. Import batch, preview, validation theo dòng và cơ chế sửa lỗi.
4. Workflow xác nhận, phê duyệt, khóa/mở kỳ.
5. Sổ nhập/xuất/điều chuyển và đối soát tồn.
6. Chi tiết chỉ tiêu KCS theo mẫu/lô.
7. Lịch tàu, giao hàng và liên kết khách hàng/sản phẩm.
8. Report Definition/Mapping Engine và quản lý phiên bản biểu mẫu.
9. Kết nối Portal, biên nhận, gửi lại và theo dõi SLA báo cáo.
10. Data lineage và audit từ ô báo cáo về bản ghi nguồn.
11. Dashboard lấy dữ liệu thật và drill-down đến giao dịch.
12. Kiểm thử cân đối, phân quyền, hiệu năng import và phục hồi lỗi.

Vì vậy bản MES hiện tại là nền giao diện/module, chưa thể được xem là hệ thống thay thế báo cáo Excel hàng ngày.

## 12. Thứ tự triển khai đề xuất

### Lát cắt 0 - Khảo sát Portal và chốt từ điển dữ liệu

- Lập danh mục 98 workbook: chủ sở hữu, tần suất, hạn nộp, nguồn, người duyệt.
- Xác minh API/file import/biên nhận của Portal.
- Chốt mã báo cáo, mã loại dữ liệu `_00` đến `_05`, `T_TCKT_*` và phiên bản biểu mẫu.
- Chọn 3-5 báo cáo ngày có giá trị cao để làm pilot.

### Lát cắt 1 - Master Data và Import Framework

- Danh mục sản phẩm/chỉ tiêu/đơn vị/đối tác/kho.
- Đồng bộ hoặc import danh mục; phát hiện mã không hợp lệ.
- Import batch có preview, validation và audit.

### Lát cắt 2 - Kế hoạch và thực hiện sản xuất

- Plan version/scenario.
- Sản lượng ngày và tổng hợp tháng/quý/năm.
- So sánh kế hoạch-thực hiện và drill-down.

### Lát cắt 3 - Kho và KCS

- Inventory ledger, tồn ngày, hàng đi đường.
- Nhập nguyên khai, xuất than sạch, chất lượng theo lô.
- Đối soát cân bằng kho.

### Lát cắt 4 - Tiêu thụ và giao vận

- Giao hàng, khách hàng, lịch tàu/nhà máy.
- Đối soát khối lượng bán, chế biến và vận chuyển.

### Lát cắt 5 - Portal Gateway

- Report definition/version/mapping.
- Sinh file hoặc payload, validation trước gửi.
- Gửi, nhận biên nhận, retry, lịch sử và cảnh báo trễ hạn.

### Lát cắt 6 - Vật tư và các module mở rộng

- Vật tư/MRO trước.
- Tài chính, đầu tư, KHCN/CNTT triển khai theo nguồn dữ liệu và chủ sở hữu đã chốt.

## 13. Tiêu chí nghiệm thu kiến trúc

- Một danh mục sản phẩm/chỉ tiêu được dùng xuyên suốt, không nhúng bản sao theo workbook.
- Một giao dịch nguồn có mã định danh duy nhất và không bị import trùng.
- Có thể truy ngược một số liệu Portal về dữ liệu nguồn và người duyệt.
- Kế hoạch, dự kiến và thực hiện không dùng chung cột mơ hồ; có scenario/version rõ ràng.
- Dashboard và file Portal lấy từ cùng dữ liệu chuẩn.
- Thay phiên bản biểu mẫu Portal không buộc thay đổi bảng nghiệp vụ.
- Có thể tái tạo chính xác báo cáo đã nộp tại một thời điểm trong quá khứ.
- Lỗi một dòng không làm mất cả batch; retry không tạo dữ liệu trùng.
- Core Platform giữ nguyên shell và quản trị; MES chỉ đăng ký module, quyền và menu của mình.

## 14. Các thông tin cần xác nhận trong vòng BA tiếp theo

Trả lời theo một danh sách duy nhất, không cần trao đổi từng câu:

1. Portal có API, SFTP, chức năng import Excel/CSV hay chỉ cho nhập trực tiếp trên web?
2. Danh sách báo cáo bắt buộc theo ngày/tháng/quý/năm, giờ chốt và đơn vị chịu trách nhiệm của từng báo cáo?
3. Nguồn thực tế của từng nhóm số liệu: SCADA, cân điện tử, KCS/LIMS, ERP/kế toán, phần mềm kho hay nhập tay?
4. Portal có trả mã biên nhận, trạng thái xử lý và chi tiết lỗi không?
5. Quy tắc chính thức của các mã `_00`, `_01`, `_02`, `_03`, `_04`, `_05` và `T_TCKT_*`?
6. Dữ liệu cần lưu đến giao dịch/lô/ca hay chỉ cần số tổng hợp báo cáo?
7. Cơ chế điều chỉnh sau khi khóa kỳ và thẩm quyền mở lại kỳ?
8. Các phép đối soát bắt buộc và ngưỡng sai số được chấp nhận?
9. Ma trận người nhập, người kiểm tra, người duyệt và người gửi theo phòng ban?
10. Phạm vi dữ liệu lịch sử cần chuyển đổi và thời gian lưu trữ?
11. Tần suất Portal thay đổi biểu mẫu và cơ chế thông báo phiên bản mới?
12. Có được phép dùng tự động hóa trình duyệt khi Portal không cung cấp API/import không?

## 15. Kết luận đề xuất

Kiến trúc phù hợp nhất là **MES code-first theo domain + Master Data tập trung + Report Mapping Engine theo metadata + Portal Integration Gateway**.

Không chọn phương án dựng 98 màn hình/bảng tương ứng 98 workbook. Phương án đó sẽ tái tạo nguyên xi nhược điểm của Excel, làm dữ liệu trùng lặp, khó đối soát và tốn chi phí khi Portal đổi mẫu.

Trước khi sửa backend/frontend MES, cần hoàn tất Lát cắt 0 và chọn một chuỗi pilot xuyên suốt, đề xuất:

`Thực hiện sản xuất ngày -> Nhập kho than nguyên khai -> KCS -> Tồn kho ngày -> Xuất kho than sạch -> Sinh và nộp báo cáo Portal`.

Chuỗi này kiểm chứng đồng thời master data, giao dịch, chất lượng, tồn kho, đối soát, phê duyệt và tích hợp Portal; sau khi ổn định mới nhân rộng sang tháng/quý/năm và các module mở rộng.
