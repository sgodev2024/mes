# MES V1 — Quy tắc Branch, Commit và Pull Request

- Mã tài liệu: `MES-ENG-GIT-001`
- Task nguồn: `MES-003`
- Phiên bản: `1.0`
- Áp dụng: toàn bộ nhân sự phát triển repository `mes`
- Nhánh phát hành: `main`

## 1. Nguyên tắc bắt buộc

- Chỉ làm việc và tạo Pull Request trong repository MES; không push mã dự án lên repository Java Core.
- `main` luôn phải build và chạy được. Không commit hoặc push trực tiếp lên `main`.
- Một task MES tương ứng một branch và một Pull Request. Không gộp nhiều task không liên quan.
- Không tự sửa phần Core trong namespace `vn.coreplatform`, shell Core hoặc màn hình Quản trị hệ thống nếu task không cho phép rõ ràng.
- Không đưa mật khẩu, token, private key, certificate, `.env`, database dump hoặc dữ liệu khách hàng thật lên Git.
- Mã do AI hỗ trợ phải được người thực hiện đọc hiểu, kiểm thử và chịu trách nhiệm như mã tự viết.
- Không merge khi pipeline lỗi, còn comment review chưa xử lý hoặc chưa đủ bằng chứng nghiệm thu.

## 2. Checklist trước khi tạo branch

- [ ] Đã đọc nội dung task, dependency và tiêu chí nghiệm thu trong backlog.
- [ ] Task không ở trạng thái `BA-GATE` hoặc đang bị task khác chặn.
- [ ] Đã xác định file/module dự kiến thay đổi và ranh giới Core không được sửa.
- [ ] Không có task khác đang sửa cùng migration hoặc component nền.
- [ ] Working tree hiện tại sạch; thay đổi cá nhân cũ đã commit hoặc cất an toàn.
- [ ] Đã đồng bộ `main` mới nhất bằng fast-forward.

