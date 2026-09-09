# Core Platform — Sổ thay đổi kỹ thuật

## 1. Mục đích và phạm vi

Đây là tài liệu chuẩn để bộ phận lập trình, kiểm thử và vận hành tra cứu mọi điều chỉnh liên quan đến mã nguồn Core Platform. Tài liệu bao phủ backend Java, frontend Next.js, database migration, security, CI/CD, Docker, Nginx và quy trình release.

Phần quyết định kỹ thuật được cập nhật có chủ đích trong cùng commit với mã. Phần lịch sử Git ở cuối tài liệu được sinh tự động sau mỗi lần push lên `main` bởi workflow `.github/workflows/technical-change-log.yml`; không cần yêu cầu cập nhật riêng.

## 2. Baseline hiện hành

| Thành phần | Baseline |
|---|---|
| Backend | Java 21, Spring Boot 3.5, module registry code-first |
| Frontend | Next.js 16 App Router, React 19, output `standalone` |
| Database | PostgreSQL, Flyway, migration user tách runtime user, tenant RLS |
| Authentication | Internal account, opaque hashed session; MFA được điều khiển bằng feature flag |
| Navigation | Application shell hợp nhất; `Trang chủ` độc lập, section `business`/`system-administration`, menu động và cây tối đa ba cấp |
| Packaging | Docker multi-stage; frontend build bằng Next.js chính thức |
| Release | PR build/test, production release có backup, rehearsal và rollback container |

## 3. Thay đổi đang có hiệu lực

### 3.1 Application shell và Navigation Registry v1.1

- Bỏ Workspace switcher; dùng một cây điều hướng hợp nhất cho dedicated deployment.
- Module đăng ký section/group/page qua `ModuleContributor`; frontend không duy trì danh sách module cố định.
- Cây điều hướng bị giới hạn ở `Section → Group → Page`; registry fail startup nếu group lồng group.
- API `GET /api/v1/navigation/me` trả `sections[]` đã lọc theo module status, authority và permission.
- API `PUT /api/v1/navigation/me/preferences` chỉ lưu yêu thích và mục gần đây; trường Workspace cuối không còn trong contract, cột legacy được reset chuỗi rỗng để tương thích schema.
- Route chuyển từ hash sang `/home`, `/business/...`, `/administration/...`; Next.js có route tương ứng để direct load/refresh.
- Section `system-administration` nằm cuối và chỉ hiện cho `ROLE_PLATFORM_ADMIN`; label người dùng là **Quản trị viên hệ thống**.
- `core.home` được tách vào adapter `home` và render thành page cấp cao, đứng cùng cấp với `Nghiệp vụ` và `Quản trị hệ thống`; section `business` chỉ chứa menu do module nghiệp vụ đóng góp.
- Registry fail startup nếu module chèn item vào `home` hoặc nếu `core.home` bị đặt lại trong `business`; section Nghiệp vụ rỗng vẫn hiển thị empty state thay vì chứa Trang chủ làm fallback.
- Nền application shell và đăng nhập dùng bộ design token `transition-green-*`: canvas xanh nhạt `#e7f2ea`, xanh chuyển đổi `#238558` và xanh rừng `#062f24`; focus ring, topbar và main canvas dùng cùng hệ màu để Core và dự án không lệch nhận diện.
- `NavigationItemDescriptor.visibilityMode=ASSIGNMENT` luôn qua `NavigationVisibilityPolicy` và exact-policy PDP, kể cả System Administrator; wildcard `*/*` không được xem là nhiệm vụ được giao (FE-BA-13).
- Core shell không hard-code `Công việc của tôi`. Chỉ module có view/API/PEP hộp việc thật mới đăng ký item `ASSIGNMENT`; tài khoản quản trị muốn xử lý nghiệp vụ phải có capability assignment chính xác.
- Capability assignment giữ menu ổn định khi hộp việc đang rỗng; view hiển thị empty state, badge không tham gia authorization.
- Frontend chuẩn hóa route không có trong manifest về page được phép, ngăn việc render trực tiếp một view đã bị backend loại khỏi navigation.
- Kiểm thử tách riêng policy hiển thị menu, bao gồm negative test chứng minh System Administrator không bypass `ASSIGNMENT`; frontend có guard chống hard-code menu tác vụ cá nhân.

### 3.2 Chuyển frontend sang Next.js chuẩn

- Loại bỏ vinext, Vite, Cloudflare Worker, Sites hosting metadata và D1/Drizzle starter không sử dụng.
- Lệnh chuẩn: `next dev`, `next build`, `next start`.
- `next.config.ts` bật `output: "standalone"`.
- Dockerfile dùng ba stage: dependency, build và runtime non-root; runtime chỉ chứa `public`, `.next/standalone` và `.next/static`, tắt telemetry và có container healthcheck.
- `NEXT_PUBLIC_CORE_API_URL` được truyền bằng Docker build argument để cố định API URL theo môi trường.
- SSR smoke test khởi động trực tiếp `.next/standalone/server.js`.

### 3.3 Tạm tắt xác thực hai lớp

- Feature flag: `CORE_MFA_ENABLED`; mặc định `true` để fail-safe ở môi trường mới.
- Production hiện đặt `CORE_MFA_ENABLED=false` theo yêu cầu vận hành.
- Khi flag `false`, `POST /api/v1/auth/login` kiểm tra mật khẩu rồi trả `mfaRequired=false` và `session`; không tạo MFA challenge.
- Khi flag `true`, hợp đồng challenge/TOTP cũ vẫn hoạt động.
- Enrollment, secret và recovery codes được giữ nguyên để có thể bật lại mà không migration dữ liệu.
- Mọi lần bỏ qua MFA được audit bằng action `AUTH_MFA_SKIPPED_BY_CONFIGURATION`.
- Bật lại: đổi `CORE_MFA_ENABLED=true` và restart backend; không cần sửa code hoặc database.

### 3.4 Frontend quản trị dedicated deployment

- Baseline nghiệp vụ là một khách hàng/một deployment/một database; không hiển thị tenant switcher hoặc SaaS Control Plane.
- Trang Người dùng nối `GET/POST /api/v1/access/users`, hỗ trợ bật/tắt và đặt lại mật khẩu.
- Trang Cơ cấu tổ chức nối `GET/POST /api/v1/access/organizations`.
- Trang Vai trò & phân quyền nối `GET/POST /api/v1/access/roles` và `GET /api/v1/access/policies`.
- Chỉ số hard-code ở khu vực truy cập và latency đã được thay bằng dữ liệu backend hoặc loại bỏ.
- Trang chủ dùng nội dung theo ngữ cảnh: System Administrator thấy tổng quan vận hành; người dùng khác thấy module nghiệp vụ được cấp quyền.
- Loại bỏ dải thông tin tĩnh `Môi trường / Phiên bản Core / Database / Mô hình vận hành` khỏi cuối Trang chủ theo FE-FR-011; xóa đồng thời CSS responsive không còn sử dụng và có source guard test chống xuất hiện lại.
- Tách `approval-domain` sang package `vn.coreplatform.demo.approval`; module contributor, controller và metadata chỉ active ở profile `demo`/`test`. Production guard cùng migration V17 loại metadata legacy nhưng giữ bảng/dữ liệu để rollback.
- Section `business` là vùng Nghiệp vụ chuẩn chờ module khách hàng. Trong demo/test, `Đề nghị phê duyệt` nằm dưới group `Nghiệp vụ mẫu`; Production loại group/page rỗng khỏi manifest.
- Frontend approval demo được tách thành lazy chunk `app/demo/approval-workspace.tsx`; shell chỉ tải khi backend manifest cho phép view `approvals`.
- Tài liệu BA chuẩn là `core-platform-ba-requirements-v1.1.md`; v1.0 chỉ còn giá trị lịch sử.

### 3.5 Lát cắt frontend MES V1

- Section adapter `business` của dự án được định danh giao diện là `MES`, đứng giữa `Trang chủ` và `Quản trị hệ thống`; không thay thế Core shell hoặc các màn hình quản trị.
- Navigation Registry đóng góp đúng 18 view/route bất biến theo `MES-NAV-001`, không có lớp group trung gian. Contract test kiểm tra đủ nhãn, thứ tự 101–118, section và route không trùng.
- Docker runtime MES dùng profile `mes`, vì vậy module demo `approval-domain` và menu `Nghiệp vụ mẫu/Đề nghị phê duyệt` không được nạp.
- Frontend sử dụng Next.js hiện hành, bộ thành phần MES dùng chung, CSS xanh chuyển đổi responsive, KPI, bộ lọc, tab, biểu đồ, danh sách, popup chi tiết và trạng thái tải/lỗi/empty.
- Bốn màn hình dùng API domain/canonical: `Dashboard điều hành`, `Kho & dòng than`, `Trung tâm báo cáo`, `Danh mục & ánh xạ dữ liệu`. Mười bốn màn hình còn lại dùng API MES basic runtime và PostgreSQL; `app/mes/mes-demo-data.ts` chỉ còn giữ metadata trình bày như tab/cột/nhãn, không cấp dữ liệu hiển thị.
- `Portal/TKV` luôn hiện trong baseline nhưng action kết nối bị vô hiệu hóa an toàn khi API chưa cấu hình; không sinh dữ liệu gửi hoặc biên nhận giả.
- Route cũ của lát cắt 11 menu được ánh xạ sang route canonical mới trong frontend để bookmark/direct URL không rơi về 404.
- Navigation MES không phụ thuộc `ROLE_PLATFORM_ADMIN`; các page yêu cầu permission `MES_REPORT:READ` và được backend lọc theo vai trò/quyền của người dùng.
- Baseline bất biến nằm tại `docs/21-mes-navigation-baseline-v1.0.md`; hướng dẫn triển khai frontend nằm tại `docs/11-mes-frontend-implementation-v1.0.md`.

### 3.6 MES Solution Blueprint v1.0

