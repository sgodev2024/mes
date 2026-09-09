# MES – Danh mục 98 biểu mẫu và từ điển Data Contract v1.0

- Ngày kiểm kê kỹ thuật: 08/09/2026
- Phạm vi: 98 workbook đã giải nén từ bộ “mẫu báo cáo BI TMK”
- Trạng thái: baseline kỹ thuật; chưa thay thế phê duyệt nghiệp vụ của chủ biểu mẫu/BGĐ
- Liên quan: Slice 0 – Data Contract và Pilot; Slice 1 – Trung tâm báo cáo ngày

## 1. Kết luận kiểm kê

Đã kiểm kê đủ **98/98 file Excel** thuộc 6 nhóm đơn vị nguồn. Tên đơn vị, chu kỳ và mức ưu tiên trong bảng dưới đây được suy luận từ cây thư mục/tên file; **hạn nộp, người lập, người duyệt và quy tắc chốt số liệu chưa có bằng chứng trong workbook**, vì vậy được giữ ở trạng thái “Cần xác nhận”, không tự giả định.

Sáu biểu mẫu P0 đã được đối chiếu trực tiếp tên file, sheet `Template_Import`, vùng tiêu đề và cột dữ liệu. Đây là phạm vi parser đang chạy. Hai biểu mẫu ngày còn lại chỉ được xếp P1, chưa được parser production nhận.

## 2. Ma trận danh mục biểu mẫu

