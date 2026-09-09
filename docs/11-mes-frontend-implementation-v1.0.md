# MES — Frontend & Basic Runtime Implementation v1.2

Ngày cập nhật: 09/09/2026  
Phạm vi: source MES local, không commit và không đẩy Git.

## 1. Baseline và nguyên tắc

- Giữ nguyên Core shell, `Trang chủ` và toàn bộ màn hình `Quản trị hệ thống`.
- Cấu trúc cấp cao: `Trang chủ → MES → Quản trị hệ thống`.
- `MES` có đúng 18 menu con theo quyết định `MES-NAV-001`; không thêm cấp nhóm trung gian.
- Backend Navigation Registry là nguồn sự thật; quyền có thể ẩn item nhưng không thay đổi thứ tự canonical.
- Frontend dùng Next.js 16 App Router/React 19 hiện hữu, không tạo shell hoặc ứng dụng giao diện thứ hai.
- Các màn hình nghiệp vụ không còn đọc fixture để hiển thị. Dữ liệu demo được seed vào PostgreSQL và đi qua API, permission, workflow, audit và outbox như dữ liệu vận hành.
- `Portal/TKV` ở trạng thái chưa cấu hình phải fail-safe: không cho gửi, không sinh biên nhận giả.

## 2. Danh sách route canonical

| STT | View key | Route | Màn hình | Trạng thái dữ liệu |
|---:|---|---|---|---|
| 1 | `mes-dashboard` | `/mes/dashboard` | Dashboard điều hành | API canonical |
| 2 | `mes-planning-performance` | `/mes/planning-performance` | Kế hoạch & hiệu quả | API basic runtime |
| 3 | `mes-production-operations` | `/mes/production-operations` | Sản xuất & điều hành | API basic runtime |
| 4 | `mes-coal-flow` | `/mes/coal-flow` | Kho & dòng than | API canonical |
| 5 | `mes-quality-acceptance` | `/mes/quality-acceptance` | Chất lượng & nghiệm thu | API basic runtime |
| 6 | `mes-sales-logistics` | `/mes/sales-logistics` | Tiêu thụ & giao vận | API basic runtime |
| 7 | `mes-materials-equipment` | `/mes/materials-equipment` | Vật tư & thiết bị | API basic runtime |
| 8 | `mes-occupational-safety` | `/mes/occupational-safety` | An toàn lao động | API basic runtime |
| 9 | `mes-finance-accounting` | `/mes/finance-accounting` | Tài chính & Kế toán | API basic runtime |
| 10 | `mes-workforce-labor` | `/mes/workforce-labor` | Nhân sự & lao động | API basic runtime |
| 11 | `mes-investment-projects` | `/mes/investment-projects` | Đầu tư & dự án | API basic runtime |
| 12 | `mes-science-digital` | `/mes/science-digital` | KHCN & Chuyển đổi số | API basic runtime |
| 13 | `mes-alerts-directives` | `/mes/alerts-directives` | Cảnh báo & chỉ đạo | API basic runtime |
| 14 | `mes-esg` | `/mes/esg` | ESG | API basic runtime |
| 15 | `mes-reporting-center` | `/mes/reporting-center` | Trung tâm báo cáo | API thật |
| 16 | `mes-portal-tkv` | `/mes/portal-tkv` | Portal/TKV | API basic runtime, action gửi bị khóa |
| 17 | `mes-data-mapping` | `/mes/data-mapping` | Danh mục & ánh xạ dữ liệu | API master data |
| 18 | `mes-integration-quality` | `/mes/integration-quality` | Tích hợp & chất lượng dữ liệu | API basic runtime |

## 3. Thành phần giao diện đã hoàn thiện

- Header theo đúng tên phân hệ, mô tả và nguồn dữ liệu.
- Bộ lọc kỳ, đơn vị, nguồn dữ liệu, trạng thái và thao tác đặt lại.
- Tabs nghiệp vụ bên trong từng menu, không làm tăng cấp sidebar.
- Bốn KPI đặc thù, biểu đồ xu hướng, danh sách vấn đề cần chú ý.
- Bảng có STT, tìm kiếm, badge trạng thái, xem chi tiết, mã liên thông và xuất CSV.
- Popup tác vụ có validation và ghi thật vào PostgreSQL.
- Workflow dùng chung: `Nháp → Đang xử lý → Chờ phê duyệt → Đã duyệt → Đã đóng`; hỗ trợ trả lại/mở lại, optimistic locking, audit và outbox.
- Responsive desktop/tablet/mobile; dùng cùng token xanh chuyển đổi của Core.

## 4. Tương thích route cũ

Application shell tự chuyển các route cũ sang route canonical: `daily-reporting → reporting-center`, `planning → planning-performance`, `execution → production-operations`, `warehouse → coal-flow`, `quality → quality-acceptance`, `logistics → sales-logistics`, `alerts → alerts-directives`, `master-data → data-mapping`, `supplemental-input/reports → reporting-center`.

## 5. Hợp đồng backend basic runtime

- `GET /api/v1/mes/modules/{module}/overview`: KPI, biểu đồ 7 ngày, ngoại lệ và danh sách theo tab.
- `POST /api/v1/mes/modules/{module}/records`: tạo bản ghi thật ở trạng thái `DRAFT`.
- `PATCH /api/v1/mes/modules/{module}/records/{id}/workflow`: chuyển trạng thái có kiểm tra phiên bản và quyền duyệt.
- Mã `correlationKey` liên kết bản ghi cùng luồng giữa các phân hệ; Dashboard điều hành đọc thêm ngoại lệ An toàn lao động.
- Migration V25 bảo đảm tên tab seed trùng tuyệt đối với metadata giao diện; mỗi tab basic runtime có ba bản ghi để kiểm tra tải, lọc và workflow.
- Đây là runtime cơ bản có thể vận hành/demo/UAT, không thay cho mô hình domain chuyên sâu, công thức ngành và Data Contract cần triển khai ở các slice tiếp theo.

## 6. Điểm kiểm thử local

- Frontend: `http://localhost:3201`
- Backend readiness: `http://localhost:8281/actuator/health/readiness`
- Route duyệt đầu tiên: `http://localhost:3201/mes/dashboard`
- Tài khoản bootstrap lấy từ cấu hình Docker local; không ghi mật khẩu trong tài liệu.
- Gate frontend: `npm test` phải build production và đạt toàn bộ source guard.
- Gate backend: `MesProductionModuleTest` phải xác nhận đủ 18 item, nhãn, thứ tự, section và route uniqueness.
