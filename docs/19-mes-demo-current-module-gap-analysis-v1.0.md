# MES — Đối Chiếu Demo Cũ Và Module Hiện Tại V1.0

**Ngày phân tích:** 09/09/2026  
**Phạm vi:** bản demo `mao-khe-mes-demo.khacthuat-it.chatgpt.site` và source MES Slice 1.2 local  
**Mục đích:** xác định phần kế thừa đúng, phần đã vận hành thật và backlog chưa khớp

## 1. Nguồn bằng chứng và giới hạn

- URL bản demo hiện yêu cầu `Sign in with ChatGPT`, nên không thể đọc nội dung sau đăng nhập bằng HTTP read-only.
- Việc đăng nhập tài khoản ChatGPT không được tự động hóa.
- Bản nguồn Sites đã xuất bản còn đầy đủ tại `mes-layout-site/public/mes-layout.html` trong workspace visualization của chính task. Source này chứa shell, menu, từng view demo và dữ liệu mẫu, vì vậy được dùng làm baseline đối chiếu.
- Demo không có cơ chế đăng nhập/phân quyền ứng dụng riêng: nhãn `Quản trị viên` và dữ liệu quản trị được hard-code trong HTML. “Admin cao nhất” của demo không tương đương tài khoản `ROLE_PLATFORM_ADMIN` đang được backend Core kiểm tra thật.
- Bản hiện tại được đối chiếu từ Navigation Registry, Next.js component, Java controller/service, migration V20–V23 và tài liệu trạng thái triển khai.

Không coi dữ liệu hoặc thao tác trong demo HTML là backend thật. Demo cũ là prototype tĩnh.

## 2. Kết luận điều hành

| Chỉ số | Kết quả | Diễn giải |
|---|---:|---|
| Danh mục demo cũ được giữ lại | 10/10 | Không mất module nghiệp vụ cũ |
| Module mới bổ sung | 1 | `Trung tâm báo cáo ngày` để quản trị Excel/workflow/canonical |
| Route MES hiện tại | 11 | Được đăng ký động bằng Navigation Registry |
| Route đang dùng API thật | 4/11 | Dashboard, Trung tâm báo cáo ngày, Kho/đối soát, Danh mục một phần |
| Route còn prototype | 7/11 | Kế hoạch, thực hiện, KCS, giao vận, cảnh báo, nhập bổ sung, báo cáo |
| Màn demo cũ dùng API thật | 0 | Toàn bộ số liệu và nút là minh họa |

`4/11` chỉ phản ánh độ phủ màn hình đã nối API, không phải phần trăm hoàn thành toàn hệ thống MES. Bốn màn hình live vẫn cần UAT dữ liệu có phát sinh và các gate production.

## 3. Đối chiếu cấu trúc tổng thể

### 3.1 Phần khớp

- Cùng cấu trúc cấp cao: `Trang chủ → Nghiệp vụ MES → Quản trị hệ thống`.
- Các trang MES nằm trực tiếp dưới `Nghiệp vụ MES`, không có cấp `Nghiệp vụ → MES → chức năng` dư thừa.
- Core shell và nhóm Quản trị hệ thống vẫn giữ các chức năng: module, tài nguyên mở rộng, người dùng, cơ cấu tổ chức, vai trò/phân quyền, Events & Jobs, tệp tin và cấu hình.
- Tất cả 10 danh mục nghiệp vụ từng demo vẫn có route tương ứng.
- Bản hiện tại thay menu hard-code của demo bằng Navigation Registry, lọc theo permission `MES_REPORT:READ`.

### 3.2 Phần thay đổi đúng kiến trúc

- Demo dùng dữ liệu tĩnh; bản hiện tại tách dữ liệu thật khỏi prototype và gắn nhãn rõ màn hình mô phỏng.
- Dashboard hiện tại không giữ các KPI đẹp nhưng chưa có nguồn như “24.580 tấn” hoặc “86,7%”. Nó chỉ đọc canonical release đã duyệt.
- Bổ sung `Trung tâm báo cáo ngày`, vì demo cũ không có workflow upload → kiểm tra → xác nhận → phê duyệt → khóa/phát hành.
- `Danh mục hệ thống` hiện chuyển từ card minh họa sang Data Steward cho sản phẩm/đối tác, có version, audit và trạng thái xác nhận.
- `Kho & luân chuyển` hiện ưu tiên đối soát nhập-xuất-tồn canonical thay cho bảng giao dịch mẫu.

