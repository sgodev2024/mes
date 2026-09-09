# MES V1 Than Mạo Khê — Backlog giao việc cho đội ngũ đang đào tạo

Phiên bản: 1.0  
Ngày lập: 2026-08-27  
Nguồn phân tích: `Ma_tran_chuc_nang_MES_V1_Than_Mao_Khe.xlsx` và `MES_V1_UI_Prototype.html`  
Phạm vi repository: `mes`; không sửa trực tiếp Java Core.

## 1. Kết luận phân tích

Ma trận hiện có 62 capability V1, được truy vết từ 94 use-case nguồn, 490 dòng từ điển trường dữ liệu, 7 vai trò, 25 chuyển trạng thái và 15 vấn đề cần khảo sát/xác nhận. Prototype HTML thể hiện 8 nhóm màn hình: Dashboard, Kế hoạch sản xuất, Thực hiện sản xuất, Kho & luân chuyển, KCS & nghiệm thu, Báo cáo & cảnh báo, Danh mục dùng chung và Quản trị & phân quyền.

Không triển khai theo cách “mỗi use-case là một màn hình”. CRUD, import, export và workflow của cùng một đối tượng phải gom trong một màn hình nghiệp vụ thống nhất.

Thứ tự đào tạo và triển khai:

1. Chạy hệ thống, đọc Core và viết kiểm thử đơn giản.
2. Dựng shell MES và component giao diện dùng chung.
3. CRUD danh mục độc lập, ít rủi ro.
4. Phân quyền và phạm vi dữ liệu.
5. Kế hoạch và báo cáo ngày/ca.
6. Kho và công thức tồn.
7. KCS, giám định và nghiệm thu.
8. Dashboard, cảnh báo, tích hợp, hiệu năng và nghiệm thu.

## 2. Quy ước giao task

| Mức | Đối tượng | Kích thước khuyến nghị |
|---|---|---|
| `E1` | Nhân sự mới, cần checklist chi tiết | 0,5–1 ngày; 1–2 điểm |
| `E2` | Đã hoàn thành ít nhất 5 task E1 | 1–2 ngày; 3 điểm |
| `E3` | Làm cùng reviewer có kinh nghiệm | 2–4 ngày; 5–8 điểm |

Mỗi task chỉ được chuyển `Hoàn thành` khi có: mã nguồn, migration nếu có, validation, trạng thái tải/rỗng/lỗi, kiểm tra quyền API, test tối thiểu, ảnh/video nghiệm thu và cập nhật tài liệu kỹ thuật.

Nhân sự đang đào tạo không tự quyết định workflow, công thức tồn kho, nguồn dữ liệu chuẩn hoặc quyền duyệt. Nếu gặp nội dung đánh dấu `BA-GATE`, dừng ở mock/API contract và chuyển trạng thái `Chờ BA`.

## 3. Backlog theo thứ tự giao việc

### Giai đoạn A — Làm quen dự án và nền tảng

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-001 | Clone repository MES và chạy Docker local | E1 | 1 | — | Backend/frontend/PostgreSQL healthy; đăng nhập được; ghi lại lệnh chạy trong README. |
| MES-002 | Đọc cấu trúc Core và lập sơ đồ thư mục MES | E1 | 1 | MES-001 | Chỉ rõ phần Core không được sửa, vị trí module MES, migration, API, frontend và test. |
| MES-003 | Tạo checklist quy tắc branch, commit và Pull Request | E1 | 1 | MES-001 | Hoàn thành tại `docs/10-mes-git-branch-commit-pr-checklist.md` và `.github/pull_request_template.md`; có mẫu tên branch, commit, PR, bằng chứng test và nguyên tắc không đưa secret lên Git. |
| MES-004 | Tạo module descriptor `mes-production` | E1 | 2 | MES-002 | Module load thành công; khai báo dependency Core; không phát sinh menu demo cũ. |
| MES-005 | Đăng ký nhóm menu `Nghiệp vụ MES` | E1 | 2 | MES-004 | Giữ nguyên Trang chủ và Quản trị hệ thống Core; menu MES lấy từ Navigation Registry. |
| MES-006 | Tạo trang “MES chưa có dữ liệu” dùng chung | E1 | 1 | MES-005 | Có empty state, nút làm mới, responsive và không dùng dữ liệu fix cứng. |
| MES-007 | Viết smoke test module và navigation | E1 | 2 | MES-004, MES-005 | Test xác nhận module được load và menu theo đúng route. |
| MES-008 | Lập file dữ liệu demo không chứa thông tin thật | E1 | 1 | MES-001 | Dữ liệu có mã rõ ràng, idempotent và chỉ bật ở profile local/demo. |