- Baseline được phê duyệt ngày 07/09/2026 tại `docs/13-mes-solution-blueprint-v1.0.md`.
- MES phải vận hành được ngay mà không chờ Data Lake; Excel Portal và nhập trực tiếp là nguồn đầu vào giai đoạn đầu.
- File gốc được lưu bất biến; dữ liệu qua staging, validation, xác nhận/phê duyệt rồi mới vào PostgreSQL và dashboard.
- Giữ nguyên Core shell và quản trị; `MES` là menu cấp cao cùng cấp với Trang chủ và Quản trị hệ thống.
- Giai đoạn hiện tại gồm Trung tâm báo cáo ngày, phát hành dữ liệu canonical, quản trị master data, đối soát nhập-xuất-tồn và Dashboard BGĐ canonical; tích hợp Portal được pending.
- Data Lake, SCADA, cân, KCS/LIMS và ERP sẽ tham gia bằng adapter dùng chung ingestion contract; không phải dependency runtime ban đầu.
- Frontend hiện không còn trình bày fixture như dữ liệu thật. Basic runtime đủ cho demo/UAT luồng chung, nhưng chỉ được coi là production-ready theo từng nghiệp vụ sau khi hoàn thành domain chuyên sâu, Data Contract, validation, RBAC và UAT tương ứng trong Blueprint.

### 3.7 Slice 0 Data Contract và Slice 1 Trung tâm báo cáo ngày

- Migration V20 tạo schema `mes`, sáu bảng tenant-scoped, PostgreSQL RLS, khóa ngoại và index cho Template Registry, import, issue, lịch báo cáo và nộp Portal.
- Migration V21 bổ sung field contract, contract hash SHA-256, ngày hiệu lực/vòng đời, trigger bất biến và khóa ngoại ghép tenant; seed bốn vai trò MES và năm action permission.
- Bootstrap runtime seed/backfill contract idempotent cho mọi tenant ngay sau Flyway; mỗi tenant chạy trong transaction có `TenantContext` để RLS vẫn fail-closed. Contract V20 rỗng được nâng một lần, contract đã có nội dung không bị sửa.
- Sáu Data Contract pilot được seed theo tenant; parser `.xlsx` dùng Apache POI 5.5.1, kiểm tra chính xác tên file/worksheet/toàn bộ header, cột bắt buộc, kiểu/miền số, quy tắc nhà cung cấp/khách hàng, external link, checksum và giới hạn workbook.
- Job `MES_XLSX_IMPORT` xử lý bất đồng bộ; dữ liệu nguồn và lỗi theo ô/dòng giữ đầy đủ traceability.
- Claim job và phát hiện checksum dùng thao tác nguyên tử/advisory transaction lock; kiểm tra trùng xét `tenant_id + reporting_date + checksum_sha256`; job dài truyền heartbeat.
- Workflow batch của Slice 1 thực thi `PENDING_CONFIRMATION → PENDING_APPROVAL → APPROVED → LOCKED`. Từ Slice 1.1, thao tác khóa đồng thời tạo bản phát hành canonical nội bộ trong cùng transaction.
- Upload trùng trong cùng ngày báo cáo vẫn tạo audit batch `SUPERSEDED` nhưng không enqueue job/không sinh record; cùng file ở ngày báo cáo khác được phép tiếp nhận. Kỳ đã khóa không thể bị lô mới ghi đè.
- Mười sáu endpoint MES được bảo vệ bằng `MES_REPORT`; hai endpoint dữ liệu canonical và endpoint trạng thái Portal dùng `READ`, còn endpoint submit Portal được giữ làm integration boundary nhưng bị từ chối khi mode `DISABLED`.
- Frontend Next.js gắn view live vào Core shell, có lịch ngày, summary, import, preview record, issue detail, tab `Dữ liệu chuẩn hóa`, refresh-token retry và action theo trạng thái.
- Audit log ghi các thay đổi chính; phê duyệt và phát hành nội bộ phát sự kiện bằng transactional outbox.
- Danh mục 98/98 biểu mẫu và từ điển kỹ thuật nằm tại `docs/15-mes-report-catalog-and-dictionaries-v1.0.md`; owner/deadline/approver và KPI BGĐ vẫn chờ phê duyệt nghiệp vụ.
- Kiểm thử ngày 08/09/2026: 13/13 backend test mục tiêu đạt trong cấu hình tách `core_admin`/`core_app` giống runtime; migration sạch đến V22 trong integration test; 8/8 frontend test và Next.js production build đạt.
- Docker `mes-local` đã build lại và nâng runtime PostgreSQL từ V21 lên V22 ngày 08/09/2026; frontend, backend và PostgreSQL đều healthy, route `/mes/daily-reporting` và backend readiness trả HTTP 200.
- Giới hạn production còn lại: antivirus adapter thật, master-data/cross-report validation, scheduler lịch chủ động, revision/mở khóa, UAT bốn mắt, Dashboard BGĐ dữ liệu thật và hợp đồng Portal.

### 3.8 Slice 1.1 — Dữ liệu canonical nội bộ và ranh giới Portal

- Migration V22 tạo `mes.internal_dataset_release`, `mes.canonical_product`, `mes.canonical_partner` và `mes.canonical_daily_metric`; cả bốn bảng dùng tenant RLS `ENABLE + FORCE`, khóa ngoại ghép tenant và index phục vụ truy vấn theo ngày/domain/dimension.
- `internal_dataset_release` và `canonical_daily_metric` đã phát hành là bất biến. Mỗi batch có tối đa một release; unique key metric ngăn nhân đôi khi projection được gọi lại.
- Khóa batch và phát hành release `PUBLISHED` chạy nguyên tử. Nếu normalize hoặc ghi canonical thất bại, toàn bộ thao tác khóa rollback; không tồn tại batch `LOCKED` thiếu dataset nội bộ.
- Sáu Data Contract pilot được ánh xạ sang bốn domain canonical `PRODUCTION`, `RECEIPT`, `INVENTORY`, `ISSUE`. Mã sản phẩm/đối tác chưa được Data Steward xác nhận được giữ ở trạng thái `UNVERIFIED`, không tự suy đoán ghép mã.
- Mỗi metric giữ lineage tới tenant, batch, source record, sheet, dòng, template code, contract version/hash và thời điểm phát hành. Event `mes.report.internal-published.v1` được ghi qua transactional outbox.
- API đọc mới: `GET /api/v1/mes/internal-data/summary`, `GET /api/v1/mes/internal-data/metrics`; frontend bổ sung tab `Dữ liệu chuẩn hóa` và liên kết truy vết về batch nguồn. Đây là read model nội bộ, chưa phải Dashboard BGĐ hoàn chỉnh.
- Cấu hình `MES_PORTAL_MODE` nhận `DISABLED`, `MANUAL_TRACKING`, `API`; mặc định Docker/application là `DISABLED`. Mode `API` vẫn trả trạng thái `NOT_CONFIGURED` và không dispatch khi chưa có contract.
- Khi Portal bị tắt, `POST /api/v1/mes/import-batches/{id}/portal-submissions` trả `409 MES_PORTAL_INTEGRATION_DISABLED`, không đổi calendar, không tạo portal submission/job/outbox và không gọi mạng. `GET /api/v1/mes/integrations/portal/status` cung cấp capability chỉ đọc.
- Gate ngày 08/09/2026: backend 13/13 test mục tiêu đạt, frontend 8/8 rendered test và production build đạt, Flyway V22 đạt trên PostgreSQL 17 integration test; runtime Docker local đã nâng lên V22 và ba service đều healthy.

### 3.9 UAT workbook, master data, đối soát và Dashboard canonical

- Migration V23 bổ sung optimistic versioning và trạng thái xác nhận cho canonical product/partner, khóa ngoại tenant tới người xác nhận, permission `MES_MASTER_DATA:READ|APPROVE` và index phục vụ Data Steward.
- API master data cho phép lọc, xem và xác nhận sản phẩm/đối tác theo tenant; cập nhật có optimistic locking, validation, permission và audit log.
- API `GET /api/v1/mes/reconciliation/inventory` tính `tồn cuối D-1 + nhập D - xuất D = tồn cuối D`, trả rõ `BALANCED`, `VARIANCE`, `INCOMPLETE` hoặc `MASTER_UNVERIFIED`; dữ liệu thiếu không được mặc định coi là cân.
- Dashboard BGĐ và màn Kho/luân chuyển đọc trực tiếp canonical API, có bộ lọc ngày, loading/error/empty state và không sử dụng fixture demo.
- Sáu workbook nguồn được chạy UAT bất biến với registry/parser thật: nhận diện đúng sáu contract nhưng đều không có dòng nghiệp vụ. Vì vậy UAT cấu trúc đạt; UAT giá trị và đối soát vẫn cần ít nhất ba ngày file đã điền dữ liệu.
- Gate mục tiêu ngày 08/09/2026: Flyway sạch đến V23 trên PostgreSQL 17, 11/11 test MES/hồi quy mục tiêu và Next.js production build đạt; chi tiết tại `docs/17-mes-uat-master-data-reconciliation-dashboard-v1.0.md`.

### 3.10 Mô hình MES hai đầu ra: nội bộ và Portal

- Chốt MES có hai trách nhiệm chính thức: dữ liệu quản trị nội bộ dùng ngay khi Data Lake chưa sẵn sàng và Portal Gateway tự động nộp báo cáo khi có API.
- Một pipeline `source → staging → validation → approval → canonical PUBLISHED` cấp dữ liệu cho cả Dashboard và Portal; cấm tạo pipeline chuẩn hóa hoặc bộ số liệu Portal riêng.
- Hiện trạng chuyển tiếp: nhân sự vẫn import Excel thủ công trên Portal; MES không tự ghi `DELIVERED` hoặc biên nhận khi chưa có bằng chứng từ Portal.
- Đích vận hành: nhập một lần tại MES, Portal Adapter sinh payload/file theo mapping version, gửi bất đồng bộ qua outbox/job, retry idempotent, lưu biên nhận và đối soát tổng/chi tiết.
- Runtime vẫn `MES_PORTAL_MODE=DISABLED` cho tới khi có contract API thật. Đây là dependency triển khai của Slice 5, không phải loại bỏ Portal khỏi phạm vi sản phẩm.
- Baseline chi tiết tại `docs/18-mes-dual-output-internal-portal-operating-model-v1.0.md`.

### 3.11 Đối chiếu demo MES cũ và module Slice 1.2

- Xác nhận Navigation Registry hiện tại giữ đủ 10/10 danh mục nghiệp vụ từng xuất hiện trong demo và bổ sung `Trung tâm báo cáo ngày`.
- Demo cũ là HTML tĩnh, không có API. Bản hiện tại có 4/11 route dùng API thật: Dashboard canonical, Trung tâm báo cáo ngày, đối soát kho và master data một phần; 7 route còn lại vẫn là prototype có nhãn mô phỏng.
- Giữ nguyên Core shell/Quản trị hệ thống thật; không phục hồi các màn quản trị tĩnh hoặc số KPI minh họa từ demo.
- Tách ranh giới `Trung tâm báo cáo ngày` cho workbook Portal, `Cổng nhập liệu bổ sung` cho ngoại lệ/manual entry và `Báo cáo & đối soát` cho tổng hợp liên domain.
- Ma trận chi tiết và backlog thay prototype bằng domain/API thật nằm tại `docs/19-mes-demo-current-module-gap-analysis-v1.0.md`.

