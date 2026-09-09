# MES — Implementation Status v1.0

| Hạng mục | Trạng thái | Bằng chứng |
|---|---|---|
| Full Core source | Hoàn thành | Baseline `core-v1.1.1-project-baseline` |
| Git repository độc lập | Hoàn thành | `origin` MES, `upstream` Java Core |
| Navigation nền tảng | Sẵn sàng | `Trang chủ` → `Nghiệp vụ` → `Quản trị hệ thống` |
| Giao diện xanh chuyển đổi | Sẵn sàng | Next.js và design tokens kế thừa baseline |
| BA/ma trận MES V1 | Đã chốt baseline | `MES Solution Blueprint v1.0`, ngày 07/09/2026 |
| Phân tích Excel/Portal | Hoàn thành inventory kỹ thuật | 98 workbook, 241 sheet; owner/deadline/approver còn chờ nghiệp vụ xác nhận |
| MES Solution Blueprint | Đã phê duyệt | Excel/nhập trực tiếp → validation → duyệt → dữ liệu canonical nội bộ; tích hợp Portal đang pending |
| Navigation module MES | Baseline mới đã duyệt, source chờ triển khai | Cấu trúc bất biến `Trang chủ` → `MES` với 18 menu → `Quản trị hệ thống`; source hiện vẫn là 11 route lát cắt trước, chưa được ghi nhận hoàn thành |
| Frontend MES V1 | Đang triển khai theo lát cắt | 10 màn hình prototype có nhãn dữ liệu mô phỏng và 1 Trung tâm báo cáo ngày dùng API thật |
| Slice 0 — Data Contract & pilot | Baseline kỹ thuật đạt | 98-form catalog; 6 versioned field contract; parser `.xlsx`, checksum, validation, RLS, audit và background job; chờ UAT nghiệp vụ |
| Domain/API nghiệp vụ MES | Đã có Slice 1.2 | API template/import/workflow/canonical, master data, đối soát và trạng thái tích hợp |
| Trung tâm báo cáo ngày | Hoàn thành baseline mã nguồn | Lịch ngày, summary, nhập Excel, preview, xem lỗi, xác nhận, phê duyệt, khóa và phát hành dữ liệu nội bộ; Portal mặc định `DISABLED` |
| Slice 1.1 — Dữ liệu canonical nội bộ | Kiểm thử tự động đạt | Flyway V22; release, sản phẩm, đối tác và chỉ tiêu ngày canonical; lineage, RLS, bất biến và phát hành nguyên tử |
| Master data canonical | Hoàn thành baseline kỹ thuật | Data Steward xác nhận sản phẩm/đối tác, optimistic version, permission, RLS và audit |
| Đối soát nhập–xuất–tồn | Hoàn thành baseline kỹ thuật | Công thức canonical, kiểm tra độ phủ 5 contract và trạng thái fail-safe khi thiếu dữ liệu/master |
| Dashboard BGĐ dữ liệu thật | Hoàn thành baseline kỹ thuật | Đã bỏ fixture tại route dashboard; chỉ đọc canonical summary và reconciliation API |
| Portal Gateway | Chờ contract bên ngoài | Là đầu ra bắt buộc thứ hai của MES; runtime fail-closed `DISABLED`, chưa gọi Portal hoặc sinh biên nhận giả |
| Đối chiếu demo cũ | Hoàn thành phân tích | Giữ đủ 10/10 danh mục cũ, thêm Trung tâm báo cáo ngày; 4/11 route dùng API thật, 7/11 còn prototype |
| Phân tích hồ sơ kỹ thuật BCNCKT | Hoàn thành, chờ duyệt đề xuất | Chuẩn hóa 94 dòng chức năng thành 9 bounded context MES, 6 năng lực Core dùng lại và 4 biên tích hợp; chưa thay đổi baseline/source |
| Deployment riêng | Chưa cấu hình | Chờ domain/server/database/secrets |
| Production readiness | Chưa đạt | Chờ toàn bộ release gates |

Kết luận: Slice 0–1.2 đã có mã nguồn frontend/backend, migration và kiểm thử tự động. UAT cấu trúc 6 workbook thật đã đạt; UAT giá trị chưa thể chốt vì các workbook bàn giao đang trống. Hệ thống chưa phải MES production cho tới khi có ba ngày dữ liệu liên tiếp, Data Steward xác nhận master và nghiệp vụ ký số kiểm soát độc lập. Portal không phải gate hiện tại.

## Xác minh mã nguồn Slice 0–1.2 ngày 08/09/2026

- Java 21 Docker build: đạt.
- Flyway V20–V23 trên PostgreSQL 17 trong môi trường integration test: đạt đủ 23 migration.
- MES backend: 11/11 test MES/hồi quy mục tiêu đạt với `core_admin` chỉ chạy Flyway, `core_app` chạy ứng dụng và RLS fail-closed; bao gồm UAT workbook, parser/workflow, canonical publish, master data, optimistic lock, đối soát, navigation và authorization.
- Next.js 16.3.1 production build: đạt.
- Frontend rendered tests: 9/9 đạt.
- Danh mục kỹ thuật: đủ 98/98 biểu mẫu tại `docs/15-mes-report-catalog-and-dictionaries-v1.0.md`.
- Database integration test dùng database tạm riêng và đã được xóa sau kiểm thử; database MES local không bị chèn fixture test.
- Docker `mes-local` sau triển khai Slice 1.2: frontend/backend/PostgreSQL đều healthy; `/mes/dashboard`, `/mes/warehouse`, `/mes/master-data` và backend readiness trả HTTP 200.
- PostgreSQL runtime local đã được Flyway nâng từ V22 lên V23 thành công; backend chạy profile `mes`, module registry có `mes-production`, frontend chạy tại `http://localhost:3201` và backend tại `http://localhost:8281`.
- Dashboard BGĐ, đối soát kho và master data đã dùng API canonical; các module prototype còn lại tiếp tục có nhãn dữ liệu mô phỏng cho tới lát cắt tương ứng.
- Data Contract và tiêu chí UAT: `docs/14-mes-slice0-data-contract-pilot-v1.0.md`.

## Baseline triển khai ngày 07/09/2026

- Không chờ Data Lake.
- Excel Portal và nhập trực tiếp là nguồn dữ liệu giai đoạn đầu.
- PostgreSQL MES lưu dữ liệu đã kiểm tra và phê duyệt.
- Lát cắt hiện có: Template Registry, import, validation, approval, khóa/phát hành canonical, quản trị master data, đối soát nhập-xuất-tồn và Dashboard BGĐ canonical. Portal được pending và mặc định `DISABLED`.
- Tài liệu chi phối: `docs/13-mes-solution-blueprint-v1.0.md`.

## Xác minh baseline ngày 2026-08-23

- Next.js 16.3.1 production build và 7/7 frontend tests: đạt.
- Java 21, Navigation Registry/API và PostgreSQL 17 Testcontainers: 9/9 backend tests đạt.
- So sánh `backend`, `frontend`, `deploy` và CI với tag baseline: không có thay đổi source chạy.
- Source được đẩy lên nhánh `main`; tag `core-v1.1.1-project-baseline` được bảo toàn trong repository dự án.
