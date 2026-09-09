# MES – UAT Workbook, Master Data, Đối Soát Và Dashboard Canonical V1.0

**Ngày thực hiện:** 08/09/2026  
**Lát cắt:** Slice 1.2  
**Trạng thái:** Đạt gate kỹ thuật; UAT số liệu nghiệp vụ chờ workbook đã điền và Data Steward ký xác nhận

## 1. Kết luận điều hành

- Sáu workbook do nghiệp vụ bàn giao đều được nhận diện đúng Data Contract pilot và không có liên kết workbook ngoài.
- Sáu workbook hiện là **biểu mẫu trống**: không có dòng số liệu nghiệp vụ để xác nhận tổng sản lượng, nhập, xuất hoặc tồn thực tế.
- Không được ghi kết quả này thành “UAT số liệu thực tế đạt”. Gate đã đạt là **UAT cấu trúc workbook thật**.
- Master data nhúng trong workbook được coi là ứng viên, mặc định `UNVERIFIED`; chỉ Data Steward có quyền phê duyệt mới chuyển thành `RESOLVED`.
- Đối soát và Dashboard BGĐ đã chuyển sang facts canonical từ release `PUBLISHED`; không đọc fixture, file Excel hoặc JSON staging.
- Portal tiếp tục `DISABLED` và không nằm trong gate của lát cắt này.

## 2. Phạm vi workbook UAT

| STT | Workbook | Data Contract | Kết quả cấu trúc | Dòng nghiệp vụ |
|---:|---|---|---|---:|
| 1 | `4100_TT_Ngay_PTCB_01.xlsx` | `MES-DAY-PRODUCTION-PTCB` | Đạt | 0 |
| 2 | `4100_TT_NhapKho_Ngay_thanNK.xlsx` | `MES-DAY-RAW-COAL-RECEIPT` | Đạt | 0 |
| 3 | `4100_TT_NhapKho_Ngay_than_sach.xlsx` | `MES-DAY-CLEAN-COAL-RECEIPT` | Đạt | 0 |
| 4 | `4100_TT_Tonkho_Ngay.xlsx` | `MES-DAY-STOCK` | Đạt | 0 |
| 5 | `4100_TT_XuatKho_Ngay_than_sach.xlsx` | `MES-DAY-CLEAN-COAL-ISSUE` | Đạt | 0 |
| 6 | `4100_TT_XuatKho_Ngay_thanNK.xlsx` | `MES-DAY-RAW-COAL-ISSUE` | Đạt | 0 |

Mỗi file được kiểm tra bằng chính Template Registry runtime: tên file, sheet `Template_Import`, header token, field contract, external link và tính bất biến byte trước/sau khi đọc. Parser trả `NO_DATA` đúng với hiện trạng file trống.

## 3. Kiểm kê master data nhúng

| Nguồn | Số bản ghi tham chiếu | Kết quả mã | Vấn đề cần xác nhận |
|---|---:|---|---|
| Danh mục sản phẩm PTCB/tồn kho | 281 | Không trùng mã | Tên `Bùn tuyển 2A-100204` và `Bùn tuyển 2B-100204` xuất hiện với nhiều mã |
| Danh mục sản phẩm nhập/xuất than sạch | 229 | Không trùng mã | Hai nhóm tên nêu trên cần Data Steward xác nhận alias hay sản phẩm riêng |
| Danh mục sản phẩm than nguyên khai | 2 | Không trùng mã | Xác nhận phạm vi mã `TNK`, `TNKNK` |
| Danh mục khách hàng | 256 | Không trùng mã/tên | Xác nhận trạng thái còn hiệu lực và owner |
| Danh mục nhà cung cấp | 1 | Không trùng mã/tên | Hiện chỉ có mã `5004100`; xác nhận có phải phạm vi đầy đủ |

Danh mục trong các sheet phụ không tự động được coi là master chính thức. Quy trình chuẩn:

```text
Workbook/reference → ứng viên canonical UNVERIFIED
→ Data Steward kiểm tra mã, tên, ĐVT và loại đối tác
→ xác nhận RESOLVED (audit + optimistic version)
→ được phép tham gia kết luận đối soát và Portal readiness
```

## 4. Quy tắc đối soát nhập – xuất – tồn

Theo từng `product_id` và ngày báo cáo `D`:

```text
Tồn tính toán(D) = Tồn cuối(D-1)
                 + external_purchase(D)
                 + internal_purchase(D)
                 + mined_receipt(D)
                 + processing_receipt(D)
                 - sale_issue(D)
                 - screening_issue(D)
                 - processing_issue(D)

Chênh lệch(D) = Tồn báo cáo(D) - Tồn tính toán(D)
```