### 3.12 Phân tích hồ sơ kỹ thuật và chuẩn hóa module MES

- Đã lọc nội dung phần mềm MES từ hồ sơ `BCNCKT-Mạo Khê 17122025`; không đưa phần đầu tư, phần cứng, camera, phòng IOC hoặc thiết kế nội bộ Data Lake vào domain MES.
- Danh sách 94 dòng chức năng trong hồ sơ được phân tích là capability/use case, không phải 94 module. Đề xuất chuẩn hóa thành 9 bounded context MES, dùng lại 6 nhóm năng lực Core và 4 biên tích hợp.
- Chuẩn hóa 45 dòng chức năng kho về một mô hình chứng từ + ledger, với transaction type và validation riêng; không tạo service/bảng lặp cho từng thao tác CRUD.
- Đối chiếu công nghệ xác nhận Java/PostgreSQL phù hợp; đề xuất giữ Next.js và modular monolith hiện tại thay vì viết lại Angular hoặc tách microservices sớm.
- Mười hai đề xuất `MES-TA-01` đến `MES-TA-12` đang chờ phê duyệt. Chưa thay đổi Blueprint, BA, backlog hoặc source chạy.
- Phân tích và ma trận chi tiết tại `docs/20-mes-technical-feasibility-module-standardization-analysis-v1.0.md`.

### 3.13 Baseline điều hướng MES toàn doanh nghiệp

- Chủ dự án chốt cấu trúc cấp cao `Trang chủ → MES → Quản trị hệ thống`; `MES` có đúng 18 menu con theo thứ tự tại `docs/21-mes-navigation-baseline-v1.0.md`.
- Bổ sung chính thức các miền An toàn lao động, Tài chính & Kế toán, Nhân sự & lao động, Đầu tư & dự án, KHCN & Chuyển đổi số và ESG vào phạm vi dữ liệu điều hành.
- Baseline menu không được thêm/xóa/đổi tên/đổi thứ tự hoặc tạo cấp sidebar mới. Chức năng phát sinh phải bố trí bên trong một trong 18 menu bằng tab/view/action.
- Permission có thể lọc khả năng nhìn thấy theo người dùng nhưng không thay đổi cấu trúc canonical của Navigation Registry.
- Source local đã triển khai đủ 18 route canonical. Bốn màn hình dùng API hiện hữu; 14 màn hình là prototype chờ backend, có dữ liệu minh họa tách biệt và endpoint dự kiến.
- Frontend duy trì redirect cho 10 route cũ để direct URL/bookmark không 404. Portal/TKV hiển thị trạng thái chưa cấu hình và khóa action gửi.
- Gate ngày 09/09/2026: Next.js production build và 9/9 frontend test đạt; `MesProductionModuleTest` đạt trên Maven/Java 21; backend compile/package đạt; Docker local gồm PostgreSQL, backend và frontend đều healthy. Frontend trả HTTP 200, backend readiness `UP`.

### 3.14 Khôi phục Dashboard điều hành theo mẫu demo Mạo Khê

- Đổi nhãn điều hướng và tiêu đề từ `Dashboard Ban lãnh đạo` thành `Dashboard điều hành` trên backend Navigation Registry, frontend và tài liệu baseline.
- Tái dựng bố cục tham chiếu từ demo đã duyệt: bộ lọc kỳ/phạm vi, 7 lát cắt điều hành, 6 KPI, nhịp sản xuất 14 ngày, quyết định cần ban hành, hiệu quả và rủi ro vận hành.
- Không đưa số liệu demo cố định vào màn hình production. KPI, biểu đồ và ngoại lệ lấy từ API canonical hiện hữu; vùng An toàn đọc dữ liệu từ basic runtime của module An toàn lao động.
- KPI và hành động drill-down mở đúng module MES tương ứng trong shell Core hiện tại.

### 3.15 MES basic runtime cho 14 phân hệ

- Migration V24 tạo `mes.operational_record`, tenant RLS, index theo module/tab/trạng thái và index `correlation_key` để liên thông dữ liệu giữa các phân hệ.
- Migration V25 căn chỉnh seed của Kế hoạch, Sản xuất, Chất lượng và Tiêu thụ theo chính xác tên tab frontend; chỉ xóa/thay dòng có cờ `seeded=true`, không tác động dữ liệu người dùng.
- Mười bốn phân hệ chưa có domain chuyên sâu dùng chung runtime cơ bản nhưng lưu dữ liệu thật trong PostgreSQL; tất cả tab đã được seed ba bản ghi để kiểm thử giao diện và workflow.
- API chung hỗ trợ đọc overview/KPI/xu hướng/ngoại lệ, tạo bản ghi và chuyển workflow. Trạng thái chuẩn là `DRAFT → IN_PROGRESS → PENDING_APPROVAL → APPROVED → CLOSED`, kèm nhánh `REJECTED` và `REOPEN`.
- Quyền `MES_OPERATION:READ|CREATE|UPDATE|APPROVE`, optimistic versioning, audit log và transactional outbox được áp dụng ở backend; System Administrator tiếp tục có toàn quyền theo Core policy.
- Frontend bỏ nhãn `Prototype · Chờ API`, đọc API thật theo tab, có filter, bảng, biểu đồ cột, chi tiết, mã luồng liên thông, popup tạo, workflow và trạng thái tải/lỗi/empty.
- Dashboard điều hành lấy KPI và điểm nóng An toàn lao động từ cùng API; các số liệu canonical sản xuất/kho vẫn giữ nguồn dữ liệu đã phát hành ở V22/V23.
- `Portal/TKV` có dữ liệu theo dõi nội bộ nhưng nút gửi tiếp tục bị khóa cho đến khi có API contract Portal thật; hệ thống không tạo trạng thái gửi hoặc biên nhận giả.
- Basic runtime là lát cắt hoạt động để demo/UAT và chuẩn hóa luồng chung. Mô hình chuyên ngành, công thức, validation chéo và adapter nguồn sẽ thay từng phần theo Blueprint mà không đổi baseline menu.
- Gate ngày 09/09/2026: frontend production build và 9/9 source guard đạt; backend package Java 21 đạt; `MesProductionModuleTest` 2/2 và `MesOperationalModuleApiTest` 1/1 đạt trên PostgreSQL 17 sạch. Docker local đã nâng Flyway tới V25 với 14 module, 68 tab, 204 bản ghi seed và ba service đều healthy.

## 4. Quy tắc cập nhật tài liệu

1. Thay đổi kiến trúc, security, API contract, migration hoặc vận hành phải cập nhật phần quyết định ở trên trong cùng pull request.
2. Commit subject phải mô tả kết quả kỹ thuật; lịch sử tự động sử dụng chính subject và danh sách file.
3. Không sửa nội dung giữa hai marker `AUTO-GENERATED`.
4. Có thể tái tạo cục bộ bằng `node scripts/update-technical-change-log.mjs` hoặc `npm run docs:changes` trong thư mục `frontend`.
5. Workflow trên `main` tự commit tài liệu sinh lại bằng tài khoản `github-actions[bot]` khi lịch sử thay đổi.

## 5. Lịch sử thay đổi tự động

<!-- AUTO-GENERATED:START -->
> Sinh tự động từ Git. Mốc mã gần nhất: `27e8ce106eada043dabf5e787856c36b5fda8eb0` (2026-09-10). Không sửa trực tiếp phần này.