### Giai đoạn B — Shell và component frontend theo prototype

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-009 | Áp dụng shell Next.js hiện tại của Core cho MES | E1 | 2 | MES-005 | Không thiết kế lại shell Core; header/sidebar/profile giữ đúng baseline. |
| MES-010 | Tạo token màu và typography MES Than Mạo Khê | E1 | 1 | MES-009 | Màu xanh navy/xanh dương từ prototype, có biến CSS, tương phản đọc được. |
| MES-011 | Tạo component PageHeader và vùng hành động | E1 | 1 | MES-009 | Dùng lại ở tối thiểu 3 trang; hỗ trợ tiêu đề, mô tả và nút hành động. |
| MES-012 | Tạo component FilterBar | E1 | 2 | MES-009 | Hỗ trợ kỳ, đơn vị, ca, trạng thái; có reset và trạng thái đang tải. |
| MES-013 | Tạo component DataTable dùng chung | E2 | 3 | MES-009 | Có STT, loading, empty, error, phân trang và cột hành động icon. |
| MES-014 | Tạo component StatusBadge và ánh xạ trạng thái | E1 | 1 | MES-013 | Không hiển thị mã trạng thái tiếng Anh trực tiếp cho người dùng. |
| MES-015 | Tạo modal form chuẩn MES | E2 | 3 | MES-011 | Validate bắt buộc, lỗi theo field, chống submit hai lần, đóng an toàn. |
| MES-016 | Tạo API client và chuẩn hóa Problem JSON | E2 | 3 | MES-001 | Tự gắn token/correlation id; xử lý 401/403/409/422/500 và JSON rỗng. |
| MES-017 | Tái dựng trang Dashboard tĩnh theo prototype | E1 | 2 | MES-010–MES-014 | Chỉ dùng fixture demo; đúng layout KPI, biểu đồ, cảnh báo; chưa kết nối API. |
| MES-018 | Kiểm thử responsive các trang MES | E1 | 2 | MES-017 | Desktop 1920×1080, laptop 1366×768 và mobile không vỡ layout. |

