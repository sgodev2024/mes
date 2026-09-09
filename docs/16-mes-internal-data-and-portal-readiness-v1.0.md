# MES – Dữ liệu nội bộ chuẩn hóa và mức sẵn sàng Portal V1.0

**Ngày chốt hướng triển khai:** 08/09/2026  
**Trạng thái:** Baseline kỹ thuật Slice 1.2 đã đạt; runtime mục tiêu Flyway V23  
**Phạm vi:** tiếp nối Slice 0 – Data Contract và Slice 1 – Trung tâm báo cáo ngày

## 1. Quyết định đã chốt

1. Tạm dừng toàn bộ chức năng MES tự gửi dữ liệu sang Portal.
2. MES tiếp tục nhận các biểu mẫu Excel hiện hành để có số liệu vận hành nội bộ trong giai đoạn Data Lake và API nguồn chưa sẵn sàng.
3. Excel chỉ là **kênh đầu vào tạm thời**, không phải mô hình dữ liệu đích.
4. Dashboard và báo cáo nội bộ chỉ đọc dữ liệu đã được chuẩn hóa, phê duyệt, khóa và phát hành nội bộ; không đọc trực tiếp ô Excel hoặc JSON staging.
5. Mọi dữ liệu chuẩn hóa phải truy vết được về tenant, file, lô import, template/contract, sheet, dòng và trường nguồn.
6. Tích hợp Portal trong tương lai phải đọc cùng bản phát hành nội bộ qua một adapter riêng. Việc thay Excel bằng API nguồn hoặc Data Lake không làm thay đổi mô hình canonical và dashboard.
7. MES được xây dựng với hai đầu ra chính thức: quản trị nội bộ và Portal Gateway. Việc tạm tắt Portal chỉ là trạng thái triển khai, không thay đổi mục tiêu sản phẩm.

## 2. Mục tiêu của lát cắt

- Có dữ liệu tin cậy để tra cứu, tổng hợp và ra quyết định nội bộ ngay.
- Chuẩn hóa tên trường, kiểu dữ liệu, mã chỉ tiêu, đơn vị tính, sản phẩm và đối tác của sáu biểu mẫu pilot.
- Tách ba lớp rõ ràng: dữ liệu nguồn, dữ liệu chuẩn hóa và dữ liệu tích hợp Portal.
- Không tạo trạng thái `PORTAL_SUBMITTED` giả khi chưa có kết nối Portal thực tế.
- Chuẩn bị event và idempotency boundary để bổ sung API Portal mà không viết lại quy trình nhập, duyệt và khóa dữ liệu.

## 3. Kiến trúc dữ liệu mục tiêu

```text
Excel / nhập trực tiếp / API nguồn / Data Lake
                    │
                    ▼
          Source Adapter + Data Contract
                    │
                    ▼
       Staging bất biến (file, sheet, row, raw payload)
                    │ validate + normalize
                    ▼
     Canonical Dimensions + Daily Metrics + Lineage
                    │ approve + lock (atomic)
                    ▼
        Internal Dataset Release – PUBLISHED
              │                    │
              ▼                    ▼
     Dashboard/Báo cáo MES   Outbox internal-published
                                   │
                                   ▼
                         Portal Adapter (DISABLED)
```

Khi Portal API sẵn sàng, nhánh Portal Adapter chuyển sang `API` nhưng nhánh Dashboard không đổi. Nhân sự chỉ nhập dữ liệu một lần vào MES; adapter chịu trách nhiệm tạo payload/file theo contract đích.

### 3.1 Các lớp dữ liệu

| Lớp | Mục đích | Cho phép Dashboard đọc |
|---|---|---:|
| File nguồn | Giữ nguyên file người dùng nộp và checksum | Không |
| Import staging | Payload nguyên bản, lỗi/cảnh báo và tọa độ nguồn | Không |
| Canonical dimension | Sản phẩm, đơn vị tính, nhà cung cấp/khách hàng đã chuẩn hóa | Có |
| Canonical daily metric | Chỉ tiêu số theo ngày, sản phẩm, đối tác, domain và đơn vị chuẩn | Có |
| Internal dataset release | Phiên bản dữ liệu đã khóa, bất biến và có lineage | Có, chỉ `PUBLISHED` |
| Portal delivery | Payload đích, lần gửi, retry và biên nhận | Chưa bật |

### 3.2 Domain pilot

| Template | Domain canonical | Nhóm dữ liệu |
|---|---|---|
| `MES-DAY-PRODUCTION-PTCB` | `PRODUCTION` | pha trộn/chế biến, mua cho PTCB, bán lại, tồn |
| `MES-DAY-RAW-COAL-RECEIPT` | `RECEIPT` | nhập than nguyên khai và thông số chất lượng |
| `MES-DAY-CLEAN-COAL-RECEIPT` | `RECEIPT` | nhập than sạch |
| `MES-DAY-STOCK` | `INVENTORY` | tồn cuối ngày và hàng đi đường |
| `MES-DAY-CLEAN-COAL-ISSUE` | `ISSUE` | xuất bán/xuất chế biến than sạch |
| `MES-DAY-RAW-COAL-ISSUE` | `ISSUE` | xuất bán/giao tuyển/chế biến than nguyên khai |

## 4. Quy trình phát hành nội bộ

```text
UPLOADED → VALIDATING → PENDING_CONFIRMATION → PENDING_APPROVAL
        → APPROVED → LOCKED + INTERNAL PUBLISHED
```