| Ngày | Commit | Nội dung | Tác giả | Số file |
|---|---|---|---|---:|
| 2026-09-10 | [`27e8ce1`](https://github.com/sgodev2024/java-core/commit/27e8ce106eada043dabf5e787856c36b5fda8eb0) | chore: add MES server deployment configuration | sgodev2024 | 2 |
| 2026-09-10 | [`4a47186`](https://github.com/sgodev2024/java-core/commit/4a471868404ee223334ced565f25c8abc5bdf168) | feat: implement MES operational platform baseline | sgodev2024 | 70 |
| 2026-08-24 | [`d57ab26`](https://github.com/sgodev2024/java-core/commit/d57ab26843c1b0037a204c797abfc9bc74c3d1e3) | fix: accept empty successful API responses | SGO Viet Nam | 1 |
| 2026-08-24 | [`0d8d3d7`](https://github.com/sgodev2024/java-core/commit/0d8d3d7289da6317a9ad648f32b735856befbbce) | fix: handle mandatory password change without infinite loading | SGO Viet Nam | 4 |
| 2026-08-23 | [`56d5f9b`](https://github.com/sgodev2024/java-core/commit/56d5f9b58ec36c93d3b288daa9d673b99c9ddc69) | Record Core baseline verification | sgodev2024 | 1 |
| 2026-08-23 | [`88927c4`](https://github.com/sgodev2024/java-core/commit/88927c4160132f5bd198ff35e00f58158a1efee2) | Initialize MES project from Core v1.1.1 baseline | sgodev2024 | 9 |
| 2026-08-23 | [`f778414`](https://github.com/sgodev2024/java-core/commit/f7784146757944069b7dc866d8762ed19151d508) | Apply green transformation interface palette | SGO Development | 6 |
| 2026-08-23 | [`c43b0b6`](https://github.com/sgodev2024/java-core/commit/c43b0b64a37571023fcbaf5fbe0cb5661d1a724a) | Separate home from business navigation | SGO Development | 13 |
| 2026-08-18 | [`e3a8ec3`](https://github.com/sgodev2024/java-core/commit/e3a8ec3a341e0445738a4614dc6b81da112b9deb) | Require browser origin deployment testing | SGO Development | 1 |
| 2026-08-18 | [`591c6e3`](https://github.com/sgodev2024/java-core/commit/591c6e350beb23af4da76fa440629d1f4f4e449f) | Document Core-to-project delivery standard | SGO Development | 2 |
| 2026-08-18 | [`7300b4d`](https://github.com/sgodev2024/java-core/commit/7300b4d70b4db30a1f1eb9b82befcd01b5c686d5) | Keep business project repository independent | SGO Development | 1 |
| 2026-08-18 | [`682d300`](https://github.com/sgodev2024/java-core/commit/682d30079ccb5f52133bc374223556955f4004c2) | Add project-ready domain module contracts | SGO Development | 17 |
| 2026-08-17 | [`5c51d9b`](https://github.com/sgodev2024/java-core/commit/5c51d9b2d93eae37a0a5c0f1d49e17339065165e) | Normalize legacy core module statuses | SGO Development | 4 |
| 2026-08-17 | [`776e86b`](https://github.com/sgodev2024/java-core/commit/776e86b888e25968029c741d02541baee99366b0) | Align migration concurrency test with V18 | SGO Development | 1 |
| 2026-08-17 | [`5c1c38a`](https://github.com/sgodev2024/java-core/commit/5c1c38a1e777a9a874fb0e0adc589077d864469e) | Fix bootstrap initializer test environment | SGO Development | 1 |
| 2026-08-17 | [`4990614`](https://github.com/sgodev2024/java-core/commit/499061418cabf6557bc3ecc524b90cf8bc31c6cb) | Align ESG frontend with production APIs | SGO Development | 31 |
| 2026-08-17 | [`0f5ec51`](https://github.com/sgodev2024/java-core/commit/0f5ec51cac77974ff5f37741cc894faa07edcea9) | Isolate approval sample behind demo and test profiles | SGO Development | 22 |
| 2026-08-17 | [`ae38ab7`](https://github.com/sgodev2024/java-core/commit/ae38ab7cca381cb1bf9630da7ed2ec106915af9d) | Remove deployment summary strip from home page | SGO Development | 5 |
| 2026-08-17 | [`16e7eab`](https://github.com/sgodev2024/java-core/commit/16e7eab53bc9368d9c9a8e90f0069cc7d29939a5) | Enforce assignment-scoped personal task navigation | SGO Development | 11 |
| 2026-08-17 | [`06ddb15`](https://github.com/sgodev2024/java-core/commit/06ddb1542bd1b7bfb1cfc7eafc0adb7d7bd15eb9) | Preserve navigation preference schema compatibility | SGO Development | 3 |
| 2026-08-17 | [`018c886`](https://github.com/sgodev2024/java-core/commit/018c886578056beaf4a7c44b050aa21326f3b356) | Fix navigation module status query compilation | SGO Development | 1 |
| 2026-08-17 | [`5f298fa`](https://github.com/sgodev2024/java-core/commit/5f298fa92011c27a9c847217e94428cea92e5e89) | Unify navigation shell and implement FE-BA v1.1 | SGO Development | 21 |
| 2026-08-17 | [`e63edc0`](https://github.com/sgodev2024/java-core/commit/e63edc07782810aac19415d0a0900aa1140ace4a) | Migrate frontend to Next.js and add MFA feature flag | SGO Development | 35 |
| 2026-08-17 | [`89e36e6`](https://github.com/sgodev2024/java-core/commit/89e36e68ab5628d63bc7fafe888b48c5d120937d) | Add workspace-based dynamic navigation registry | SGO Development | 15 |
| 2026-08-16 | [`3741888`](https://github.com/sgodev2024/java-core/commit/3741888b8abde726d06438e75f5423150b737d1a) | Add sample domain, full-text search, SSRF-guarded webhooks, CSV idempotency and SBOM (E10+E11+E13) | sgodev2024 | 14 |
| 2026-08-16 | [`3e9eb18`](https://github.com/sgodev2024/java-core/commit/3e9eb18be31f3dac682450c6810f7dd152f2780f) | Add file lifecycle (staging/scan/finalize/reconcile) and dynamic resource hardening (E8+E9) | sgodev2024 | 9 |
| 2026-08-16 | [`9ede356`](https://github.com/sgodev2024/java-core/commit/9ede356a5922396f3891afe95cd2ea49b4365f88) | Add job queue leases, heartbeat, retry classification and scheduler leader election (E7) | sgodev2024 | 14 |
| 2026-08-16 | [`77f4071`](https://github.com/sgodev2024/java-core/commit/77f4071d45f9294983160fd89ead41ad46de8131) | Add transactional outbox, relay leases, inbox idempotency and replay (E6) | sgodev2024 | 17 |
| 2026-08-16 | [`c081a6b`](https://github.com/sgodev2024/java-core/commit/c081a6b1b52658b9921b704ee08300b26e70fa56) | Allow 127.0.0.1 origins in CORS for local UI access | sgodev2024 | 1 |
| 2026-08-16 | [`99d2a5b`](https://github.com/sgodev2024/java-core/commit/99d2a5bd1ea8c9ff16a250c40c086b99ac1b4cdd) | Add audit integrity: transactional audit, masking, hash chain, checkpoint/retention (E5) | sgodev2024 | 14 |
| 2026-08-16 | [`a25a057`](https://github.com/sgodev2024/java-core/commit/a25a0579d5d939ed7f2231bd6cdda288d574bd21) | Add resource registry SPI, PDP/PEP hardening and classification gate (E4) | sgodev2024 | 24 |
| 2026-08-16 | [`87e03ac`](https://github.com/sgodev2024/java-core/commit/87e03ac0df3f4b5cc369c859192220280d038f55) | Add local identity hardening: Argon2id, lockout, TOTP MFA, refresh rotation, service accounts (E3) | sgodev2024 | 12 |
| 2026-08-16 | [`f5420c2`](https://github.com/sgodev2024/java-core/commit/f5420c2c860982a261c5db7eb7891881e917bbfe) | Add database roles, RLS tenant isolation and tenant-aware pooling (E2) | sgodev2024 | 9 |
| 2026-08-16 | [`7092171`](https://github.com/sgodev2024/java-core/commit/70921719a10cbda8ad5240e24c4e7bcc90633920) | Add engineering foundation and kernel module runtime (E0+E1) | sgodev2024 | 32 |
| 2026-08-15 | [`70827f9`](https://github.com/sgodev2024/java-core/commit/70827f97865159178207433e18099d68c122a116) | Add tenant-authorized file storage and streaming | SGO Development | 4 |
| 2026-08-15 | [`de08774`](https://github.com/sgodev2024/java-core/commit/de087746d5ea63a6a10a3c07125739e2f1a03c31) | Remove external CSV runtime dependency | SGO Development | 2 |
| 2026-08-15 | [`3e5d210`](https://github.com/sgodev2024/java-core/commit/3e5d21041bc5f603eda3da5a370e980a6570dd43) | Add Dynamic Resource console and CSV operations | SGO Development | 3 |
| 2026-08-15 | [`2bd78ad`](https://github.com/sgodev2024/java-core/commit/2bd78ad47608a73a7f3f632fc7543c8edda6768a) | Enforce tenant-scoped policy and access management | SGO Development | 5 |
| 2026-08-15 | [`f7e490b`](https://github.com/sgodev2024/java-core/commit/f7e490b2b33441bce6beeec9b9f5d864222ead4d) | Add tenant permission and dynamic resource foundation | SGO Development | 3 |
| 2026-08-15 | [`996a826`](https://github.com/sgodev2024/java-core/commit/996a826daf979c1c635053f2f93f541f5292b1cd) | Implement control plane operations backed by PostgreSQL | SGO Development | 3 |
| 2026-08-15 | [`5cca3c8`](https://github.com/sgodev2024/java-core/commit/5cca3c8b1c0b0c8a5dd77b4f288e0e7bf3bfc94f) | Use compact standalone frontend runtime | SGO Development | 3 |
| 2026-08-15 | [`82f5ac3`](https://github.com/sgodev2024/java-core/commit/82f5ac3e0f201bfa2b48c1b20efaf74f004fcc0b) | Self-host frontend on corejava domain | SGO Development | 3 |
| 2026-08-15 | [`73ff468`](https://github.com/sgodev2024/java-core/commit/73ff46893ee6d0c6c72e37c37f56899bfe8a5b78) | Route production frontend through dedicated API domain | SGO Development | 3 |
| 2026-08-15 | [`b5d5d26`](https://github.com/sgodev2024/java-core/commit/b5d5d26ac2c58e61e5a4c0b84f395f3ae9b02f0b) | Connect frontend to production API | SGO Development | 1 |
| 2026-08-15 | [`6b5f782`](https://github.com/sgodev2024/java-core/commit/6b5f782f6dc519242f09fe54700bc6d34eb22b61) | Fix PostgreSQL summary alias | SGO Development | 1 |
| 2026-08-15 | [`6267349`](https://github.com/sgodev2024/java-core/commit/62673493fed5bc796e77a98021db925c9b5fb250) | Fix session expiry persistence | SGO Development | 1 |
| 2026-08-15 | [`11e6532`](https://github.com/sgodev2024/java-core/commit/11e6532fab7bdaa480d578e9f1a8ff7298b77f3b) | Secure bootstrap admin credentials | SGO Development | 4 |
| 2026-08-15 | [`9b25425`](https://github.com/sgodev2024/java-core/commit/9b254255a044698b96f3b5a39f12ac304caa3e52) | Fix security filter compilation | SGO Development | 1 |
| 2026-08-15 | [`295b0e6`](https://github.com/sgodev2024/java-core/commit/295b0e6d6b200c9cd17cfe23d11683f8d04d19e8) | Initialize Java Core Platform | SGO Development | 57 |

## Chi tiết file theo commit

### 2026-09-10 — chore: add MES server deployment configuration

- Commit: [`27e8ce106eada043dabf5e787856c36b5fda8eb0`](https://github.com/sgodev2024/java-core/commit/27e8ce106eada043dabf5e787856c36b5fda8eb0)
- Tác giả: sgodev2024
- Phạm vi file:

- `A` `deploy/ubuntu20/nginx-mes.sgodata.com.conf`
- `A` `docker-compose.server.override.yml`

### 2026-09-10 — feat: implement MES operational platform baseline

- Commit: [`4a471868404ee223334ced565f25c8abc5bdf168`](https://github.com/sgodev2024/java-core/commit/4a471868404ee223334ced565f25c8abc5bdf168)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `.github/PULL_REQUEST_TEMPLATE.md`
- `M` `backend/Dockerfile`
- `M` `backend/pom.xml`
- `M` `backend/src/main/java/vn/coreplatform/jobs/JobService.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/KernelModule.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/MesProductionModule.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/operations/MesOperationalModuleController.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/operations/MesOperationalModuleService.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesCanonicalDataService.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesDailyReportingController.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesDailyReportingService.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesImportJobHandler.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesInternalDataController.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesInventoryReconciliationController.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesInventoryReconciliationService.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesMasterDataController.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesMasterDataService.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesPortalConfiguration.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesTemplateBootstrap.java`
- `A` `backend/src/main/java/vn/coreplatform/mes/reporting/MesWorkbookParser.java`
- `M` `backend/src/main/resources/application.yml`
- `A` `backend/src/main/resources/db/migration/V20__mes_daily_reporting_slice.sql`
- `A` `backend/src/main/resources/db/migration/V21__mes_data_contract_hardening.sql`
- `A` `backend/src/main/resources/db/migration/V22__mes_internal_canonical_data.sql`
- `A` `backend/src/main/resources/db/migration/V23__mes_master_data_and_inventory_reconciliation.sql`
- `A` `backend/src/main/resources/db/migration/V24__mes_operational_modules_basic_runtime.sql`
- `A` `backend/src/main/resources/db/migration/V25__align_mes_operational_seed_tabs.sql`
- `M` `backend/src/test/java/vn/coreplatform/AbstractApiTest.java`
- `M` `backend/src/test/java/vn/coreplatform/controlplane/ControlPlaneTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleRegistrationTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/mes/MesDailyReportingApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/mes/MesMasterDataReconciliationApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/mes/MesOperationalModuleApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/mes/MesProductionModuleTest.java`
- `A` `backend/src/test/java/vn/coreplatform/mes/MesWorkbookParserTest.java`
- `A` `backend/src/test/java/vn/coreplatform/mes/reporting/MesRealWorkbookContractUatTest.java`
- `A` `docker-compose.local.override.yml`
- `M` `docker-compose.yml`
- `M` `docs/01-business-analysis-v1.0.md`
- `M` `docs/04-data-and-integration-contracts-v1.0.md`
- `M` `docs/05-implementation-status-v1.0.md`
- `A` `docs/09-mes-v1-training-task-backlog.md`
- `A` `docs/10-mes-git-branch-commit-pr-checklist.md`
- `A` `docs/11-mes-frontend-implementation-v1.0.md`
- `A` `docs/12-mes-bi-portal-assessment-v1.0.md`
- `A` `docs/13-mes-solution-blueprint-v1.0.md`
- `A` `docs/14-mes-slice0-data-contract-pilot-v1.0.md`
- `A` `docs/15-mes-report-catalog-and-dictionaries-v1.0.md`
- `A` `docs/16-mes-internal-data-and-portal-readiness-v1.0.md`
- `A` `docs/17-mes-uat-master-data-reconciliation-dashboard-v1.0.md`
- `A` `docs/18-mes-dual-output-internal-portal-operating-model-v1.0.md`
- `A` `docs/19-mes-demo-current-module-gap-analysis-v1.0.md`
- `A` `docs/20-mes-technical-feasibility-module-standardization-analysis-v1.0.md`
- `A` `docs/21-mes-navigation-baseline-v1.0.md`
- `M` `docs/decisions.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/Dockerfile`
- `M` `frontend/app/components/app-icon.tsx`
- `M` `frontend/app/globals.css`
- `A` `frontend/app/mes/[...path]/page.tsx`
- `A` `frontend/app/mes/daily-reporting-center.tsx`
- `A` `frontend/app/mes/mes-demo-data.ts`
- `A` `frontend/app/mes/mes-executive-dashboard.tsx`
- `A` `frontend/app/mes/mes-inventory-reconciliation.tsx`
- `A` `frontend/app/mes/mes-live-api.ts`
- `A` `frontend/app/mes/mes-master-data.tsx`
- `A` `frontend/app/mes/mes-workspace.tsx`
- `M` `frontend/app/page.tsx`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-24 — fix: accept empty successful API responses

- Commit: [`d57ab26843c1b0037a204c797abfc9bc74c3d1e3`](https://github.com/sgodev2024/java-core/commit/d57ab26843c1b0037a204c797abfc9bc74c3d1e3)
- Tác giả: SGO Viet Nam
- Phạm vi file:

- `M` `frontend/app/page.tsx`

### 2026-08-24 — fix: handle mandatory password change without infinite loading

- Commit: [`0d8d3d7289da6317a9ad648f32b735856befbbce`](https://github.com/sgodev2024/java-core/commit/0d8d3d7289da6317a9ad648f32b735856befbbce)
- Tác giả: SGO Viet Nam
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/identity/BootstrapAdminInitializer.java`
- `M` `backend/src/test/java/vn/coreplatform/identity/BootstrapAdminInitializerTest.java`
- `M` `frontend/app/globals.css`
- `M` `frontend/app/page.tsx`

### 2026-08-23 — Record Core baseline verification

- Commit: [`56d5f9b58ec36c93d3b288daa9d673b99c9ddc69`](https://github.com/sgodev2024/java-core/commit/56d5f9b58ec36c93d3b288daa9d673b99c9ddc69)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `docs/05-implementation-status-v1.0.md`

### 2026-08-23 — Initialize MES project from Core v1.1.1 baseline

- Commit: [`88927c4160132f5bd198ff35e00f58158a1efee2`](https://github.com/sgodev2024/java-core/commit/88927c4160132f5bd198ff35e00f58158a1efee2)
- Tác giả: sgodev2024
- Phạm vi file:

- `A` `CORE_BASELINE`
- `A` `PROJECT.md`
- `A` `docs/00-core-to-project-implementation-standard-v1.0.md`
- `A` `docs/01-business-analysis-v1.0.md`
- `A` `docs/02-data-discovery-plan-v1.0.md`
- `A` `docs/03-business-rules-v1.0.md`
- `A` `docs/04-data-and-integration-contracts-v1.0.md`
- `A` `docs/05-implementation-status-v1.0.md`
- `A` `docs/06-deployment-runbook-preparation-v1.0.md`

### 2026-08-23 — Apply green transformation interface palette

- Commit: [`f7784146757944069b7dc866d8762ed19151d508`](https://github.com/sgodev2024/java-core/commit/f7784146757944069b7dc866d8762ed19151d508)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `.gitignore`
- `M` `core-platform-ba-requirements-v1.1.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/README.md`
- `M` `frontend/app/globals.css`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-23 — Separate home from business navigation

- Commit: [`c43b0b64a37571023fcbaf5fbe0cb5661d1a724a`](https://github.com/sgodev2024/java-core/commit/c43b0b64a37571023fcbaf5fbe0cb5661d1a724a)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/kernel/KernelModule.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/NavigationRegistry.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/NavigationWorkspaceDescriptor.java`
- `M` `backend/src/main/java/vn/coreplatform/navigation/NavigationController.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationApiTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationRegistryTest.java`
- `M` `core-platform-ba-requirements-v1.1.md`
- `M` `docs/navigation-registry.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/README.md`
- `M` `frontend/app/globals.css`
- `M` `frontend/app/page.tsx`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-18 — Require browser origin deployment testing

- Commit: [`e3a8ec3a341e0445738a4614dc6b81da112b9deb`](https://github.com/sgodev2024/java-core/commit/e3a8ec3a341e0445738a4614dc6b81da112b9deb)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `docs/core-to-project-implementation-standard-v1.0.md`

### 2026-08-18 — Document Core-to-project delivery standard

- Commit: [`591c6e350beb23af4da76fa440629d1f4f4e449f`](https://github.com/sgodev2024/java-core/commit/591c6e350beb23af4da76fa440629d1f4f4e449f)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `README.md`
- `A` `docs/core-to-project-implementation-standard-v1.0.md`

### 2026-08-18 — Keep business project repository independent

- Commit: [`7300b4d70b4db30a1f1eb9b82befcd01b5c686d5`](https://github.com/sgodev2024/java-core/commit/7300b4d70b4db30a1f1eb9b82befcd01b5c686d5)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `.gitignore`

### 2026-08-18 — Add project-ready domain module contracts

- Commit: [`682d30079ccb5f52133bc374223556955f4004c2`](https://github.com/sgodev2024/java-core/commit/682d30079ccb5f52133bc374223556955f4004c2)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `README.md`
- `A` `backend/src/main/java/vn/coreplatform/kernel/CoreCompatibility.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/DomainResourceAdapter.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/DomainResourceAdapterRegistry.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/ModuleDescriptor.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/ModuleRegistry.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/DomainResourceAdapterRegistryTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleRegistryTest.java`
- `M` `docs/backend-frontend-gap-analysis-v1.0.md`
- `M` `docs/decisions.md`
- `A` `scripts/new-domain-module.ps1`
- `A` `templates/domain-module/README.md`
- `A` `templates/domain-module/backend/DomainAdapter.java.template`
- `A` `templates/domain-module/backend/DomainModule.java.template`
- `A` `templates/domain-module/backend/V__MIGRATION_VERSION____MODULE_KEY_SQL____baseline.sql.template`
- `A` `templates/domain-module/frontend/page.tsx.template`
- `A` `templates/domain-module/module-manifest.yaml.template`

### 2026-08-17 — Normalize legacy core module statuses

- Commit: [`5c51d9b2d93eae37a0a5c0f1d49e17339065165e`](https://github.com/sgodev2024/java-core/commit/5c51d9b2d93eae37a0a5c0f1d49e17339065165e)
- Tác giả: SGO Development
- Phạm vi file:

- `A` `backend/src/main/resources/db/migration/V19__normalize_core_module_runtime_status.sql`
- `M` `backend/src/test/java/vn/coreplatform/controlplane/LegacySeedDataCleanupTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`
- `M` `docs/backend-frontend-gap-analysis-v1.0.md`

### 2026-08-17 — Align migration concurrency test with V18

- Commit: [`776e86b888e25968029c741d02541baee99366b0`](https://github.com/sgodev2024/java-core/commit/776e86b888e25968029c741d02541baee99366b0)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`

### 2026-08-17 — Fix bootstrap initializer test environment

- Commit: [`5c1c38a1e777a9a874fb0e0adc589077d864469e`](https://github.com/sgodev2024/java-core/commit/5c1c38a1e777a9a874fb0e0adc589077d864469e)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/test/java/vn/coreplatform/identity/BootstrapAdminInitializerTest.java`

### 2026-08-17 — Align ESG frontend with production APIs

- Commit: [`499061418cabf6557bc3ecc524b90cf8bc31c6cb`](https://github.com/sgodev2024/java-core/commit/499061418cabf6557bc3ecc524b90cf8bc31c6cb)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/README.md`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneModule.java`
- `M` `backend/src/main/java/vn/coreplatform/demo/approval/ApprovalDomainModule.java`
- `M` `backend/src/main/java/vn/coreplatform/filemanagement/FileController.java`
- `A` `backend/src/main/java/vn/coreplatform/filemanagement/FileResourceMetadata.java`
- `A` `backend/src/main/java/vn/coreplatform/identity/BootstrapAdminInitializer.java`
- `D` `backend/src/main/java/vn/coreplatform/identity/DemoAccountInitializer.java`
- `A` `backend/src/main/java/vn/coreplatform/identity/IdentityResourceMetadata.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/KernelModule.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/ModuleRegistry.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/ResourceRegistry.java`
- `M` `backend/src/main/java/vn/coreplatform/permission/AccessManagementController.java`
- `A` `backend/src/main/resources/db/migration/V18__remove_legacy_demo_seed_data.sql`
- `A` `backend/src/test/java/vn/coreplatform/controlplane/LegacySeedDataCleanupTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/BootstrapAdminInitializerTest.java`
- `M` `core-platform-ba-requirements-v1.1.md`
- `A` `docs/backend-frontend-gap-analysis-v1.0.md`
- `M` `docs/decisions.md`
- `M` `frontend/README.md`
- `A` `frontend/app/components/app-icon.tsx`
- `M` `frontend/app/demo/approval-workspace.tsx`
- `M` `frontend/app/globals.css`
- `M` `frontend/app/page.tsx`
- `M` `frontend/package-lock.json`
- `M` `frontend/package.json`
- `D` `frontend/postcss.config.mjs`
- `D` `frontend/public/file.svg`
- `D` `frontend/public/globe.svg`
- `D` `frontend/public/window.svg`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-17 — Isolate approval sample behind demo and test profiles

- Commit: [`0f5ec51cac77974ff5f37741cc894faa07edcea9`](https://github.com/sgodev2024/java-core/commit/0f5ec51cac77974ff5f37741cc894faa07edcea9)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `README.md`
- `M` `backend/README.md`
- `A` `backend/src/main/java/vn/coreplatform/demo/approval/ApprovalDomainModule.java`
- `A` `backend/src/main/java/vn/coreplatform/demo/approval/ApprovalRequestController.java`
- `A` `backend/src/main/java/vn/coreplatform/demo/approval/DemoApprovalMetadata.java`
- `A` `backend/src/main/java/vn/coreplatform/demo/approval/DemoApprovalProductionGuard.java`
- `D` `backend/src/main/java/vn/coreplatform/domain/ApprovalDomainModule.java`
- `D` `backend/src/main/java/vn/coreplatform/domain/ApprovalRequestController.java`
- `A` `backend/src/main/resources/db/migration/V17__isolate_demo_approval_module.sql`
- `M` `backend/src/test/java/vn/coreplatform/AbstractApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/demo/approval/ApprovalDomainTest.java`
- `A` `backend/src/test/java/vn/coreplatform/demo/approval/DemoApprovalProfileTest.java`
- `D` `backend/src/test/java/vn/coreplatform/domain/ApprovalDomainTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleBoundaryTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationApiTest.java`
- `M` `core-platform-ba-requirements-v1.1.md`
- `M` `docs/navigation-registry.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/README.md`
- `A` `frontend/app/demo/approval-workspace.tsx`
- `M` `frontend/app/page.tsx`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-17 — Remove deployment summary strip from home page

- Commit: [`ae38ab7cca381cb1bf9630da7ed2ec106915af9d`](https://github.com/sgodev2024/java-core/commit/ae38ab7cca381cb1bf9630da7ed2ec106915af9d)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `core-platform-ba-requirements-v1.1.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/app/globals.css`
- `M` `frontend/app/page.tsx`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-17 — Enforce assignment-scoped personal task navigation

- Commit: [`16e7eab53bc9368d9c9a8e90f0069cc7d29939a5`](https://github.com/sgodev2024/java-core/commit/16e7eab53bc9368d9c9a8e90f0069cc7d29939a5)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/kernel/NavigationRegistry.java`
- `M` `backend/src/main/java/vn/coreplatform/navigation/NavigationController.java`
- `A` `backend/src/main/java/vn/coreplatform/navigation/NavigationVisibilityPolicy.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationRegistryTest.java`
- `A` `backend/src/test/java/vn/coreplatform/navigation/NavigationVisibilityPolicyTest.java`
- `M` `core-platform-ba-requirements-v1.1.md`
- `M` `docs/navigation-registry.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/README.md`
- `M` `frontend/app/page.tsx`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-17 — Preserve navigation preference schema compatibility

- Commit: [`06ddb1542bd1b7bfb1cfc7eafc0adb7d7bd15eb9`](https://github.com/sgodev2024/java-core/commit/06ddb1542bd1b7bfb1cfc7eafc0adb7d7bd15eb9)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/navigation/NavigationController.java`
- `M` `docs/navigation-registry.md`
- `M` `docs/technical-change-register.md`

### 2026-08-17 — Fix navigation module status query compilation

- Commit: [`018c886578056beaf4a7c44b050aa21326f3b356`](https://github.com/sgodev2024/java-core/commit/018c886578056beaf4a7c44b050aa21326f3b356)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/navigation/NavigationController.java`

### 2026-08-17 — Unify navigation shell and implement FE-BA v1.1

- Commit: [`5f298fa92011c27a9c847217e94428cea92e5e89`](https://github.com/sgodev2024/java-core/commit/5f298fa92011c27a9c847217e94428cea92e5e89)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `README.md`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneModule.java`
- `M` `backend/src/main/java/vn/coreplatform/domain/ApprovalDomainModule.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/KernelModule.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/NavigationItemDescriptor.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/NavigationRegistry.java`
- `M` `backend/src/main/java/vn/coreplatform/navigation/NavigationController.java`
- `M` `backend/src/main/java/vn/coreplatform/permission/PermissionService.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationApiTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/NavigationRegistryTest.java`
- `M` `backend/src/test/java/vn/coreplatform/permission/PermissionTest.java`
- `A` `core-platform-ba-requirements-v1.1.md`
- `M` `docs/navigation-registry.md`
- `M` `docs/technical-change-register.md`
- `M` `frontend/README.md`
- `A` `frontend/app/administration/[...path]/page.tsx`
- `A` `frontend/app/business/[...path]/page.tsx`
- `M` `frontend/app/globals.css`
- `A` `frontend/app/home/page.tsx`
- `M` `frontend/app/page.tsx`
- `M` `technical-delivery-pack-v1.0/README.md`

### 2026-08-17 — Migrate frontend to Next.js and add MFA feature flag

- Commit: [`e63edc07782810aac19415d0a0900aa1140ace4a`](https://github.com/sgodev2024/java-core/commit/e63edc07782810aac19415d0a0900aa1140ace4a)
- Tác giả: SGO Development
- Phạm vi file:

- `A` `.github/workflows/technical-change-log.yml`
- `M` `README.md`
- `M` `backend/.env.example`
- `M` `backend/README.md`
- `M` `backend/src/main/java/vn/coreplatform/identity/AuthController.java`
- `M` `backend/src/main/resources/application.yml`
- `M` `backend/src/test/java/vn/coreplatform/AbstractApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/MfaDisabledLoginTest.java`
- `M` `deploy/ubuntu20/core-platform.env.example`
- `M` `docker-compose.yml`
- `A` `docs/technical-change-register.md`
- `M` `frontend/.dockerignore`
- `M` `frontend/.gitignore`
- `D` `frontend/.openai/hosting.json`
- `M` `frontend/Dockerfile`
- `M` `frontend/README.md`
- `D` `frontend/app/chatgpt-auth.ts`
- `M` `frontend/app/globals.css`
- `M` `frontend/app/layout.tsx`
- `M` `frontend/app/page.tsx`
- `D` `frontend/build/sites-vite-plugin.ts`
- `D` `frontend/db/index.ts`
- `D` `frontend/db/schema.ts`
- `D` `frontend/drizzle.config.ts`
- `D` `frontend/drizzle/meta/_journal.json`
- `M` `frontend/eslint.config.mjs`
- `D` `frontend/examples/d1/app/api/notes/route.ts`
- `D` `frontend/examples/d1/db/schema.ts`
- `M` `frontend/next.config.ts`
- `M` `frontend/package-lock.json`
- `M` `frontend/package.json`
- `M` `frontend/tests/rendered-html.test.mjs`
- `D` `frontend/vite.config.ts`
- `D` `frontend/worker/index.ts`
- `A` `scripts/update-technical-change-log.mjs`

### 2026-08-17 — Add workspace-based dynamic navigation registry

- Commit: [`89e36e68ab5628d63bc7fafe888b48c5d120937d`](https://github.com/sgodev2024/java-core/commit/89e36e68ab5628d63bc7fafe888b48c5d120937d)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `README.md`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneModule.java`
- `M` `backend/src/main/java/vn/coreplatform/domain/ApprovalDomainModule.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/KernelModule.java`
- `M` `backend/src/main/java/vn/coreplatform/kernel/ModuleContributor.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/NavigationItemDescriptor.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/NavigationRegistry.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/NavigationWorkspaceDescriptor.java`
- `A` `backend/src/main/java/vn/coreplatform/navigation/NavigationController.java`
- `A` `backend/src/main/resources/db/migration/V16__navigation_registry_preferences.sql`
- `A` `backend/src/test/java/vn/coreplatform/kernel/NavigationApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/NavigationRegistryTest.java`
- `A` `docs/navigation-registry.md`
- `M` `frontend/app/globals.css`
- `M` `frontend/app/page.tsx`

### 2026-08-16 — Add sample domain, full-text search, SSRF-guarded webhooks, CSV idempotency and SBOM (E10+E11+E13)

- Commit: [`3741888b8abde726d06438e75f5423150b737d1a`](https://github.com/sgodev2024/java-core/commit/3741888b8abde726d06438e75f5423150b737d1a)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/pom.xml`
- `A` `backend/src/main/java/vn/coreplatform/domain/ApprovalDomainModule.java`
- `A` `backend/src/main/java/vn/coreplatform/domain/ApprovalRequestController.java`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `A` `backend/src/main/java/vn/coreplatform/webhook/WebhookController.java`
- `A` `backend/src/main/java/vn/coreplatform/webhook/WebhookModule.java`
- `A` `backend/src/main/java/vn/coreplatform/webhook/WebhookService.java`
- `A` `backend/src/main/resources/db/migration/V14__sample_domain_e10.sql`
- `A` `backend/src/main/resources/db/migration/V15__search_webhook_e11.sql`
- `M` `backend/src/test/java/vn/coreplatform/controlplane/ControlPlaneTest.java`
- `A` `backend/src/test/java/vn/coreplatform/domain/ApprovalDomainTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleBoundaryTest.java`
- `A` `backend/src/test/java/vn/coreplatform/webhook/WebhookAndSearchTest.java`

### 2026-08-16 — Add file lifecycle (staging/scan/finalize/reconcile) and dynamic resource hardening (E8+E9)

- Commit: [`3e9eb18be31f3dac682450c6810f7dd152f2780f`](https://github.com/sgodev2024/java-core/commit/3e9eb18be31f3dac682450c6810f7dd152f2780f)
- Tác giả: sgodev2024
- Phạm vi file:

- `A` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceAdminController.java`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `M` `backend/src/main/java/vn/coreplatform/filemanagement/FileController.java`
- `A` `backend/src/main/java/vn/coreplatform/filemanagement/FileStorageService.java`
- `A` `backend/src/main/resources/db/migration/V12__file_lifecycle_e8.sql`
- `A` `backend/src/main/resources/db/migration/V13__dynamic_advanced_e9.sql`
- `A` `backend/src/test/java/vn/coreplatform/dynamicresource/DynamicResourceAdvancedTest.java`
- `A` `backend/src/test/java/vn/coreplatform/filemanagement/FileLifecycleTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`

### 2026-08-16 — Add job queue leases, heartbeat, retry classification and scheduler leader election (E7)

- Commit: [`9ede356a5922396f3891afe95cd2ea49b4365f88`](https://github.com/sgodev2024/java-core/commit/9ede356a5922396f3891afe95cd2ea49b4365f88)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/README.md`
- `A` `backend/src/main/java/vn/coreplatform/audit/AuditCheckpointHandler.java`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `A` `backend/src/main/java/vn/coreplatform/jobs/JobHandler.java`
- `A` `backend/src/main/java/vn/coreplatform/jobs/JobScheduler.java`
- `A` `backend/src/main/java/vn/coreplatform/jobs/JobService.java`
- `A` `backend/src/main/java/vn/coreplatform/jobs/JobWorker.java`
- `A` `backend/src/main/java/vn/coreplatform/jobs/JobsModule.java`
- `M` `backend/src/main/resources/application.yml`
- `A` `backend/src/main/resources/db/migration/V11__jobs_scheduler_e7.sql`
- `A` `backend/src/test/java/vn/coreplatform/jobs/JobQueueSchedulerTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleBoundaryTest.java`
- `M` `docker-compose.yml`

### 2026-08-16 — Add transactional outbox, relay leases, inbox idempotency and replay (E6)

- Commit: [`77f4071d45f9294983160fd89ead41ad46de8131`](https://github.com/sgodev2024/java-core/commit/77f4071d45f9294983160fd89ead41ad46de8131)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/README.md`
- `A` `backend/src/main/java/vn/coreplatform/controlplane/ActivityProjector.java`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `A` `backend/src/main/java/vn/coreplatform/eventing/EventingModule.java`
- `A` `backend/src/main/java/vn/coreplatform/eventing/IntegrationEvent.java`
- `A` `backend/src/main/java/vn/coreplatform/eventing/IntegrationEventHandler.java`
- `A` `backend/src/main/java/vn/coreplatform/eventing/OutboxRelay.java`
- `A` `backend/src/main/java/vn/coreplatform/eventing/OutboxService.java`
- `M` `backend/src/main/resources/application.yml`
- `A` `backend/src/main/resources/db/migration/V10__eventing_outbox_inbox_e6.sql`
- `A` `backend/src/test/java/vn/coreplatform/eventing/EventContractTest.java`
- `A` `backend/src/test/java/vn/coreplatform/eventing/OutboxRelayTest.java`
- `A` `backend/src/test/java/vn/coreplatform/eventing/OutboxTransactionTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleBoundaryTest.java`
- `M` `docker-compose.yml`

### 2026-08-16 — Allow 127.0.0.1 origins in CORS for local UI access

- Commit: [`c081a6b1b52658b9921b704ee08300b26e70fa56`](https://github.com/sgodev2024/java-core/commit/c081a6b1b52658b9921b704ee08300b26e70fa56)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`

### 2026-08-16 — Add audit integrity: transactional audit, masking, hash chain, checkpoint/retention (E5)

- Commit: [`99d2a5bd1ea8c9ff16a250c40c086b99ac1b4cdd`](https://github.com/sgodev2024/java-core/commit/99d2a5bd1ea8c9ff16a250c40c086b99ac1b4cdd)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/README.md`
- `A` `backend/src/main/java/vn/coreplatform/audit/AuditModule.java`
- `A` `backend/src/main/java/vn/coreplatform/audit/AuditService.java`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `M` `backend/src/main/java/vn/coreplatform/filemanagement/FileController.java`
- `M` `backend/src/main/java/vn/coreplatform/identity/AuthController.java`
- `M` `backend/src/main/java/vn/coreplatform/permission/AccessManagementController.java`
- `M` `backend/src/main/java/vn/coreplatform/permission/PermissionService.java`
- `A` `backend/src/main/resources/db/migration/V9__audit_integrity_e5.sql`
- `A` `backend/src/test/java/vn/coreplatform/audit/AuditIntegrityTest.java`
- `M` `backend/src/test/java/vn/coreplatform/controlplane/ControlPlaneTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`
- `M` `backend/src/test/java/vn/coreplatform/kernel/ModuleBoundaryTest.java`

### 2026-08-16 — Add resource registry SPI, PDP/PEP hardening and classification gate (E4)

- Commit: [`a25a0579d5d939ed7f2231bd6cdda288d574bd21`](https://github.com/sgodev2024/java-core/commit/a25a0579d5d939ed7f2231bd6cdda288d574bd21)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `README.md`
- `M` `backend/README.md`
- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/ResourceDescriptor.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/ResourceRegistry.java`
- `A` `backend/src/main/java/vn/coreplatform/permission/PermissionEnforcementInterceptor.java`
- `M` `backend/src/main/java/vn/coreplatform/permission/PermissionService.java`
- `A` `backend/src/main/java/vn/coreplatform/permission/RequirePermission.java`
- `A` `backend/src/main/resources/db/migration/V8__resource_registry_e4.sql`
- `A` `backend/src/test/java/vn/coreplatform/controlplane/ControlPlaneTest.java`
- `A` `backend/src/test/java/vn/coreplatform/dynamicresource/ClassificationGateTest.java`
- `A` `backend/src/test/java/vn/coreplatform/dynamicresource/DynamicResourceTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/ResourceRegistryTest.java`
- `A` `backend/src/test/java/vn/coreplatform/permission/PepFailClosedTest.java`
- `A` `backend/src/test/java/vn/coreplatform/permission/PermissionPredicateTest.java`
- `A` `backend/src/test/java/vn/coreplatform/permission/PermissionTest.java`
- `A` `backend/src/test/java/vn/coreplatform/permission/TenantIsolationTest.java`
- `A` `docs/adr/adr-template.md`
- `A` `docs/decisions.md`
- `A` `frontend/.env.example`
- `M` `frontend/.gitignore`
- `M` `frontend/app/page.tsx`
- `M` `frontend/tests/rendered-html.test.mjs`

### 2026-08-16 — Add local identity hardening: Argon2id, lockout, TOTP MFA, refresh rotation, service accounts (E3)

- Commit: [`87e03ac0df3f4b5cc369c859192220280d038f55`](https://github.com/sgodev2024/java-core/commit/87e03ac0df3f4b5cc369c859192220280d038f55)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/identity/AuthController.java`
- `M` `backend/src/main/java/vn/coreplatform/identity/DemoAccountInitializer.java`
- `A` `backend/src/main/java/vn/coreplatform/identity/Totp.java`
- `M` `backend/src/main/java/vn/coreplatform/permission/AccessManagementController.java`
- `M` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`
- `A` `backend/src/main/resources/db/migration/V7__identity_tenant_e3.sql`
- `A` `backend/src/test/java/vn/coreplatform/identity/MfaEnrollFlowTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/MfaEnrollmentTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/PasswordPolicyTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/RefreshRotationTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/ServiceAccountTest.java`
- `A` `backend/src/test/java/vn/coreplatform/permission/TenantOrganizationTest.java`

### 2026-08-16 — Add database roles, RLS tenant isolation and tenant-aware pooling (E2)

- Commit: [`f5420c2c860982a261c5db7eb7891881e917bbfe`](https://github.com/sgodev2024/java-core/commit/f5420c2c860982a261c5db7eb7891881e917bbfe)
- Tác giả: sgodev2024
- Phạm vi file:

- `M` `backend/Dockerfile`
- `A` `backend/src/main/java/vn/coreplatform/kernel/TenantAwareDataSource.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/TenantContext.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/TenantContextFilter.java`
- `A` `backend/src/main/resources/db/migration/V6__kernel_roles_rls.sql`
- `A` `backend/src/test/java/vn/coreplatform/kernel/RowLevelSecurityTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/TenantDataSourceLeakTest.java`
- `A` `deploy/postgres/01-core-roles.sql`
- `M` `docker-compose.yml`

### 2026-08-16 — Add engineering foundation and kernel module runtime (E0+E1)

- Commit: [`70921719a10cbda8ad5240e24c4e7bcc90633920`](https://github.com/sgodev2024/java-core/commit/70921719a10cbda8ad5240e24c4e7bcc90633920)
- Tác giả: sgodev2024
- Phạm vi file:

- `A` `.github/CODEOWNERS`
- `A` `.github/PULL_REQUEST_TEMPLATE.md`
- `A` `.github/workflows/backend-ci.yml`
- `A` `.github/workflows/frontend-ci.yml`
- `A` `backend/.mvn/wrapper/maven-wrapper.properties`
- `A` `backend/mvnw`
- `A` `backend/mvnw.cmd`
- `M` `backend/pom.xml`
- `A` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneModule.java`
- `A` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceModule.java`
- `M` `backend/src/main/java/vn/coreplatform/filemanagement/FileController.java`
- `A` `backend/src/main/java/vn/coreplatform/filemanagement/FileManagementModule.java`
- `A` `backend/src/main/java/vn/coreplatform/identity/IdentityModule.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/KernelModule.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/MigrationCoordinator.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/ModuleContributor.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/ModuleDescriptor.java`
- `A` `backend/src/main/java/vn/coreplatform/kernel/ModuleRegistry.java`
- `A` `backend/src/main/java/vn/coreplatform/permission/PermissionModule.java`
- `M` `backend/src/main/java/vn/coreplatform/shared/ApiExceptionHandler.java`
- `A` `backend/src/main/java/vn/coreplatform/shared/CorrelationIdFilter.java`
- `M` `backend/src/main/resources/application.yml`
- `A` `backend/src/test/java/vn/coreplatform/AbstractApiTest.java`
- `A` `backend/src/test/java/vn/coreplatform/boundaryfixture/dynamicresource/DynamicResourceBoundaryViolation.java`
- `A` `backend/src/test/java/vn/coreplatform/boundaryfixture/identity/IdentityBoundaryViolation.java`
- `A` `backend/src/test/java/vn/coreplatform/filemanagement/FileManagementTest.java`
- `A` `backend/src/test/java/vn/coreplatform/identity/AuthFlowTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/MigrationCoordinatorTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/ModuleBoundaryTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/ModuleRegistrationTest.java`
- `A` `backend/src/test/java/vn/coreplatform/kernel/ModuleRegistryTest.java`
- `A` `backend/src/test/java/vn/coreplatform/shared/AuditCorrelationTest.java`

### 2026-08-15 — Add tenant-authorized file storage and streaming

- Commit: [`70827f97865159178207433e18099d68c122a116`](https://github.com/sgodev2024/java-core/commit/70827f97865159178207433e18099d68c122a116)
- Tác giả: SGO Development
- Phạm vi file:

- `A` `backend/src/main/java/vn/coreplatform/filemanagement/FileController.java`
- `M` `backend/src/main/resources/application.yml`
- `A` `backend/src/main/resources/db/migration/V5__file_storage.sql`
- `M` `frontend/app/page.tsx`

### 2026-08-15 — Remove external CSV runtime dependency

- Commit: [`de087746d5ea63a6a10a3c07125739e2f1a03c31`](https://github.com/sgodev2024/java-core/commit/de087746d5ea63a6a10a3c07125739e2f1a03c31)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/pom.xml`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`

### 2026-08-15 — Add Dynamic Resource console and CSV operations

- Commit: [`3e5d21041bc5f603eda3da5a370e980a6570dd43`](https://github.com/sgodev2024/java-core/commit/3e5d21041bc5f603eda3da5a370e980a6570dd43)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/pom.xml`
- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `M` `frontend/app/page.tsx`

### 2026-08-15 — Enforce tenant-scoped policy and access management

- Commit: [`2bd78ad47608a73a7f3f632fc7543c8edda6768a`](https://github.com/sgodev2024/java-core/commit/2bd78ad47608a73a7f3f632fc7543c8edda6768a)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `A` `backend/src/main/java/vn/coreplatform/permission/AccessManagementController.java`
- `A` `backend/src/main/java/vn/coreplatform/permission/PermissionService.java`
- `M` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`
- `A` `backend/src/main/resources/db/migration/V4__permission_management.sql`

### 2026-08-15 — Add tenant permission and dynamic resource foundation

- Commit: [`f7e490b2b33441bce6beeec9b9f5d864222ead4d`](https://github.com/sgodev2024/java-core/commit/f7e490b2b33441bce6beeec9b9f5d864222ead4d)
- Tác giả: SGO Development
- Phạm vi file:

- `A` `backend/src/main/java/vn/coreplatform/dynamicresource/DynamicResourceController.java`
- `M` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`
- `A` `backend/src/main/resources/db/migration/V3__tenant_permission_dynamic_resource.sql`

### 2026-08-15 — Implement control plane operations backed by PostgreSQL

- Commit: [`996a826daf979c1c635053f2f93f541f5292b1cd`](https://github.com/sgodev2024/java-core/commit/996a826daf979c1c635053f2f93f541f5292b1cd)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `A` `backend/src/main/resources/db/migration/V2__control_plane_operations.sql`
- `M` `frontend/app/page.tsx`

### 2026-08-15 — Use compact standalone frontend runtime

- Commit: [`5cca3c8b1c0b0c8a5dd77b4f288e0e7bf3bfc94f`](https://github.com/sgodev2024/java-core/commit/5cca3c8b1c0b0c8a5dd77b4f288e0e7bf3bfc94f)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `frontend/.dockerignore`
- `M` `frontend/Dockerfile`
- `M` `frontend/next.config.ts`

### 2026-08-15 — Self-host frontend on corejava domain

- Commit: [`82f5ac3e0f201bfa2b48c1b20efaf74f004fcc0b`](https://github.com/sgodev2024/java-core/commit/82f5ac3e0f201bfa2b48c1b20efaf74f004fcc0b)
- Tác giả: SGO Development
- Phạm vi file:

- `A` `deploy/ubuntu20/nginx-frontend-corejava.conf`
- `A` `frontend/.dockerignore`
- `A` `frontend/Dockerfile`

### 2026-08-15 — Route production frontend through dedicated API domain

- Commit: [`73ff46893ee6d0c6c72e37c37f56899bfe8a5b78`](https://github.com/sgodev2024/java-core/commit/73ff46893ee6d0c6c72e37c37f56899bfe8a5b78)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`
- `A` `deploy/ubuntu20/nginx-api-corejava.conf`
- `M` `frontend/app/page.tsx`

### 2026-08-15 — Connect frontend to production API

- Commit: [`b5d5d26ac2c58e61e5a4c0b84f395f3ae9b02f0b`](https://github.com/sgodev2024/java-core/commit/b5d5d26ac2c58e61e5a4c0b84f395f3ae9b02f0b)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `frontend/app/page.tsx`

### 2026-08-15 — Fix PostgreSQL summary alias

- Commit: [`6b5f782f6dc519242f09fe54700bc6d34eb22b61`](https://github.com/sgodev2024/java-core/commit/6b5f782f6dc519242f09fe54700bc6d34eb22b61)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`

### 2026-08-15 — Fix session expiry persistence

- Commit: [`62673493fed5bc796e77a98021db925c9b5fb250`](https://github.com/sgodev2024/java-core/commit/62673493fed5bc796e77a98021db925c9b5fb250)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/identity/AuthController.java`

### 2026-08-15 — Secure bootstrap admin credentials

- Commit: [`11e6532fab7bdaa480d578e9f1a8ff7298b77f3b`](https://github.com/sgodev2024/java-core/commit/11e6532fab7bdaa480d578e9f1a8ff7298b77f3b)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/.env.example`
- `M` `backend/src/main/java/vn/coreplatform/identity/AuthController.java`
- `M` `backend/src/main/java/vn/coreplatform/identity/DemoAccountInitializer.java`
- `M` `backend/src/main/resources/application.yml`

### 2026-08-15 — Fix security filter compilation

- Commit: [`9b254255a044698b96f3b5a39f12ac304caa3e52`](https://github.com/sgodev2024/java-core/commit/9b254255a044698b96f3b5a39f12ac304caa3e52)
- Tác giả: SGO Development
- Phạm vi file:

- `M` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`

### 2026-08-15 — Initialize Java Core Platform

- Commit: [`295b0e6d6b200c9cd17cfe23d11683f8d04d19e8`](https://github.com/sgodev2024/java-core/commit/295b0e6d6b200c9cd17cfe23d11683f8d04d19e8)
- Tác giả: SGO Development
- Phạm vi file:

- `A` `.gitignore`
- `A` `README.md`
- `A` `backend/.env.example`
- `A` `backend/Dockerfile`
- `A` `backend/README.md`
- `A` `backend/pom.xml`
- `A` `backend/src/main/java/vn/coreplatform/CorePlatformApplication.java`
- `A` `backend/src/main/java/vn/coreplatform/controlplane/ControlPlaneController.java`
- `A` `backend/src/main/java/vn/coreplatform/identity/AuthController.java`
- `A` `backend/src/main/java/vn/coreplatform/identity/DemoAccountInitializer.java`
- `A` `backend/src/main/java/vn/coreplatform/security/SecurityConfig.java`
- `A` `backend/src/main/java/vn/coreplatform/shared/ApiExceptionHandler.java`
- `A` `backend/src/main/resources/application.yml`
- `A` `backend/src/main/resources/db/migration/V1__platform_baseline.sql`
- `A` `core-platform-architecture-standard-v1.1.md`
- `A` `core-platform-ba-requirements-v1.0.md`
- `A` `core-platform-database-architecture-v1.0.md`
- `A` `core-platform-runtime-architecture-v1.0.md`
- `A` `deploy/ubuntu20/README.md`
- `A` `deploy/ubuntu20/core-platform.env.example`
- `A` `deploy/ubuntu20/core-platform.service`
- `A` `deploy/ubuntu20/deploy.sh`
- `A` `deploy/ubuntu20/nginx-core-platform.conf`
- `A` `docker-compose.yml`
- `A` `frontend/.gitignore`
- `A` `frontend/.openai/hosting.json`
- `A` `frontend/README.md`
- `A` `frontend/app/chatgpt-auth.ts`
- `A` `frontend/app/globals.css`
- `A` `frontend/app/layout.tsx`
- `A` `frontend/app/page.tsx`
- `A` `frontend/build/sites-vite-plugin.ts`
- `A` `frontend/db/index.ts`
- `A` `frontend/db/schema.ts`
- `A` `frontend/drizzle.config.ts`
- `A` `frontend/drizzle/meta/_journal.json`
- `A` `frontend/eslint.config.mjs`
- `A` `frontend/examples/d1/app/api/notes/route.ts`
- `A` `frontend/examples/d1/db/schema.ts`
- `A` `frontend/next.config.ts`
- `A` `frontend/package-lock.json`
- `A` `frontend/package.json`
- `A` `frontend/postcss.config.mjs`
- `A` `frontend/public/favicon.svg`
- `A` `frontend/public/file.svg`
- `A` `frontend/public/globe.svg`
- `A` `frontend/public/og.png`
- `A` `frontend/public/window.svg`
- `A` `frontend/tests/rendered-html.test.mjs`
- `A` `frontend/tsconfig.json`
- `A` `frontend/vite.config.ts`
- `A` `frontend/worker/index.ts`
- `A` `technical-delivery-pack-v1.0/01-technical-implementation-specification.md`
- `A` `technical-delivery-pack-v1.0/02-implementation-backlog.md`
- `A` `technical-delivery-pack-v1.0/03-delivery-and-quality-checklist.md`
- `A` `technical-delivery-pack-v1.0/04-execution-start-plan.md`
- `A` `technical-delivery-pack-v1.0/README.md`
<!-- AUTO-GENERATED:END -->