### Giai đoạn C — Danh mục nền, ưu tiên CRUD đơn giản

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-019 | Thiết kế migration bảng trạng thái hiệu lực dùng chung | E2 | 3 | MES-004 | Có tenant, code duy nhất, effective date, active/inactive, audit columns. |
| MES-020 | CRUD Ca làm việc | E1 | 2 | MES-013, MES-015, MES-019 | Danh sách/thêm/sửa/ngừng/mở lại; không cho thời gian kết thúc bằng bắt đầu. |
| MES-021 | CRUD Nhóm chỉ tiêu | E1 | 2 | MES-019 | Mã duy nhất; không xóa cứng bản ghi đã dùng. |
| MES-022 | CRUD Chỉ tiêu cơ bản | E2 | 3 | MES-021 | Bắt buộc nhóm và đơn vị tính; chỉ chọn nhóm hiệu lực. |
| MES-023 | CRUD Chỉ tiêu chi tiết | E2 | 3 | MES-022 | Bắt buộc chỉ tiêu cha; ngăn tham chiếu không hợp lệ. |
| MES-024 | CRUD Loại sản phẩm | E1 | 2 | MES-019 | Danh sách, tìm kiếm, thêm, sửa, ngừng và mở lại. |
| MES-025 | CRUD Nhóm sản phẩm | E2 | 3 | MES-024 | Nhóm phải thuộc loại sản phẩm hiệu lực. |
| MES-026 | CRUD Sản phẩm | E2 | 3 | MES-025 | Bắt buộc loại, nhóm, ĐVT; mã duy nhất; giữ lịch sử hiệu lực. |
| MES-027 | CRUD Loại và nhóm đối tác | E2 | 3 | MES-019 | Quan hệ loại–nhóm hợp lệ; không xóa cứng khi có tham chiếu. |
| MES-028 | CRUD Đối tác/khách hàng | E2 | 3 | MES-027 | Tìm kiếm, trạng thái, kiểm tra trùng mã; chưa làm import. |
| MES-029 | CRUD Kho/cảng | E2 | 3 | MES-019 | Gắn đơn vị quản lý; không xóa kho có giao dịch; `BA-GATE Q06/Q10`. |
| MES-030 | CRUD Phương tiện | E2 | 3 | MES-019 | Mã/biển số duy nhất trong thời gian hiệu lực; `BA-GATE Q08/Q10`. |
| MES-031 | CRUD địa điểm hầm lò/lộ thiên dùng một pattern | E2 | 3 | MES-019 | Hai loại dùng chung service/component; `BA-GATE Q03/Q10`. |
| MES-032 | Import danh mục có kiểm tra lỗi theo dòng | E3 | 5 | MES-021–MES-030 | Preview trước import; báo dòng lỗi; transaction rõ ràng; không tạo trùng. |
| MES-033 | Export danh mục theo bộ lọc | E1 | 2 | MES-021–MES-030 | File tải xuống đúng cột, Unicode và đúng phạm vi dữ liệu. |

### Giai đoạn D — Phân quyền và phạm vi dữ liệu

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-034 | Khai báo 7 role MES trong module | E2 | 3 | MES-004 | ADMIN, BAN_DIEU_HANH, KE_HOACH, KCS, KCM, PHAN_XUONG, VIEWER. |
| MES-035 | Ánh xạ quyền R/C/U/D/S/A/E/I cho danh mục | E2 | 3 | MES-034 | Frontend ẩn hành động và backend từ chối đúng 403 khi không có quyền. |
| MES-036 | Thiết kế data scope theo đơn vị/kho | E3 | 5 | MES-034 | Policy fail-closed; test không đọc chéo phạm vi; `BA-GATE Q14`. |
| MES-037 | Tạo dữ liệu demo và test cho từng role | E2 | 3 | MES-035, MES-036 | Mỗi role có một tài khoản demo local và ma trận API test tự động. |
| MES-038 | Kiểm thử audit cho thay đổi quyền và dữ liệu | E2 | 3 | MES-035 | Log có actor, action, resource, thời gian, kết quả và correlation id. |