- `LOCKED` và việc tạo bản phát hành canonical phải nằm trong cùng transaction.
- Nếu chuẩn hóa hoặc phát hành thất bại, thao tác khóa phải rollback; không được có lô `LOCKED` nhưng thiếu dữ liệu nội bộ.
- Một lô chỉ tạo tối đa một bản phát hành; gọi lại không nhân đôi fact hoặc outbox event.
- Lô `INVALID`, `REJECTED`, `SUPERSEDED` hoặc còn chờ duyệt không xuất hiện trong read model nội bộ.
- Mã danh mục chưa được xác nhận vẫn được giữ để không làm mất dữ liệu vận hành, nhưng mang trạng thái `UNVERIFIED` và làm giảm mức sẵn sàng Portal. Không được tự suy đoán ghép mã.

## 5. Mức trưởng thành của dữ liệu

| Mức | Điều kiện |
|---|---|
| `RAW` | File đã tiếp nhận, chưa kiểm tra |
| `VALIDATED` | Đúng template/contract và qua kiểm tra cấp trường |
| `INTERNAL_PUBLISHED` | Đã xác nhận, phê duyệt, khóa và sinh canonical facts |
| `PORTAL_READY` | Danh mục đã resolve, đối soát đạt, mapping Portal được duyệt |
| `DELIVERED` | Portal trả biên nhận thành công |

Trong giai đoạn hiện tại, đích nghiệm thu kỹ thuật là `INTERNAL_PUBLISHED`; `PORTAL_READY` và `DELIVERED` thuộc Slice 5 bắt buộc của sản phẩm nhưng chưa thể triển khai khi thiếu contract Portal.

## 6. Ranh giới Portal

- Cấu hình mặc định: `MES_PORTAL_MODE=DISABLED`.
- Khi `DISABLED`, backend từ chối mọi lệnh tạo lần nộp Portal, không đổi lịch sang `PORTAL_SUBMITTED`, không tạo job và không gọi mạng.
- `MANUAL_TRACKING` chỉ dành cho tình huống cần ghi nhận người dùng đã tự nộp bằng quy trình cũ; không phải tích hợp tự động.
- `API` là trạng thái dự kiến. Trước khi bật phải có OpenAPI/schema, cơ chế xác thực, endpoint test/production, giới hạn tải, timeout/retry, idempotency và quy tắc biên nhận.
- Secret Portal phải lấy từ secret store/environment, không lưu trong source, audit, response hoặc bảng mapping.

## 7. API nội bộ của lát cắt

| Method | Endpoint | Mục đích |
|---|---|---|
| `GET` | `/api/v1/mes/internal-data/summary?from=&to=` | KPI dữ liệu canonical đã phát hành |
| `GET` | `/api/v1/mes/internal-data/metrics?from=&to=&domain=&page=&size=` | Tra cứu chỉ tiêu, dimension và lineage |
| `GET` | `/api/v1/mes/integrations/portal/status` | Trạng thái cấu hình Portal, chỉ đọc |

Tất cả endpoint dữ liệu yêu cầu quyền `MES_REPORT:READ`, áp dụng tenant context và RLS của Core.

## 8. Gate nghiệm thu

- Import fixture hợp lệ → xác nhận → phê duyệt → khóa sinh đúng canonical metrics.
- Tổng hợp nội bộ chỉ đọc bản phát hành `PUBLISHED`.
- Mỗi metric truy vết được tới batch, sheet, dòng, contract version/hash.
- Khóa/lập projection chạy lại không tạo dữ liệu trùng.
- Portal mặc định `DISABLED`; lời gọi endpoint nộp bị từ chối và không thay đổi database.
- Cùng một file có thể là nguồn cho ngày báo cáo khác; chống trùng phải xét tenant + ngày báo cáo + checksum.
- Test với runtime role `core_app` có RLS, migration role `core_admin`.

Bằng chứng tự động ngày 08/09/2026: Flyway chạy sạch đủ V1–V23 trên PostgreSQL 17; 11/11 test mục tiêu MES/Control Plane/UAT workbook và 9/9 frontend rendered test kèm Next.js production build đều đạt.

Bằng chứng runtime local: Docker `mes-local` nâng Flyway đến V23 thành công; frontend, backend và PostgreSQL đều healthy; route `/mes/dashboard`, `/mes/warehouse`, `/mes/master-data` và backend readiness trả HTTP 200.

## 9. Việc chưa thuộc phạm vi

- Không gọi HTTP/SFTP/RPA tới Portal.
- Chưa chốt mapping payload Portal hoặc quy tắc biên nhận.
- Chưa thay nguồn Excel bằng Data Lake/API nghiệp vụ.
- Chưa tự động resolve mã danh mục chưa được Data Steward phê duyệt.
- Chưa triển khai toàn bộ đối soát liên biểu mẫu và công thức KPI sản xuất dành cho Ban Giám đốc.

Các hạng mục Portal trên là pending theo dependency bên ngoài, không phải loại khỏi roadmap MES.

## 10. Bước tiếp theo sau lát cắt này

1. Nhận và UAT ba ngày số liệu thực tế liên tiếp cho mỗi template pilot; UAT cấu trúc file trống đã đạt.
2. Data Steward xác nhận danh mục sản phẩm, đơn vị tính, nhà cung cấp, khách hàng và alias trên màn hình đã triển khai.
3. Nghiệp vụ duyệt quy tắc chuyển đổi PTCB, hàng đi đường và ngưỡng sai số nếu có.
4. Nghiệm thu số liệu Dashboard BGĐ canonical bằng số kiểm soát độc lập.
5. Chỉ sau khi Portal cung cấp hợp đồng tích hợp mới xây `Portal Mapping` và `Portal Adapter`.
