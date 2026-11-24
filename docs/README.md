# TÀI LIỆU DỰ ÁN: GAME CỜ CARO CLIENT-SERVER TÍCH HỢP AI ĐỐI KHÁNG

> Nhóm: **Nguyễn Phan Như Hoàng** (24IT079) · **Nguyễn Phúc Phi** (24IT200) · GVHD: ThS. Nguyễn Ngọc Huyền Trân
> Thư mục `docs/` là **nguồn sự thật duy nhất** của nhóm. Muốn đổi quyết định ở mục 3, 9, 10 thì sửa tài liệu trước, báo người kia, rồi mới sửa code.

## Mục lục

Số mục được giữ nguyên như bản gốc, nên các câu "xem mục 9", "theo bảng mục 8" trong tài liệu và trong commit cũ vẫn đúng. Tra file ở bảng dưới.

| File | Mục | Nội dung | Đọc khi |
|---|---|---|---|
| [01-tong-quan.md](01-tong-quan.md) | 1–6 | Tổng quan, phạm vi, **quyết định D1–D13**, actor, use case, danh sách FE, yêu cầu phi chức năng | Bắt đầu dự án, viết báo cáo |
| [02-kien-truc.md](02-kien-truc.md) | 7–8 | Client-Server + MVC, 7 quy tắc MVC, **cây thư mục và chủ sở hữu từng file** | Trước khi tạo hoặc sửa một file |
| [03-giao-thuc.md](03-giao-thuc.md) | 9–11 | **Hợp đồng interface**, giao thức TCP/UDP, các luồng xử lý, trạng thái phòng | Khi viết code gọi sang phần của người kia |
| [04-co-so-du-lieu.md](04-co-so-du-lieu.md) | 12 | Bảng `players`, `matches` | Làm DAO, `schema.sql` |
| [05-thiet-ke-ai.md](05-thiet-ke-ai.md) | 11.5 | Hàm đánh giá, độ khó, thắng ngay/chặn ngay | Làm bot (FE-16 đến FE-19) |
| [06-ke-hoach.md](06-ke-hoach.md) | 13, 16, 17 | Phân công GĐ0–GĐ4, rủi ro, việc còn mở | Hằng ngày: xem việc, tick việc |
| [07-quy-trinh-git.md](07-quy-trinh-git.md) | 14 | Nhánh `dev`/`main`, commit, file hợp đồng | Trước khi push hoặc merge |
| [08-kiem-thu.md](08-kiem-thu.md) | 15 | Định nghĩa hoàn thành, kịch bản T-01 đến T-18 | Giai đoạn kiểm thử |

## Quy ước

- File **hợp đồng** là [03-giao-thuc.md](03-giao-thuc.md) (mục 9–10). Sửa nó thì kéo theo `Command.java`: nhắn người kia trước, xem [07-quy-trinh-git.md](07-quy-trinh-git.md).
- `docs/test-report.md` (Phi) sẽ được tạo ở giai đoạn kiểm thử; kịch bản gốc nằm ở [08-kiem-thu.md](08-kiem-thu.md).
- Thêm mục mới thì đặt vào file cùng chủ đề và cập nhật bảng trên, không tạo lại một file tổng hợp.