### Giai đoạn E — Kế hoạch và thực hiện sản xuất

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-039 | Danh sách kế hoạch chỉ tiêu và bộ lọc | E2 | 3 | MES-022, MES-036 | Lọc năm/tháng/ngày, đơn vị, chỉ tiêu, trạng thái; export đúng quyền. |
| MES-040 | Form lập kế hoạch Draft | E2 | 3 | MES-039 | Bắt buộc kỳ, đơn vị, chỉ tiêu, giá trị; chưa cho duyệt. |
| MES-041 | Workflow trình–duyệt–giao kế hoạch | E3 | 5 | MES-040 | Chuyển trạng thái hợp lệ, audit, chống cập nhật sai version; `BA-GATE Q02`. |
| MES-042 | Phiên bản và điều chỉnh kế hoạch | E3 | 5 | MES-041 | Không ghi đè bản đã giao; lưu trước/sau, lý do và thời điểm áp dụng. |
| MES-043 | Kế hoạch tiêu thụ | E3 | 5 | MES-028, MES-026, MES-041 | Gắn kỳ, sản phẩm, đối tác; dùng workflow kế hoạch chung. |
| MES-044 | Form báo cáo sản xuất ngày | E2 | 3 | MES-022, MES-036 | Draft/Submitted; ghi nguồn và người nhập; `BA-GATE Q01/Q03`. |
| MES-045 | Kết quả sản xuất theo ca | E3 | 5 | MES-020, MES-031, MES-044 | Duy nhất theo ca+đơn vị+chỉ tiêu+địa điểm; `BA-GATE Q03`. |
| MES-046 | Xác nhận và khóa báo cáo ngày/ca | E3 | 5 | MES-044, MES-045 | Submitted→Confirmed→Locked; không sửa khi khóa; quyền mở lại phải được chốt. |

### Giai đoạn F — Kho và luân chuyển

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-047 | Thiết kế sổ giao dịch kho bất biến | E3 | 8 | MES-026, MES-029 | Không cập nhật số dư trực tiếp; mọi phát sinh truy về chứng từ và audit. |
| MES-048 | Nhập tồn đầu kỳ | E2 | 3 | MES-047 | Draft→Confirmed→Locked; chỉ dùng cho chuyển đổi; có import kiểm tra lỗi. |
| MES-049 | Phiếu nhập kho từ sản xuất | E3 | 5 | MES-047 | Draft/Submitted/Confirmed/Adjusted/Cancelled; số lượng >0. |
| MES-050 | Phiếu nhập kho sàng tuyển | E2 | 3 | MES-049 | Tái sử dụng form/service phiếu; liên kết phiếu/lô nguồn nếu có. |
| MES-051 | Phiếu xuất kho cho sàng tuyển | E3 | 5 | MES-047 | Kiểm tra tồn khả dụng; `BA-GATE Q06/Q07`. |
| MES-052 | Phiếu xuất kho tiêu thụ | E3 | 5 | MES-028, MES-030, MES-051 | Gắn đối tác/phương tiện; `BA-GATE Q08`. |
| MES-053 | Điều chuyển kho | E3 | 5 | MES-051 | Kho nguồn khác kho đích; xác nhận tạo hai bút toán nguyên tử. |
| MES-054 | Báo cáo tồn kho và drill-down | E3 | 5 | MES-048–MES-053 | Tồn đầu + nhập − xuất ± điều chỉnh = tồn cuối; truy vết được chứng từ. |
| MES-055 | Báo cáo nhập, xuất và sổ chi tiết sản phẩm | E2 | 3 | MES-054 | Chỉ tính chứng từ Confirmed/Adjusted; loại trừ Cancelled. |
| MES-056 | Quy trình kiểm kê | E3 | 8 | MES-054 | Snapshot tồn sổ; nhập thực tế; giải trình chênh lệch; Posted tạo điều chỉnh. |

### Giai đoạn G — KCS, nghiệm thu, Dashboard và cảnh báo

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-057 | Giám định sản xuất | E3 | 5 | MES-044, MES-047 | Giữ số liệu trước/sau; Draft→Confirmed→Applied; `BA-GATE Q04`. |
| MES-058 | Giám định tiêu thụ | E3 | 5 | MES-052, MES-057 | Liên kết lô/chứng từ; không ghi đè dữ liệu ban đầu. |
| MES-059 | Tổng hợp và đối soát nghiệm thu tháng | E3 | 8 | MES-046, MES-057, MES-058 | Draft→Reconciling→Confirmed→Closed; `BA-GATE Q05/Q09`. |
| MES-060 | Dashboard điều hành kết nối API | E3 | 5 | MES-039, MES-046, MES-054 | KPI động, lọc kỳ/đơn vị/ca, drill-down và hiển thị trạng thái dữ liệu. |
| MES-061 | Dashboard tồn kho/chất lượng/tiêu thụ | E3 | 5 | MES-054, MES-057–MES-060 | Số liệu chỉ từ bản xác nhận và truy vết được nguồn. |
| MES-062 | Báo cáo chỉ tiêu, kế hoạch, sàng tuyển và tiêu thụ | E3 | 8 | MES-043, MES-055, MES-060 | Bộ lọc dùng chung; export Excel/PDF; `BA-GATE Q12`. |
| MES-063 | Cảnh báo chậm tiến độ | E3 | 5 | MES-039, MES-046 | Ngưỡng cấu hình; chống cảnh báo lặp; `BA-GATE Q11`. |
| MES-064 | Workflow tiếp nhận và xử lý cảnh báo | E2 | 3 | MES-063 | New→Acknowledged→InProgress→Resolved→Closed; bắt buộc kết quả xử lý. |