Lệnh khởi tạo chuẩn:

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/MES-<ID>-<mo-ta-ngan>
```

Ví dụ:

```bash
git switch -c feature/MES-002-map-project-structure
git switch -c feature/MES-004-module-descriptor
```

Loại branch được phép:

| Loại | Mục đích | Ví dụ |
|---|---|---|
| `feature/` | Task phát triển hoặc tài liệu backlog | `feature/MES-005-navigation-registry` |
| `fix/` | Sửa lỗi có mã task/issue | `fix/MES-016-empty-api-response` |
| `hotfix/` | Lỗi khẩn cấp production, có incident | `hotfix/INC-012-login-failure` |
| `docs/` | Chỉ sửa tài liệu, không đổi runtime | `docs/MES-002-project-map` |

Tên branch phải dùng chữ thường, không dấu, ngăn cách bằng dấu `-` và phải chứa mã task hoặc incident.

## 3. Checklist trong quá trình làm việc

- [ ] Chỉ thay đổi đúng phạm vi task; phát hiện yêu cầu mới thì tạo task khác.
- [ ] Không sửa migration đã được merge/chạy; tạo migration kế tiếp nếu cần thay đổi.
- [ ] Không hard-code credential, domain, quyền, menu hoặc dữ liệu nghiệp vụ production.
- [ ] Backend vẫn là lớp quyết định permission cuối; việc ẩn nút frontend không thay thế phân quyền.
- [ ] Có validation, loading, empty, error và retry state tương ứng với phạm vi task.
- [ ] Có test tối thiểu cho happy path, dữ liệu không hợp lệ và từ chối quyền nếu có API.
- [ ] Không commit file build, log, IDE, cache, database volume hoặc binary không cần thiết.
- [ ] Tự xem lại thay đổi trước mỗi commit bằng `git diff` và `git status`.

## 4. Quy tắc commit

Định dạng:

```text
<type>(MES-<ID>): <mô tả ngắn bằng động từ>
```

Các `type` được dùng: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `build`, `ci`.

Ví dụ đúng:

```text
docs(MES-002): map Core and MES module boundaries
feat(MES-005): register MES business navigation
test(MES-007): cover module and navigation smoke flow
fix(MES-016): handle empty successful API responses
```

Không chấp nhận:

```text
update code
fix bug
done
MES task
```

Checklist commit:

- [ ] Commit chỉ chứa một thay đổi logic có thể giải thích.
- [ ] Nội dung commit khớp mã task trên branch.
- [ ] Không commit secret hoặc dữ liệu cá nhân/thật.
- [ ] Không dùng `git add .` mà chưa kiểm tra danh sách file.
- [ ] Không force-push lên `main`; không sửa lịch sử branch của người khác.
- [ ] Không tạo commit chỉ để sửa lỗi format có thể gộp hợp lý trước khi review.

## 5. Kiểm tra bắt buộc trước khi mở Pull Request

Chạy những kiểm tra phù hợp với phần đã thay đổi:

```bash
git status
git diff main...HEAD
docker compose config
docker compose up -d --build
docker compose ps
```

Nếu thay backend:

```bash
cd backend
mvn test
```

Nếu thay frontend:

```bash
cd frontend
npm ci
npm run build
```

Người thực hiện phải lưu bằng chứng:

- MES-001: `docker compose ps`, readiness backend, HTTP frontend và kết quả đăng nhập local.
- MES-002: sơ đồ thư mục, bảng phân biệt Core/MES và danh sách vị trí module, migration, API, frontend, test.
- Thay giao diện: ảnh trước/sau và kích thước màn hình kiểm thử.
- Thay API: request/response mẫu, mã lỗi và kiểm tra permission.
- Thay database: tên migration, kết quả chạy database sạch và phương án nâng cấp.

## 6. Checklist Pull Request của người tạo

- [ ] Tiêu đề theo mẫu: `[MES-<ID>] <Kết quả đạt được>`.
- [ ] PR trỏ vào `main` và branch chỉ chứa một task.
- [ ] Mô tả nêu kết quả, phạm vi, file/module ảnh hưởng và phần không làm.
- [ ] Liên kết task/backlog và ghi rõ dependency đã hoàn thành.
- [ ] Liệt kê lệnh test cùng kết quả thực tế; không chỉ ghi “đã test”.
- [ ] Đính kèm ảnh/video/log hoặc API evidence cần thiết.
- [ ] Khai báo rõ có hay không thay đổi API, database, permission, navigation và Core boundary.
- [ ] Không có secret, `.env`, key, dump, dữ liệu thật hoặc generated noise trong diff.
- [ ] Đã tự review toàn bộ diff trên GitHub.
- [ ] Đã cập nhật tài liệu kỹ thuật nếu hành vi hệ thống thay đổi.
- [ ] Ghi rõ rủi ro, giới hạn và việc còn lại; không che giấu lỗi đã biết.
- [ ] Yêu cầu đúng reviewer; nhân sự đào tạo bắt buộc có Tech Lead review.

## 7. Checklist của reviewer

- [ ] Kết quả đáp ứng đúng acceptance criteria, không chỉ “code chạy”.
- [ ] Diff nằm đúng repository/module và không làm thay đổi Core ngoài phạm vi.
- [ ] Tên biến, cấu trúc và luồng lỗi có thể hiểu và bảo trì được.
- [ ] Validation và permission được kiểm tra tại backend.
- [ ] Migration tiến, an toàn và không sửa migration đã áp dụng.
- [ ] Test bao phủ happy path, validation và negative permission theo phạm vi.
- [ ] Không có credential, PII, log nhạy cảm hoặc dependency không rõ lý do.
- [ ] Bằng chứng Docker/build/test/ảnh/API đủ để tái kiểm tra.
- [ ] Code do AI hỗ trợ không chứa đoạn thừa, API giả hoặc hành vi người làm không giải thích được.
- [ ] Tất cả comment bắt buộc đã được xử lý hoặc có phản hồi rõ ràng.

Mức phê duyệt:

- Task E1/E2: tối thiểu một reviewer, nhân sự đào tạo phải có Tech Lead hoặc reviewer được chỉ định.
- Migration, permission, authentication, workflow, transaction hoặc tích hợp: tối thiểu Tech Lead phê duyệt; cần reviewer thứ hai khi ảnh hưởng nhiều module.
- Không cho phép người tạo PR tự merge khi chưa có approval.

## 8. Merge và sau merge

- Dùng **Squash and merge** để một task tạo một commit dễ truy vết trên `main`.
- Squash commit giữ đúng mẫu: `type(MES-ID): mô tả`.
- Chỉ merge khi pipeline xanh, đủ approval và không còn conflict.
- Xóa branch sau khi merge.
- Người thực hiện cập nhật local bằng `git switch main` và `git pull --ff-only origin main`.
- Nếu sau merge phát hiện lỗi, tạo task/issue và branch `fix/`; không sửa trực tiếp trên `main`.

## 9. Điều kiện khóa Pull Request

PR phải bị yêu cầu sửa, không được merge, nếu có một trong các trường hợp:

- Không có mã MES/incident hoặc gộp nhiều task không liên quan.
- Sửa Core, shell Core hoặc Quản trị hệ thống ngoài phạm vi đã duyệt.
- Có secret, dữ liệu thật, file môi trường hoặc dump.
- Pipeline/test/build lỗi hoặc không có bằng chứng nghiệm thu.
- API chỉ ẩn nút frontend nhưng không kiểm tra permission backend.
- Sửa migration đã chạy hoặc thay đổi contract mà không công bố.
- Người thực hiện không giải thích được mã do AI sinh ra.
- PR quá lớn và không thể review an toàn; phải tách lại theo lát cắt kiểm thử được.

