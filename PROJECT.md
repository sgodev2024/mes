# MES Project Foundation

- Project key: `MES`
- Repository: `git@github.com:sgodev2024/mes.git`
- Core upstream: `git@github.com:sgodev2024/java-core.git`
- Trạng thái: Đã khởi tạo full source từ Core baseline; chưa chốt BA nghiệp vụ
- Core baseline: `core-v1.1.1-project-baseline` (`afd9cc18a150`)

Repository này là source độc lập để phát triển và bàn giao dự án MES. Mã nghiệp vụ chỉ được thêm sau khi BA, business rules, dữ liệu, tích hợp và acceptance criteria được duyệt. Không phát triển nghiệp vụ MES trong repository Java Core.

## Điều kiện bắt đầu phát triển nghiệp vụ

1. Chốt BA và phạm vi phát hành đầu tiên.
2. Chốt domain modules, vai trò, permission và Navigation Registry contributions.
3. Chốt mô hình dữ liệu, tích hợp và yêu cầu phi chức năng.
4. Tạo backlog có acceptance criteria và test evidence.
5. Giữ `origin` là MES; chỉ nâng Core qua quy trình compatibility riêng từ `upstream`.