## 4. Ma trận từng module

| STT | Demo cũ | Module hiện tại | Mức khớp | Hiện trạng thật | Chênh lệch cần xử lý |
|---:|---|---|---|---|---|
| 1 | Dashboard tổng quan | Dashboard điều hành/BGĐ | Khớp mục tiêu, khác KPI | API canonical thật | Chưa có kế hoạch-thực hiện, sản lượng theo ca, KPI KCS và cảnh báo vận hành thật |
| 2 | Kế hoạch điều hành | Kế hoạch điều hành | Khớp menu/UI | Prototype | Chưa có plan version, CRUD, trình duyệt, điều chỉnh, giao kế hoạch và API |
| 3 | Thực hiện sản xuất | Thực hiện sản xuất | Khớp menu/UI | Prototype | Chưa có ghi nhận ca/ngày/đơn vị, xác nhận sản lượng, API và liên kết kế hoạch |
| 4 | Kho & luân chuyển than | Kho & luân chuyển than | Khớp một phần | Đối soát API thật | Chưa có sổ giao dịch và CRUD nhập/xuất/điều chuyển/kiểm kê như demo |
| 5 | KCS & chất lượng | KCS & chất lượng | Khớp menu/UI | Prototype | Chưa có lô mẫu, chỉ tiêu độ tro/ẩm, giám định, nghiệm thu, workflow và API |
| 6 | Tiêu thụ & giao vận | Tiêu thụ & giao vận | Khớp menu | Prototype | Chưa có lệnh giao, cân, phương tiện, bàn giao, trạng thái và API |
| 7 | Cảnh báo & tiến độ | Cảnh báo & tiến độ | Khớp menu/UI | Prototype | Chưa có rule/threshold, alert engine, giao xử lý, SLA, notification và API |
| 8 | Danh mục hệ thống | Danh mục hệ thống | Khớp một phần | Sản phẩm/đối tác dùng API thật | Kho/cảng, chỉ tiêu, phương tiện và mapping đơn vị-phân xưởng chưa có contract/API |
| 9 | Cổng nhập liệu bổ sung | Cổng nhập liệu bổ sung | Khớp menu/UI | Prototype | Chưa có manual-entry schema, form động và workflow; cần tránh trùng chức năng Trung tâm báo cáo ngày |
| 10 | Báo cáo & đối soát | Báo cáo & đối soát | Khớp menu/UI | Prototype | Chưa có report catalog, báo cáo kế hoạch/sản xuất/KCS/tiêu thụ, export và drill-down |
| 11 | Không có | Trung tâm báo cáo ngày | Bổ sung mới | API/workflow thật | Sáu contract pilot; cần mở rộng dần lên 98 mẫu và Portal Gateway |

## 5. Đối chiếu Dashboard

### Demo cũ có

- sản lượng hôm nay;
- tiến độ kế hoạch;
- tồn kho;
- số cảnh báo;
- biểu đồ sản lượng theo ca;
- danh sách cảnh báo gần nhất.

### Bản hiện tại có dữ liệu thật

- số canonical release;
- số điểm dữ liệu/dòng nguồn;
- số tham chiếu master chưa xác nhận;
- số chênh lệch tồn kho;
- tổng chỉ tiêu canonical theo ngày;
- độ phủ 5 Data Contract đối soát;
- sản phẩm cần BGĐ chú ý và nguyên nhân.

### Kết luận

Dashboard mới đúng hơn về độ tin cậy và lineage nhưng chưa đủ góc nhìn điều hành như demo. Không đưa lại số mẫu. Cần bổ sung KPI demo theo thứ tự: sản lượng ngày/ca → kế hoạch-thực hiện → chất lượng → cảnh báo, chỉ sau khi từng domain có canonical contract và API thật.