| STT | Đường dẫn biểu mẫu | Đơn vị nguồn (suy luận) | Chu kỳ | Hạn nộp | Người duyệt | Nguồn hiện tại | Ưu tiên |
|---:|---|---|---|---|---|---|---|
| 1 | `1 Ban KCL\cong nghe thong tin\4100_Chi phí ứng dụng CNTT và CĐS.xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 2 | `1 Ban KCL\cong nghe thong tin\4100_PhieuThuThapNhuCau_CNTT (1).xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 3 | `1 Ban KCL\khoa hoc va cong nghe\4100_DanhMuc_NhiemVu.xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 4 | `1 Ban KCL\khoa hoc va cong nghe\4100_KH_KHCN.xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 5 | `1 Ban KCL\khoa hoc va cong nghe\4100_NoiDung_NhiemVu_V1.xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 6 | `1 Ban KCL\khoa hoc va cong nghe\4100_TaiSan_NhiemVu.xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 7 | `1 Ban KCL\khoa hoc va cong nghe\4100_ThongTinChung_NhiemVu.xlsx` | Ban KCL | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 8 | `2 Ban Ke hoach\1 Du kien\4100_KH_DK_KD8_to_KD19.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 9 | `2 Ban Ke hoach\1 Du kien\4100_KH_DK_Pha_tron_NK.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 10 | `2 Ban Ke hoach\1 Du kien\4100_KH_DK_SXTT_KH02_KH04_nhap.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 11 | `2 Ban Ke hoach\1 Du kien\4100_KH_DK_SXTT_KH02_KH04_xuat.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 12 | `2 Ban Ke hoach\1 Du kien\4100_KH_DK_TV_PTK.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 13 | `2 Ban Ke hoach\2 ke hoach nam\4100_KH_KHN_KD8 to KD19.xlsx` | Ban Kế hoạch | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 14 | `2 Ban Ke hoach\2 ke hoach nam\4100_KH_KHN_Pha_tron_NK.xlsx` | Ban Kế hoạch | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 15 | `2 Ban Ke hoach\2 ke hoach nam\4100_KH_KHN_SXTT_KH02_KH04_nhap.xlsx` | Ban Kế hoạch | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 16 | `2 Ban Ke hoach\2 ke hoach nam\4100_KH_KHN_SXTT_KH02_KH04_xuat.xlsx` | Ban Kế hoạch | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 17 | `2 Ban Ke hoach\2 ke hoach nam\4100_KH_KHN_TV_PTK.xlsx` | Ban Kế hoạch | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 18 | `2 Ban Ke hoach\3 KE HOACH THANG\4100_KH_KHT_KD8 to KD19.xlsx` | Ban Kế hoạch | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 19 | `2 Ban Ke hoach\3 KE HOACH THANG\4100_KH_KHT_Pha_tron_NK.xlsx` | Ban Kế hoạch | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 20 | `2 Ban Ke hoach\3 KE HOACH THANG\4100_KH_KHT_SXTT_KH02_KH04_nhap.xlsx` | Ban Kế hoạch | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 21 | `2 Ban Ke hoach\3 KE HOACH THANG\4100_KH_KHT_SXTT_KH02_KH04_xuat.xlsx` | Ban Kế hoạch | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 22 | `2 Ban Ke hoach\3 KE HOACH THANG\4100_KH_KHT_TV_PTK.xlsx` | Ban Kế hoạch | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 23 | `2 Ban Ke hoach\4 Thuc te\4100_KH_DT_NoiBo_Khac.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 24 | `2 Ban Ke hoach\4 Thuc te\4100_KH_DT_NoiBo_KhoangSan.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 25 | `2 Ban Ke hoach\4 Thuc te\4100_KH_DT_NoiBo_Than.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 26 | `2 Ban Ke hoach\4 Thuc te\4100_KH_Thong ke lao dong.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 27 | `2 Ban Ke hoach\4 Thuc te\4100_KH_Thucte_Pha_tron_NK.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 28 | `2 Ban Ke hoach\4 Thuc te\4100_KH_TT_KD8 to KD19.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 29 | `2 Ban Ke hoach\4 Thuc te\4100_KH_TT_SXTT_KH02_KH04_nhap.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 30 | `2 Ban Ke hoach\4 Thuc te\4100_KH_TT_SXTT_KH02_KH04_xuat.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 31 | `2 Ban Ke hoach\4 Thuc te\4100_KH_TT_TV_PTK.xlsx` | Ban Kế hoạch | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 32 | `3 Ban dau tu\DT - Ke hoach TKV giao ban dau\4100_KeHoach_TKV_giao.xlsx` | Ban Đầu tư | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 33 | `3 Ban dau tu\DT - thuc hien, giai ngan\4100_Thuchien_giaingan.xlsx` | Ban Đầu tư | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 34 | `4 Ban san xuat - tieu thu than\1 Du kien TH thang\4100_DK_SX_Thang_Giaotuyen_00.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 35 | `4 Ban san xuat - tieu thu than\1 Du kien TH thang\4100_DK_SX_Thang_Metlo_Datboc_00.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 36 | `4 Ban san xuat - tieu thu than\1 Du kien TH thang\4100_DK_SX_Thang_than_sach_00.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 37 | `4 Ban san xuat - tieu thu than\1 Du kien TH thang\4100_DK_SX_Thang_thanNK_00.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 38 | `4 Ban san xuat - tieu thu than\1 Du kien TH thang\4100_DK_TieuThu_Thang_00.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 39 | `4 Ban san xuat - tieu thu than\2 Ke hoach nam\4100_KH_SX_Nam_Giaotuyen_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 40 | `4 Ban san xuat - tieu thu than\2 Ke hoach nam\4100_KH_SX_Nam_Metlo_Datboc_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 41 | `4 Ban san xuat - tieu thu than\2 Ke hoach nam\4100_KH_SX_Nam_than_sach_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 42 | `4 Ban san xuat - tieu thu than\2 Ke hoach nam\4100_KH_SX_Nam_thanNK_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 43 | `4 Ban san xuat - tieu thu than\2 Ke hoach nam\4100_KH_TT_Nam_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 44 | `4 Ban san xuat - tieu thu than\2 Ke hoach nam\4100_KH_TT_Nam_PTCB_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 45 | `4 Ban san xuat - tieu thu than\3 Ke hoach quy\4100_KH_SX_Nam_Giaotuyen_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 46 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_Giaotuyen_02.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 47 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_Giaotuyen_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 48 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_Metlo_Datboc_02.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 49 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_Metlo_Datboc_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 50 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_than_sach_02.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 51 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_than_sach_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 52 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_thanNK_02.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 53 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_SX_Thang_thanNK_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 54 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_TT_Thang_PTCB_02.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 55 | `4 Ban san xuat - tieu thu than\4 Ke hoach thang\4100_KH_TT_Thang_PTCB_03.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 56 | `4 Ban san xuat - tieu thu than\5 Ke hoach dieu hanh\4100_KH_SX_Thang_Giaotuyen_04.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 57 | `4 Ban san xuat - tieu thu than\5 Ke hoach dieu hanh\4100_KH_SX_Thang_Metlo_Datboc_04.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 58 | `4 Ban san xuat - tieu thu than\5 Ke hoach dieu hanh\4100_KH_SX_Thang_than_sach_04.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 59 | `4 Ban san xuat - tieu thu than\5 Ke hoach dieu hanh\4100_KH_SX_Thang_thanNK_04.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 60 | `4 Ban san xuat - tieu thu than\5 Ke hoach dieu hanh\4100_KH_TT_Thang_04.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 61 | `4 Ban san xuat - tieu thu than\5 Ke hoach dieu hanh\4100_KH_TT_Thang_PTCB_04.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 62 | `4 Ban san xuat - tieu thu than\6 Thục hien quy\4100_Thucte_SX_Quý_Giaotuyen_05.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 63 | `4 Ban san xuat - tieu thu than\6 Thục hien quy\4100_Thucte_SX_Quý_Metlo_datboc_05.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 64 | `4 Ban san xuat - tieu thu than\6 Thục hien quy\4100_Thucte_SX_Quý_than_sach_05.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 65 | `4 Ban san xuat - tieu thu than\6 Thục hien quy\4100_Thucte_SX_Quý_thanNK_05.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 66 | `4 Ban san xuat - tieu thu than\6 Thục hien quy\4100_Thucte_Tieuthu_PTCB_quy_05.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 67 | `4 Ban san xuat - tieu thu than\6 Thục hien quy\4100_Thucte_Tieuthu_quy_05.xlsx` | Ban Sản xuất – Tiêu thụ than | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 68 | `4 Ban san xuat - tieu thu than\7 Thuc hien thang\4100_Thucte_SX_Thang_Giaotuyen_01.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 69 | `4 Ban san xuat - tieu thu than\7 Thuc hien thang\4100_Thucte_SX_Thang_Metlo_Datboc_01.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 70 | `4 Ban san xuat - tieu thu than\7 Thuc hien thang\4100_Thucte_SX_Thang_than_sach_01.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 71 | `4 Ban san xuat - tieu thu than\7 Thuc hien thang\4100_Thucte_SX_Thang_thanNK_01.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 72 | `4 Ban san xuat - tieu thu than\7 Thuc hien thang\4100_Thucte_TT_Thang_01.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 73 | `4 Ban san xuat - tieu thu than\7 Thuc hien thang\4100_TT_Theodoi_lich_tau.xlsx` | Ban Sản xuất – Tiêu thụ than | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 74 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_Ngay_PTCB_01.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P0 – Pilot |
| 75 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_NhapKho_Ngay_Metlo_Datboc_Dathai.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P1 – Ngày |
| 76 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_NhapKho_Ngay_than_sach.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P0 – Pilot |
| 77 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_NhapKho_Ngay_thanNK.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P0 – Pilot |
| 78 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_Tonkho_Ngay.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P0 – Pilot |
| 79 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_XuatKho_Ngay_Dathai_Camda.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P1 – Ngày |
| 80 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_XuatKho_Ngay_than_sach.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P0 – Pilot |
| 81 | `4 Ban san xuat - tieu thu than\8 Thuc hien ngay\4100_TT_XuatKho_Ngay_thanNK.xlsx` | Ban Sản xuất – Tiêu thụ than | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P0 – Pilot |
| 82 | `5 Ban Vat tu thuong mai\4100_DK_KHSX_TTVT_TN.xlsx` | Ban Vật tư thương mại | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 83 | `5 Ban Vat tu thuong mai\4100_DukienNhucau_Nam.xlsx` | Ban Vật tư thương mại | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 84 | `5 Ban Vat tu thuong mai\4100_LuanChuyenVatTu_Quy.xlsx` | Ban Vật tư thương mại | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 85 | `5 Ban Vat tu thuong mai\4100_SX_TieuThuVTTN.xlsx` | Ban Vật tư thương mại | Theo phát sinh – cần xác nhận | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P3 – Chuẩn hóa sau |
| 86 | `5 Ban Vat tu thuong mai\4100_THMS_Quy.xlsx` | Ban Vật tư thương mại | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 87 | `5 Ban Vat tu thuong mai\4100_THMSVTTN_Quy.xlsx` | Ban Vật tư thương mại | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 88 | `5 Ban Vat tu thuong mai\4100_THTH_PheLieu_Quy.xlsx` | Ban Vật tư thương mại | Quý (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 89 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_1. Thực hiện các chỉ tiêu chủ yếu.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 90 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_2.Chi tiết mua than.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 91 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_3.Tiêu thụ than.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 92 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_4. Nhập, xuất, tồn - Than.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 93 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_5. Chi tiết mua khoáng sản.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 94 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_6. Tiêu thụ khoáng sản.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 95 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_7. Nhập, xuất, tồn khoáng sản.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 96 | `6 Ban ke toan - tai chinh\Bao cao thang\4100_8. Nhập, xuất, tồn - VLN,CK,VLXD,KHAC.xlsx` | Ban Kế toán – Tài chính | Tháng (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |
| 97 | `6 Ban ke toan - tai chinh\BAO CAO TON KHO\4100_1. Báo cáo nhanh tồn kho ngày.xlsx` | Ban Kế toán – Tài chính | Ngày (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P1 – Ngày |
| 98 | `6 Ban ke toan - tai chinh\NHAP KE HOACH NAM\4100_1. Kế hoạch năm các chỉ tiêu chủ yếu.xlsx` | Ban Kế toán – Tài chính | Năm (suy luận) | Cần xác nhận | Cần xác nhận | Excel nộp Portal | P2 – Kỳ |

## 3. Từ điển hậu tố/tập biểu mẫu

Bảng dưới là kết quả đối chiếu tên file và thư mục, chưa phải quy ước nghiệp vụ được ban hành:

| Hậu tố/nhóm | Nghĩa kỹ thuật suy luận | Số file quan sát | Trạng thái |
|---|---|---:|---|
| `_00` | Dự kiến thực hiện tháng | 5 | Cần chủ biểu mẫu xác nhận |
| `_01` | Thực tế tháng/ngày | 6 nhóm chính | Cần xác nhận phạm vi theo từng mẫu |
| `_02` | Kế hoạch tháng | 5 | Cần chủ biểu mẫu xác nhận |
| `_03` | Kế hoạch năm/phiên bản kế hoạch | 12 | Có file trùng tên ở thư mục quý; cần làm sạch |
| `_04` | Kế hoạch điều hành tháng | 6 | Cần chủ biểu mẫu xác nhận |
| `_05` | Thực hiện quý | 6 | Cần chủ biểu mẫu xác nhận |

Nhóm mã tài chính quan sát được: `T_TCKT_01` báo cáo nhanh tồn kho ngày; `T_TCKT_02` kế hoạch năm chỉ tiêu chủ yếu; `T_TCKT_03` thực hiện/ước thực hiện chỉ tiêu năm; `T_TCKT_06` nhập-xuất-tồn than; `T_TCKT_07` nhập-xuất-tồn khoáng sản; `T_TCKT_08` nhập-xuất-tồn VLN/CK/VLXD/khác; `T_TCKT_09` tiêu thụ than; `T_TCKT_10` chi tiết mua than; `T_TCKT_11` chi tiết mua khoáng sản; `T_TCKT_12` tiêu thụ khoáng sản. Mã còn thiếu trong chuỗi không được tự suy diễn.

## 4. Data Contract P0 đã hiện thực

| Mã template | Trường chính được kiểm tra | Quy tắc hiện có |
|---|---|---|
| `MES-DAY-PRODUCTION-PTCB` | Mã/tên sản phẩm, ĐVT, xuất xứ, nhập PTCB, bán lại TKV, tồn kho, tên loại, điểm PTCB | Bắt buộc định danh sản phẩm/ĐVT; số không âm; ít nhất một trường số lượng |
| `MES-DAY-RAW-COAL-RECEIPT` | NCC, sản phẩm, ĐVT, các nguồn nhập, PT khai thác, xuất xứ, chất lượng, tàu | Số lượng/chất lượng đúng kiểu và miền; dòng nhập mua phải có NCC |
| `MES-DAY-CLEAN-COAL-RECEIPT` | NCC, sản phẩm, ĐVT, mua ngoài/trong TKV, nhập chế biến, xuất xứ, tàu | Số không âm; ít nhất một nguồn nhập; dòng mua phải có NCC |
| `MES-DAY-STOCK` | Mã/tên sản phẩm, ĐVT, tồn cuối ngày, đi đường | Tồn cuối ngày bắt buộc; các số không âm |
| `MES-DAY-CLEAN-COAL-ISSUE` | Khách hàng, sản phẩm, ĐVT, xuất bán/chế biến, tàu, loại hình | Ít nhất một lượng xuất; dòng bán phải có khách hàng |
| `MES-DAY-RAW-COAL-ISSUE` | Khách hàng, sản phẩm, ĐVT, xuất bán/giao tuyển/chế biến, tàu, loại hình | Ít nhất một lượng xuất; dòng bán phải có khách hàng |

Mỗi contract có phiên bản, hash SHA-256, ngày hiệu lực và trạng thái vòng đời. Contract đã có dữ liệu không được sửa tại chỗ; thay đổi phải tạo phiên bản mới.

## 5. KPI vận hành khả dụng trong Slice 1

Các chỉ số hiện tại chỉ phản ánh **quy trình báo cáo**, gồm: tổng mẫu phải nộp, chưa nộp, đang xử lý, lỗi, chờ xác nhận, chờ phê duyệt, đã duyệt, đã khóa, đã ghi nhận nộp Portal và quá hạn. Đây **không phải KPI sản xuất/điều hành BGĐ**. KPI sản xuất chỉ được triển khai sau khi chủ dữ liệu chốt công thức, đơn vị tính, chiều phân tích và ngưỡng cảnh báo.

## 6. Điểm cần nghiệp vụ xác nhận trước khi mở rộng P1/P2

1. Chủ sở hữu, người lập, người duyệt và giờ hạn nộp của từng biểu mẫu.
2. Ý nghĩa chính thức của hậu tố `_00` đến `_05`; xử lý file trùng tên/sai thư mục.
3. Khóa nghiệp vụ và danh mục chuẩn: sản phẩm, đơn vị tính, nhà cung cấp, khách hàng, kho, phân xưởng.
4. Quy tắc cộng dồn/ngày-tháng-quý-năm và quy tắc đối soát chéo.
5. Công thức KPI BGĐ, ngưỡng cảnh báo và nguồn số liệu có thẩm quyền.
6. Quy trình mở khóa/chỉnh sửa số liệu đã khóa và nguyên tắc bốn mắt.

## 7. Tiêu chí chuyển trạng thái tài liệu

- “Baseline kỹ thuật”: đủ inventory, parser và validation kỹ thuật có kiểm thử.
- “Đã duyệt nghiệp vụ”: có chữ ký/xác nhận của owner cho deadline, approver, field mapping và công thức.
- “Sẵn sàng production”: hoàn tất UAT file thật, kiểm thử phân quyền bốn mắt, antivirus adapter thật, backup/restore và runbook vận hành.

