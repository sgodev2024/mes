# MES — Mô Hình Vận Hành Hai Đầu Ra Nội Bộ Và Portal V1.0

**Ngày chốt:** 09/09/2026  
**Trạng thái:** Baseline BA/kiến trúc; nhánh nội bộ đang triển khai, nhánh Portal chờ hợp đồng API  
**Phạm vi:** toàn bộ biểu mẫu báo cáo ngày/tháng được MES tiếp nhận

## 1. Quyết định sản phẩm

MES được xây dựng để giải quyết đồng thời hai nhu cầu:

1. tập hợp, kiểm tra, chuẩn hóa và tự động gửi báo cáo sang Portal thay cho thao tác import Excel thủ công;
2. tạo kho dữ liệu quản trị nội bộ, Dashboard và báo cáo phục vụ điều hành trong thời gian Data Lake chưa chuẩn hóa.

Đây là **một hệ thống với hai đầu ra**, không phải hai hệ thống. File chỉ được thu thập và xử lý một lần trong MES. Dashboard và Portal phải sử dụng cùng một phiên bản dữ liệu đã phê duyệt.

## 2. Hiện trạng và kiến trúc đích

### 2.1 Giai đoạn chuyển tiếp

```text
Nhân sự lập Excel
       ├─ import vào MES → kiểm tra/duyệt → canonical → Dashboard nội bộ
       └─ đăng nhập Portal → import thủ công theo quy trình hiện hành
```

Việc nộp Portal thủ công vẫn tiếp tục cho đến khi có API. MES không được tự suy đoán rằng file đã gửi thành công và không tạo biên nhận giả.

### 2.2 Giai đoạn có Portal API

```text
Excel / nhập trực tiếp / API nguồn
                │
                ▼
      MES ingestion + validation
                │
        xác nhận → phê duyệt
                │
                ▼
       Canonical release PUBLISHED
          │                  │
          ▼                  ▼
 Dashboard/Báo cáo     Portal Mapping
      nội bộ                  │
                              ▼
                  Outbox → Job → Portal API
                              │
                              ▼
                    Biên nhận + đối soát
```

Người dùng không phải import lại trên Portal. Portal Gateway tự gửi đúng dữ liệu đã được phê duyệt và khóa trong MES.

## 3. Nguyên tắc bắt buộc

- Một canonical release là nguồn sự thật cho cả hai đầu ra.
- Chỉ release `PUBLISHED` và đạt điều kiện `PORTAL_READY` mới được gửi.
- Portal mapping được version hóa theo biểu mẫu, hiệu lực và môi trường.
- Mỗi lần gửi có idempotency key; retry không được tạo nộp trùng.
- File/payload gửi, checksum, người kích hoạt, thời điểm, số lần thử, HTTP/result code và biên nhận phải truy vết được.
- Secret/token/chứng thư Portal chỉ lấy từ secret store hoặc environment, không lưu trong source hay audit payload.
- Gửi Portal chạy bất đồng bộ qua transactional outbox và background job; không giữ transaction nghiệp vụ trong lúc gọi mạng.
- Dashboard không phụ thuộc trạng thái Portal và Portal không được đọc trực tiếp staging chưa duyệt.

## 4. Trạng thái dữ liệu và delivery

```text
RAW → VALIDATED → PENDING_CONFIRMATION → PENDING_APPROVAL
    → APPROVED → LOCKED → INTERNAL_PUBLISHED
    → PORTAL_READY → QUEUED → SENDING
    → DELIVERED
              └→ RETRY_WAIT → SENDING
              └→ REJECTED / FAILED_FINAL
```

| Trạng thái | Ý nghĩa |
|---|---|
| `INTERNAL_PUBLISHED` | Dữ liệu đã đủ điều kiện dùng nội bộ |
| `PORTAL_READY` | Master, đối soát và mapping Portal đã đạt |
| `QUEUED` | Outbox/job gửi đã được tạo |
| `DELIVERED` | Portal trả biên nhận thành công |
| `REJECTED` | Portal từ chối do dữ liệu/nghiệp vụ; phải sửa bằng revision mới |
| `FAILED_FINAL` | Hết retry do lỗi kỹ thuật; cần vận hành xử lý |

