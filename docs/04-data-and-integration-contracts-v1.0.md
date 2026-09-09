# MES — Data and Integration Contracts v1.0

- Trạng thái: **Đã chốt nguyên tắc tích hợp; contract chi tiết hoàn thiện theo từng biểu mẫu**

## Nguồn dữ liệu giai đoạn đầu

- `ExcelPortalAdapter`: nguồn chính thức từ 98 biểu mẫu Portal.
- `ManualEntryAdapter`: nhập và bổ sung trực tiếp trên MES.
- PostgreSQL MES: nguồn dữ liệu chuẩn sau validation/xác nhận/phê duyệt.

Data Lake, SCADA, cân điện tử, KCS/LIMS và ERP là adapter tương lai, không phải dependency runtime của giai đoạn đầu.

## Hợp đồng chung

Mọi adapter phải chuyển dữ liệu về cùng canonical model và dùng chung validation, idempotency, approval, audit, lineage và reconciliation. Contract phải ghi rõ owner, version, authentication, schema, idempotency key, timeout, retry/backoff, rate limit, error model, correlation ID, audit/outbox event, reconciliation, SLA và quy trình thay đổi.

API dự án dùng `/api/v1`, pagination phía server và `application/problem+json` theo Core standard. Chi tiết baseline nằm tại `13-mes-solution-blueprint-v1.0.md`.

## Portal

Portal Gateway là đầu ra chính thức thứ hai của MES, song song với Dashboard/Báo cáo nội bộ. Phần delivery đang pending do chưa có hợp đồng API, không phải bị loại khỏi kiến trúc sản phẩm. Runtime mặc định `MES_PORTAL_MODE=DISABLED`: không gọi HTTP/SFTP/RPA, không tạo lần gửi hoặc biên nhận giả và không đổi trạng thái kỳ báo cáo sang đã nộp. Chỉ xây mapping/adapter và bật tích hợp sau khi Portal cung cấp hợp đồng cụ thể về schema, xác thực, idempotency, retry, giới hạn tải, môi trường test/production và quy tắc biên nhận.

Portal trong tương lai phải đọc cùng bản phát hành canonical `PUBLISHED` mà Dashboard/Báo cáo nội bộ sử dụng; không tạo một luồng chuẩn hóa dữ liệu song song.

Tùy contract Portal, adapter có thể gửi file gốc đã kiểm tra, file được hệ thống sinh lại theo template hoặc payload JSON canonical. Cách đóng gói được cấu hình theo `portal_report_mapping`; nguồn sự thật vẫn là release `PUBLISHED` và mọi lần gửi phải có idempotency key, checksum, trạng thái, retry và biên nhận.

## Master data và đối soát nội bộ

- Product/partner được ghi nhận từ nguồn vào ở trạng thái `UNVERIFIED`; chỉ Data Steward có quyền `MES_MASTER_DATA:APPROVE` mới được xác nhận `RESOLVED`.
- Mỗi thay đổi giữ version để chống ghi đè đồng thời, người/thời điểm xác nhận và audit log.
- Baseline đối soát tồn kho dùng công thức `tồn cuối D-1 + nhập D - xuất D = tồn cuối báo cáo D` trên canonical metric đã phát hành.
- Một ngày chỉ được kết luận `BALANCED` khi đủ năm contract bắt buộc, các master liên quan đã xác nhận và chênh lệch đúng bằng 0. Không áp dụng ngưỡng sai số ngầm định.
- Chi tiết API và bằng chứng UAT nằm tại `17-mes-uat-master-data-reconciliation-dashboard-v1.0.md`.