### Giai đoạn H — Tích hợp, chất lượng và nghiệm thu

| ID | Task giao việc | Mức | Điểm | Phụ thuộc | Tiêu chí nghiệm thu chính |
|---|---|---:|---:|---|---|
| MES-065 | Lập contract Data Lake/API và fixture giả lập | E3 | 5 | BA-GATE Q01 | Có schema, version, idempotency, error mapping và dữ liệu test. |
| MES-066 | Background job đồng bộ dữ liệu nguồn | E3 | 5 | MES-065 | Retry/backoff, dead-letter, metric, audit và không tạo trùng. |
| MES-067 | Kiểm thử phân quyền toàn ma trận | E3 | 8 | MES-035–MES-064 | Test dương/âm cho role, action và data scope; API luôn là lớp quyết định cuối. |
| MES-068 | Kiểm thử workflow và concurrency | E3 | 8 | MES-041, MES-046, MES-049, MES-059 | Chặn transition sai; optimistic locking; submit lặp không tạo giao dịch kép. |
| MES-069 | Kiểm thử công thức tồn kho và đối soát | E3 | 8 | MES-054, MES-056 | Bộ dữ liệu chuẩn cân bằng; phát hiện sai số và tái lập tồn tại một thời điểm. |
| MES-070 | Kiểm thử hiệu năng truy vấn Dashboard/báo cáo | E3 | 5 | MES-060–MES-062 | Có dataset mục tiêu, explain plan, index và ngưỡng phản hồi được duyệt. |
| MES-071 | Hoàn thiện log, metric, health và cảnh báo vận hành | E2 | 3 | MES-066 | Không ghi secret/PII; có correlation id và health cho dependency. |
| MES-072 | Nghiệm thu Docker local và tài liệu triển khai | E2 | 5 | MES-067–MES-071 | Build sạch, migration từ DB trống và DB nâng cấp, seed demo, backup/restore và runbook. |

## 4. Các task BA-GATE phải làm trước workflow nhạy cảm