Không gộp trạng thái `INTERNAL_PUBLISHED` và `DELIVERED`. MES vẫn phục vụ nội bộ khi Portal gián đoạn.

## 5. Ba hình thức payload cần được Portal xác nhận

| Hình thức | Khi sử dụng | Nguồn tạo |
|---|---|---|
| Upload file gốc | Portal API nhận đúng workbook hiện hành | File bất biến đã validate + checksum |
| File sinh lại | Portal yêu cầu file theo template/version cụ thể | Canonical release + template renderer |
| JSON/XML payload | Portal cung cấp API dữ liệu có schema | Canonical release + versioned mapping |

Không chốt hình thức trước khi có tài liệu API. Adapter được thiết kế để hỗ trợ cả ba mà không thay đổi canonical model.

## 6. Thông tin bắt buộc phải nhận từ Portal

1. OpenAPI/Swagger hoặc tài liệu endpoint test và production;
2. phương thức xác thực: OAuth2, API key, mTLS, chữ ký số hoặc VPN/IP allowlist;
3. định dạng gửi: multipart file, JSON/XML, encoding, giới hạn dung lượng;
4. mã biểu mẫu, version và quy tắc mapping từng trường;
5. idempotency key hoặc quy tắc chống nộp trùng;
6. xử lý đồng bộ hay bất đồng bộ, callback hoặc API truy vấn trạng thái;
7. timeout, rate limit, retry-after và thời gian bảo trì;
8. danh mục mã lỗi và phân loại lỗi có thể retry/không retry;
9. cấu trúc biên nhận, mã giao dịch và tiêu chí `DELIVERED`;
10. quy tắc hủy, thay thế, nộp lại và điều chỉnh báo cáo đã nhận;
11. dữ liệu kiểm thử, tài khoản sandbox và đầu mối nghiệm thu;
12. yêu cầu lưu trữ, chữ ký, mã hóa và audit.

Thiếu một trong các thông tin ảnh hưởng trực tiếp phải giữ `MES_PORTAL_MODE=DISABLED` hoặc `NOT_CONFIGURED`.

## 7. Backlog Portal Gateway

| Thứ tự | Hạng mục | Kết quả |
|---:|---|---|
| 1 | Portal API discovery | Contract và ma trận gap được ký duyệt |
| 2 | Portal report mapping | Mapping version hóa cho từng Data Contract |
| 3 | Readiness validator | Chặn gửi khi master/đối soát/mapping chưa đạt |
| 4 | Payload builder | Sinh file hoặc payload bất biến kèm checksum |
| 5 | Delivery/outbox/job | Gửi bất đồng bộ, retry idempotent |
| 6 | Receipt processor | Lưu biên nhận và trạng thái Portal thật |
| 7 | Reconciliation | So sánh tổng/chi tiết MES với dữ liệu Portal nhận |
| 8 | Operations UI | Hàng đợi, lần gửi, lỗi, retry và tải biên nhận |
| 9 | Security hardening | Secret, mTLS/OAuth, masking, audit, network policy |
| 10 | Sandbox UAT/cutover | Chạy song song và dừng import thủ công có kiểm soát |

## 8. Điều kiện chuyển từ thủ công sang tự động

- UAT dữ liệu nội bộ đạt tối thiểu ba ngày liên tiếp cho các contract pilot.
- Master data được Data Steward xác nhận.
- Mapping Portal được Data Owner và Portal owner ký duyệt.
- Sandbox Portal chứng minh chống gửi trùng, retry và biên nhận.
- Đối soát MES–Portal đạt trên cả tổng và chi tiết.
- Có dashboard vận hành, cảnh báo job lỗi và runbook khôi phục.
- Chạy song song thủ công/tự động trong khoảng thời gian nghiệp vụ phê duyệt.
- Chỉ dừng import thủ công sau quyết định cutover có rollback plan.

## 9. Kết luận phạm vi hiện tại

Nhánh dữ liệu quản trị nội bộ tiếp tục được hoàn thiện ngay. Nhánh Portal là mục tiêu bắt buộc nhưng đang bị chặn bởi hợp đồng tích hợp bên ngoài. Không viết adapter giả và không bật `API` chỉ dựa trên suy đoán từ workbook. Khi tài liệu Portal được cung cấp, triển khai Slice 5 trên canonical release hiện có mà không phải xây lại MES.
