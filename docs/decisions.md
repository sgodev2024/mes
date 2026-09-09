# Dependency & Version Decision Register

Quy tắc (technical-delivery-pack-v1.0/01-technical-implementation-specification.md):
không thêm dependency mới nếu chưa ghi lý do, license và operational impact tại đây.
Version được pin qua `backend/pom.xml`; cấm dynamic version.

| Ngày | Dependency | Version | Scope | Lý do | License | Ghi chú vận hành |
|---|---|---|---|---|---|---|
| 2026-08-14 | Spring Boot (parent) | 3.5.4 | runtime | Baseline framework đã chốt trong CP-ARCH-002 | EPL-2.0/ORR | — |
| 2026-08-14 | PostgreSQL JDBC | quản lý bởi Boot BOM | runtime | Database chuẩn của platform | BSD-2 | — |
| 2026-08-14 | Flyway core + postgresql | quản lý bởi Boot BOM | runtime | Migration có version theo CP-DATA-003 | Apache-2.0 | — |
| 2026-08-14 | springdoc-openapi-webmvc-ui | 2.8.9 | runtime | API contract/Swagger cho developer (CAP-022) | Apache-2.0 | Tắt ở production nếu không cần |
| 2026-08-15 | spring-boot-testcontainers | quản lý bởi Boot BOM | test | `@ServiceConnection` cho integration test | Apache-2.0 | Cần Docker khi chạy test |
| 2026-08-15 | testcontainers junit-jupiter + postgresql | 1.21.4 (pin qua property `testcontainers.version`) | test | Integration test với PostgreSQL thật; 1.21.3 lỗi thương lượng API với Docker Engine 29 (400 trên /info) | MIT | Windows cần `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` + `TESTCONTAINERS_RYUK_DISABLED=true` (Ryuk qua npipe có thể giết container giữa chừng); CI Linux không cần |
| 2026-08-16 | ArchUnit (archunit-junit5) | 1.4.1 | test | Boundary verification E1-S02: kernel/shared trung tính, module không chạm nội bộ nhau, fixture vi phạm cố ý làm rule fail | Apache-2.0 | Spring Modulith hoãn đến khi tách Maven multi-module (sprint sau) — ghi ở đây để không quên |
| 2026-08-16 | Flyway programmatic qua `MigrationCoordinator` | quản lý bởi Boot BOM | runtime | E1-S04: advisory lock (`pg_advisory_lock`) serialize migration khi nhiều instance bật đồng thời; chạy BeanFactoryPostProcessor để thấy đủ Environment (kể cả DynamicPropertySource của test) trước khi bean tạo | Apache-2.0 | Tắt `spring.flyway` auto; credential migration tách biệt `DB_MIGRATION_USER` |
| 2026-08-16 | RLS ENABLE+FORCE + GUC `core.tenant_id` | PostgreSQL 17 built-in | runtime | E2-S03: isolation ở tầng database cho `dynamic_resource.*` và `files.file_object`; app gắn GUC mỗi lần lease connection và reset khi trả pool (`TenantAwareDataSource`) | PostgreSQL License | Role runtime `core_app` không DDL/owner/BYPASSRLS; thiếu GUC → 0 dòng (fail-closed) |
| 2026-08-16 | BouncyCastle bcprov-jdk18on | 1.81 | runtime | E3-S02: `Argon2PasswordEncoder` của spring-security-crypto cần BC để chạy Argon2id; Boot không quản lý version nên pin tay | MIT | Hash mới`{argon2}`; hash `{bcrypt}` cũ verify được và tự rehash khi login |
| 2026-08-15 | Maven Wrapper | 3.9.11 | build | `mvnw verify` chuẩn hóa mọi máy/CI (E0-S01) | Apache-2.0 | distributionUrl trỏ repo.maven.apache.org |
| 2026-08-18 | Domain Resource Adapter SPI + history contract | Core API 1.1.0 | runtime | Domain module tham gia Resource Registry/read/history mà không dùng generic repository hoặc generic command | Internal | Adapter phải `storageMode=DOMAIN`, unique resource type; module vẫn sở hữu invariant/transaction/persistence |
| 2026-08-18 | Core semver range gate | Core API 1.1.0 | runtime | Module khai báo exact/range (`>=1.1.0 <2.0.0`); startup fail-fast khi không tương thích | Internal | Chưa thay thế compatibility matrix và install/upgrade/rollback workflow |
| 2026-08-17 | Next.js + eslint-config-next | 16.3.1 | frontend runtime/build | App Router chính thức, standalone OCI; cập nhật khỏi 16.2.6 để xử lý security advisories và đồng bộ lint contract | MIT | Pin exact; Node.js 22; `npm audit` = 0; không dùng vinext/Vite |
| 2026-08-17 | SVG icon nội bộ | source-owned | frontend | Icon theo ngữ nghĩa module, không thêm runtime dependency/icon font | Nội bộ dự án | Render inline, kế thừa `currentColor`, tương thích Navigation Registry |
| 2026-09-07 | Apache POI `poi-ooxml` | 5.5.1 | MES backend | Đọc cấu trúc, công thức và cached value của biểu mẫu `.xlsx` trong Slice 1 | Apache-2.0 | Parse trong job worker; không chạy macro; giới hạn zip/file/row; LibreOffice cô lập chưa nằm trong Slice 1 |
| 2026-09-08 | Versioned MES field contract + SHA-256 contract hash | PostgreSQL V21 | MES data governance | Khóa schema nhận diện/field/rule của một phiên bản; ngăn sửa ngầm làm dữ liệu cùng version có nghĩa khác nhau | PostgreSQL License/Internal | Contract đã có dữ liệu là bất biến; thay đổi phải tạo version mới và UAT lại |
| 2026-09-08 | PostgreSQL advisory transaction lock | PostgreSQL 17 built-in | MES ingestion | Serialize phát hiện file trùng trong cùng ngày báo cáo và phát hành canonical theo batch khi có nhiều request đồng thời | PostgreSQL License | Lock chỉ sống trong transaction; claim batch dùng atomic update-returning |
| 2026-09-08 | Duplicate audit batch `SUPERSEDED` theo ngày báo cáo | MES ingestion v1.1 | MES traceability | Giữ bằng chứng upload trùng mà không parse/không tạo dữ liệu nghiệp vụ lần hai; không chặn cùng file khi dùng cho ngày báo cáo khác | Internal | Business key kiểm tra trùng là `tenant_id + reporting_date + checksum_sha256`; cùng file/cùng ngày tạo `SUPERSEDED`, cùng file/ngày khác được tiếp nhận |
| 2026-09-08 | Canonical internal dataset release | PostgreSQL V22 / MES 1.2.0-slice1.1 | MES data serving | Tách staging Excel khỏi dữ liệu đích; dashboard và báo cáo nội bộ đọc release canonical đã khóa, có lineage và tenant isolation | PostgreSQL License/Internal | Tạo `internal_dataset_release`, `canonical_product`, `canonical_partner`, `canonical_daily_metric`; release và metric bất biến; `LOCKED` và `PUBLISHED` cùng transaction, mỗi batch tối đa một release |
| 2026-09-08 | MES Portal mode fail-closed | MES 1.2.0-slice1.1 | MES integration | Pending Portal cho tới khi có hợp đồng tích hợp; ngăn ghi trạng thái đã nộp giả hoặc gọi mạng ngoài ý muốn | Internal | `DISABLED` là mặc định; `MANUAL_TRACKING` chỉ bật có chủ đích; `API` vẫn `NOT_CONFIGURED`; khi disabled endpoint submit trả `409 MES_PORTAL_INTEGRATION_DISABLED` và không thay đổi database/outbox/network |
| 2026-09-08 | MES Data Steward master confirmation | PostgreSQL V23 / MES 1.3.0-slice1.2 | MES data governance | Không tự coi mã/tên trong Excel là master chính thức; yêu cầu xác nhận có audit và optimistic locking | PostgreSQL License/Internal | Sản phẩm/đối tác khởi tạo `UNVERIFIED`; quyền `MES_MASTER_DATA:READ/APPROVE`; chỉ `RESOLVED` mới đủ gate đối soát |
| 2026-09-08 | Canonical inventory reconciliation | MES 1.3.0-slice1.2 | MES data quality | Đối soát nhập–xuất–tồn trên cùng read model với Dashboard, không suy diễn số từ workbook/staging | Internal | Cần 5 contract/ngày, tồn D-1/D và master resolved; không cấu hình tolerance ngầm; thiếu điều kiện không được trả `BALANCED` |
| 2026-09-08 | Executive Dashboard reads canonical only | MES 1.3.0-slice1.2 | MES frontend | Ngăn số mô phỏng hoặc dữ liệu chưa duyệt xuất hiện trong quyết định điều hành | Internal | Route Dashboard/Kho/Danh mục gọi API thật, có loading/empty/error; Trang chủ và Quản trị hệ thống Core không đổi |
| 2026-09-09 | MES dual-output operating model | MES Blueprint / Slice 1.2→5 | MES product architecture | Một lần thu thập và chuẩn hóa phục vụ đồng thời quản trị nội bộ và tự động gửi Portal; tránh hai bộ số liệu hoặc hai pipeline | Internal | Hiện tại nội bộ dùng canonical còn nhân sự nộp Portal thủ công; khi có API, Portal Gateway chỉ gửi release `PUBLISHED`, có mapping version, idempotency, retry, biên nhận và reconciliation |
| 2026-09-09 | MES Navigation Baseline | MES-NAV-001 | MES frontend/backend contract | Khóa cấu trúc điều hướng toàn doanh nghiệp để mọi lát cắt phát triển dùng chung một taxonomy ổn định | Internal | `Trang chủ → MES (18 menu theo thứ tự đã chốt) → Quản trị hệ thống`; tính năng mới nằm bên trong menu hiện có; source 11 route cũ còn chờ migration |

## Quy ước chạy test không có Docker

Test mặc định khởi PostgreSQL 17 bằng Testcontainers. Khi máy không có Docker
(Windows không WSL, môi trường hạn chế), đặt các biến sau trước khi chạy `./mvnw verify`:

```text
IT_DB_URL=jdbc:postgresql://127.0.0.1:55432/core_platform
IT_DB_USER=core_app
IT_DB_PASSWORD=core_app_dev
```

Database đích phải trống hoặc đã đúng schema do Flyway quản lý; mọi test dùng
suffix ngẫu nhiên nên chạy lại nhiều lần không xung đột dữ liệu.