Điều kiện kết luận:

| Trạng thái | Điều kiện |
|---|---|
| `BALANCED` | Đủ 5 contract nhập/tồn/xuất, master đã xác nhận, có tồn D-1 và D, chênh lệch bằng 0 |
| `VARIANCE` | Đủ dữ liệu như trên nhưng chênh lệch khác 0 |
| `INCOMPLETE` | Thiếu contract trong ngày, tồn D-1 hoặc tồn D |
| `MASTER_UNVERIFIED` | Sản phẩm chưa được Data Steward xác nhận |

Không có ngưỡng sai số mặc định. `purchase_for_processing`, `resale_to_tkv` và chuyển đổi sản phẩm PTCB chưa được đưa vào cân bằng tồn cho tới khi nghiệp vụ duyệt hệ số và quan hệ đầu vào–đầu ra.

## 5. API và quyền

| Method | Endpoint | Permission | Mục đích |
|---|---|---|---|
| `GET` | `/api/v1/mes/master-data/products` | `MES_MASTER_DATA:READ` | Tra cứu, lọc trạng thái sản phẩm canonical |
| `PATCH` | `/api/v1/mes/master-data/products/{id}` | `MES_MASTER_DATA:APPROVE` | Hiệu chỉnh/xác nhận sản phẩm, có `expectedVersion` |
| `GET` | `/api/v1/mes/master-data/partners` | `MES_MASTER_DATA:READ` | Tra cứu nhà cung cấp/khách hàng canonical |
| `PATCH` | `/api/v1/mes/master-data/partners/{id}` | `MES_MASTER_DATA:APPROVE` | Hiệu chỉnh/xác nhận đối tác, có `expectedVersion` |
| `GET` | `/api/v1/mes/reconciliation/inventory` | `MES_REPORT:READ` | Đối soát canonical theo ngày |
| `GET` | `/api/v1/mes/internal-data/summary` | `MES_REPORT:READ` | KPI nguồn dữ liệu cho Dashboard BGĐ |

Mọi truy vấn áp dụng tenant context và PostgreSQL RLS. Cập nhật master data ghi audit và từ chối ghi đè khi version đã thay đổi.

## 6. Frontend đã chuyển đổi

- `Dashboard BGĐ`: dữ liệu canonical theo ngày, độ phủ contract, số tham chiếu chưa xác nhận, tổng nhập/xuất/chênh lệch và danh sách cần chú ý.
- `Kho & luân chuyển than`: màn đối soát chi tiết nhập – xuất – tồn.
- `Danh mục hệ thống`: danh sách sản phẩm/đối tác, lọc `UNVERIFIED/RESOLVED`, form Data Steward và trạng thái audit/version.
- Các danh mục kho/cảng, chỉ tiêu và phương tiện hiển thị trạng thái chưa có contract; không sinh dữ liệu mô phỏng.
- Trang chủ, shell và toàn bộ Quản trị hệ thống Core được giữ nguyên.

## 7. Bằng chứng kiểm thử

- Flyway V1–V23 trên PostgreSQL 17: đạt.
- Java 21 compile/package: đạt.
- UAT workbook thật: 6/6 nhận diện đúng, 6/6 `NO_DATA`, 0 external link, byte file không đổi.
- Bộ test mục tiêu MES + Control Plane: 11/11 đạt.
- Next.js 16.3.1 production build: đạt.
- Frontend rendered test: 9/9 đạt.
- Database kiểm thử tách riêng; không chèn fixture UAT vào database local vận hành.

## 8. Dữ liệu cần nghiệp vụ bàn giao để hoàn tất UAT giá trị

Đối với ít nhất ba ngày liên tiếp, cần sáu workbook đã điền cùng kỳ và được người sở hữu dữ liệu ký xác nhận, bao gồm:

1. ngày báo cáo và đơn vị báo cáo;
2. sản phẩm có chung mã giữa tồn, nhập và xuất;
3. trường hợp cân bằng đúng;
4. trường hợp có chênh lệch biết trước;
5. trường hợp số 0 hợp lệ và ô trống có chủ đích;
6. số kiểm soát độc lập để so sánh tổng và từng sản phẩm.

Khi chưa có bộ dữ liệu này, Dashboard chạy đúng bằng trạng thái trống/thiếu dữ liệu nhưng chưa được phép dùng để nghiệm thu con số điều hành thực tế.