| ID | Nội dung cần xác nhận | Chặn task |
|---|---|---|
| BA-MES-01 | Chốt Source of Record và phương thức API/read-only DB/export cho từng dữ liệu | MES-044, MES-045, MES-057, MES-065 |
| BA-MES-02 | Chốt cấp lập/trình/duyệt/giao kế hoạch và số cấp duyệt | MES-041–MES-043 |
| BA-MES-03 | Chốt dữ liệu sản xuất ngày/ca, địa điểm, nguồn tự động và dữ liệu nhập tay | MES-044–MES-046 |
| BA-MES-04 | Chốt hệ thống KCS hiện tại, dữ liệu giám định/chất lượng và cơ chế xác nhận | MES-057, MES-058 |
| BA-MES-05 | Chốt đơn vị tạo/xác nhận tồn kho, số phiếu/quy tắc đánh số | MES-047–MES-053 |
| BA-MES-06 | Chốt có cho tồn âm và quy trình ngoại lệ | MES-051–MES-054 |
| BA-MES-07 | Chốt cách liên kết đầu vào–đầu ra sàng tuyển/lô sản xuất | MES-050, MES-055, MES-062 |
| BA-MES-08 | Chốt nguồn dữ liệu cân, giao vận, khách hàng, phương tiện và trường chuẩn | MES-030, MES-052, MES-065 |
| BA-MES-09 | Chốt công thức nghiệm thu tháng, bên xác nhận và cách nhận/chốt/mở lại | MES-059 |
| BA-MES-10 | Chốt chủ dữ liệu của đơn vị, kho, sản phẩm, đối tác, phương tiện, địa điểm, chỉ tiêu | MES-019–MES-032 |
| BA-MES-11 | Chốt ngưỡng cảnh báo, công thức, người nhận và quyền đóng/mở lại | MES-063, MES-064 |
| BA-MES-12 | Chốt mẫu báo cáo chính thức và định dạng xuất | MES-062 |
| BA-MES-13 | Chốt phạm vi và nền tảng mobile; không làm mobile trước khi web ổn định | Backlog Mobile P2 |
| BA-MES-14 | Chốt phạm vi dữ liệu theo đơn vị/kho/chỉ tiêu và trường hợp kiêm nhiệm | MES-036, MES-067 |
| BA-MES-15 | Chốt thời điểm khóa ngày/tháng và quyền mở lại, thời hạn điều chỉnh | MES-046, MES-059 |

## 5. Lô giao việc đầu tiên đề xuất

Chỉ giao 10 task đầu tiên: `MES-001` đến `MES-010`. Mỗi nhân sự nhận tối đa một task đang làm. Sau khi hoàn thành ba task E1 liên tiếp đạt review, mới giao task E2. Không giao task E3 cho một nhân sự mới làm độc lập.

Phân bổ gợi ý cho 5 nhân sự:

| Nhân sự | Task đầu tiên | Reviewer |
|---|---|---|
| Dev 1 | MES-001 → MES-006 | Tech lead |
| Dev 2 | MES-002 → MES-004 | Tech lead |
| Dev 3 | MES-003 → MES-007 | Tech lead |
| Dev 4 | MES-008 → MES-010 | Frontend reviewer |
| Dev 5 | MES-009 → MES-011 | Frontend reviewer |

Không triển khai đồng thời hai task có cùng file migration hoặc cùng component nền. Task sau chỉ bắt đầu khi dependency đã merge và pipeline xanh.

## 6. Definition of Done dùng cho mọi task

- Chỉ thay đổi repository MES; không commit vào repository Core.
- Không sửa shell và màn hình Quản trị hệ thống của Core nếu task không được duyệt riêng.
- Không hard-code dữ liệu nghiệp vụ trong component production.
- Backend kiểm tra tenant, role, action và data scope; ẩn nút frontend không được coi là phân quyền.
- Có migration tiến, không sửa migration đã chạy.
- Có validation frontend và backend; thông báo lỗi bằng tiếng Việt dễ hiểu.
- Có loading, empty, error và disabled state.
- Có test cho happy path, validation và từ chối quyền.
- Có audit cho create/update/deactivate/activate/submit/approve/cancel/adjust/close/reopen.
- `docker compose up -d --build` chạy được từ dữ liệu sạch.
- PR ghi rõ task ID, ảnh giao diện, API đã kiểm thử, migration và rủi ro còn lại.

## 7. Nội dung chưa đưa vào lô đầu

- Mobile P2 (`MOB-AUTH-001`, `MOB-REP-001..003`).
- Tích hợp hệ thống thật khi Source of Record chưa chốt.
- Workflow duyệt/chốt/mở lại khi chưa hoàn tất BA-GATE.
- Công thức tồn âm, ngoại lệ và nghiệm thu chưa được đơn vị nghiệp vụ xác nhận.
- Tối ưu báo cáo lớn khi chưa có quy mô dữ liệu và SLA truy vấn.
