# MES — Navigation Baseline v1.0

## 1. Kiểm soát tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Mã quyết định | MES-NAV-001 |
| Phiên bản | 1.0 |
| Ngày chốt | 09/09/2026 |
| Trạng thái | **ĐÃ CHỐT — BASELINE BẤT BIẾN** |
| Phạm vi | Cấu trúc, nhãn và thứ tự điều hướng MES |
| Người phê duyệt | Chủ dự án |

## 2. Cấu trúc điều hướng chính thức

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

## 3. Quy tắc bất biến

1. Ba khu vực cấp cao luôn theo đúng thứ tự `Trang chủ → MES → Quản trị hệ thống`.
2. `MES` có đúng 18 menu con theo nhãn và thứ tự tại mục 2.
3. Không thêm, xóa, đổi tên, đổi thứ tự hoặc tạo thêm cấp điều hướng trong sidebar MES.
4. Chức năng mới trong tương lai phải được bố trí bằng tab, view, card, bộ lọc hoặc action bên trong một trong 18 menu đã chốt.
5. `Trang chủ` và `Quản trị hệ thống` tiếp tục dùng shell và màn hình Core; module MES không được thay thế hoặc sao chép các màn hình này.
6. Navigation Registry backend là nguồn sự thật. Frontend không hard-code một cây menu MES khác.
7. Phân quyền có thể ẩn menu người dùng không được phép truy cập nhưng không làm thay đổi baseline cấu trúc hoặc thứ tự registry.
8. `Portal/TKV` vẫn giữ vị trí cố định khi tích hợp chưa được cấu hình; màn hình phải hiển thị đúng trạng thái `DISABLED/NOT_CONFIGURED`, không tạo dữ liệu gửi hoặc biên nhận giả.
9. Route cũ khi đổi sang nhãn/route baseline phải có redirect tương thích trong thời gian chuyển đổi; bookmark hoặc direct URL không được trả 404.
10. CI phải có contract test kiểm tra đủ 18 navigation item, nhãn, thứ tự, section và route uniqueness.

## 4. Ranh giới nội dung của từng menu

| Menu | Nội dung thuộc menu |
|---|---|
| Dashboard điều hành | Tổng quan đa lĩnh vực, KPI, xu hướng, ngoại lệ và drill-down |
| Kế hoạch & hiệu quả | Kế hoạch, dự kiến, thực hiện, so sánh và hiệu quả theo đơn vị/kỳ |
| Sản xuất & điều hành | Sản lượng, ca/ngày, tiến độ và tình hình vận hành sản xuất |
| Kho & dòng than | Nhập, xuất, tồn, điều chuyển, dòng than và đối soát khối lượng |
| Chất lượng & nghiệm thu | KCS, mẫu/lô, chỉ tiêu chất lượng, giám định và nghiệm thu |
| Tiêu thụ & giao vận | Tiêu thụ, khách hàng, phương tiện/tàu, cân, giao nhận và tiến độ |
| Vật tư & thiết bị | Nhu cầu, cấp phát, tiêu hao, tồn vật tư, thiết bị, sửa chữa và downtime |
| An toàn lao động | Sự cố, nguy cơ, kiểm tra, khắc phục, huấn luyện và KPI an toàn |
| Tài chính & Kế toán | Góc nhìn quản trị về doanh thu, chi phí, dòng tiền, công nợ và giá trị tồn |
| Nhân sự & lao động | Nhân lực, giờ công, cơ cấu, năng suất, đào tạo và dữ liệu lao động |
| Đầu tư & dự án | Kế hoạch đầu tư, tiến độ, giải ngân và kết quả dự án |
| KHCN & Chuyển đổi số | Nhiệm vụ, hợp đồng, tiến độ, tài sản và kết quả KHCN/CNTT/CĐS |
| Cảnh báo & chỉ đạo | Cảnh báo liên domain, giao xử lý, chỉ đạo, SLA và xác nhận kết quả |
| ESG | Môi trường, xã hội, quản trị, công thức chỉ tiêu và hồ sơ bằng chứng |
| Trung tâm báo cáo | Lịch báo cáo, nhập tay/import, validation, xác nhận, phê duyệt, khóa và canonical release |
| Portal/TKV | Readiness, mapping, payload/file, hàng đợi, retry, biên nhận và đối soát Portal |
| Danh mục & ánh xạ dữ liệu | Master/reference data, mapping mã, đơn vị tính, chỉ tiêu và version |
| Tích hợp & chất lượng dữ liệu | Nguồn dữ liệu, adapter, job, freshness, completeness, lỗi và lineage |

## 5. Trạng thái triển khai

Ngày 09/09/2026, source local đã triển khai Navigation Registry đủ 18 item, đúng nhãn, thứ tự, route và section `MES`. Frontend có đủ 18 màn hình: bốn màn hình dùng API domain/canonical (`Dashboard điều hành`, `Kho & dòng than`, `Trung tâm báo cáo`, `Danh mục & ánh xạ dữ liệu`) và 14 màn hình dùng MES basic runtime lưu PostgreSQL, có workflow, audit, outbox và dữ liệu seed kiểm thử. Không còn nhãn chờ API trên giao diện. Route cũ được chuyển hướng tương thích trong application shell. Contract test backend và source guard frontend bảo vệ baseline; trạng thái runtime Docker được ghi trong sổ thay đổi kỹ thuật sau mỗi lần dựng.
