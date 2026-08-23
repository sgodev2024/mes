# MES — Deployment Runbook Preparation v1.0

- Trạng thái: Chưa triển khai

Trước khi tạo runtime phải chốt môi trường, domain, server path, Compose project name, loopback ports, database/schema/roles, storage, Nginx, DNS/SSL, CORS allowlist, secret delivery, backup/restore, monitoring và rollback. Không dùng chung container, volume, database hoặc secret với Core hay dự án khác.

Release đầu tiên chỉ được thực hiện khi CI đạt, working tree sạch, BA/permission/migration được duyệt và có smoke test qua domain thật.