## 6. Đối chiếu Danh mục hệ thống

| Danh mục | Demo cũ | Bản hiện tại | Kết luận |
|---|---|---|---|
| Đơn vị/phân xưởng | Card minh họa | Core có cơ cấu tổ chức; MES chưa có mapping vận hành | Tái sử dụng Core organization, bổ sung MES site/workshop mapping thay vì tạo bảng tổ chức trùng |
| Kho/vị trí kho | Card minh họa | Tab hiện trạng thái chưa có contract | Cần xây warehouse/location master |
| Sản phẩm than | Card minh họa | API Data Steward `UNVERIFIED/RESOLVED` | Đã tiến bộ hơn demo; cần UAT danh mục thật |
| Chỉ tiêu vận hành | Card minh họa | Tab hiện trạng thái chưa có contract | Cần metric catalog, unit, aggregation và formula version |
| Đối tác | Không thể hiện rõ | API nhà cung cấp/khách hàng canonical | Là phần bổ sung đúng từ workbook |
| Phương tiện | Chỉ xuất hiện trong giao vận | Tab chưa có contract | Xây cùng Slice giao vận, không tạo sớm bằng suy đoán |

## 7. Ranh giới cần chốt để tránh trùng chức năng

### Trung tâm báo cáo ngày

Dùng cho 98 workbook Portal: upload file, nhận diện contract, validation, preview lỗi, xác nhận, phê duyệt, khóa, canonical release và readiness gửi Portal.

### Cổng nhập liệu bổ sung

Chỉ dùng cho dữ liệu chưa có workbook/API nguồn: form nhập tay theo schema được duyệt, file ngoại lệ hoặc bổ sung có lý do. Không được trở thành một màn upload Excel thứ hai.

### Kho & luân chuyển

Quản lý giao dịch và đối soát chi tiết của domain kho. `Báo cáo & đối soát` chỉ tổng hợp liên domain, báo cáo kỳ, drill-down và xuất bản; không nhân đôi bảng đối soát kho.

## 8. Backlog đề xuất theo thứ tự

| Ưu tiên | Lát cắt | Lý do |
|---:|---|---|
| 1 | Master kho/vị trí, chỉ tiêu và mapping đơn vị-phân xưởng | Là nền cho các module còn lại |
| 2 | Thực hiện sản xuất ngày/ca | Tạo KPI sản lượng thật cho Dashboard |
| 3 | Kế hoạch và so sánh kế hoạch-thực hiện | Hoàn thiện KPI tiến độ từng demo |
| 4 | KCS/chất lượng | Bổ sung độ tro, độ ẩm, nghiệm thu và cảnh báo chất lượng |
| 5 | Sổ kho và giao dịch nhập/xuất/điều chuyển/kiểm kê | Hoàn thiện phần còn thiếu của Kho |
| 6 | Cảnh báo/rule engine/SLA | Thay cảnh báo demo bằng sự kiện thật |
| 7 | Tiêu thụ/giao vận | Lệnh giao, cân, phương tiện, bàn giao |
| 8 | Báo cáo liên domain và export | Chỉ thực hiện sau khi domain nguồn đủ dữ liệu |
| 9 | Cổng nhập liệu bổ sung | Xây schema/form theo use case ngoại lệ đã xác nhận |
| 10 | Portal Gateway | Thực hiện khi có API contract; dùng cùng canonical release |

## 9. Kết luận nghiệm thu

- Không cần khôi phục hoặc copy nguyên trạng demo cũ vào production.
- Giữ 10 module cũ làm target functional scope; giữ `Trung tâm báo cáo ngày` là module mới bắt buộc.
- Giữ nguyên Core shell và Quản trị hệ thống hiện tại; không dùng lại các trang quản trị tĩnh trong demo.
- Công việc tiếp theo không phải chỉnh menu mà là thay lần lượt 7 prototype bằng domain/API/canonical data thật theo backlog trên.
- Không bật Portal API trước khi có contract; Portal Gateway vẫn là đầu ra bắt buộc của MES hoàn chỉnh.
