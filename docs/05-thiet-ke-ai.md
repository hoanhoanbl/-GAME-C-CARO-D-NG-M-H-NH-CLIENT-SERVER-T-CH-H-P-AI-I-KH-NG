# Thiết kế AI (mục 11.5)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

## 11.5 Gợi ý thiết kế AI (FE-16, FE-17)

- **Ô ứng viên:** chỉ xét ô trống cách quân đã đánh ≤ 2 ô. Bàn trống thì đánh ô giữa (7,7).
- **Hàm đánh giá:** quét 4 hướng, đếm chuỗi quân cùng màu và số đầu mở:

| Mẫu | Điểm (tham khảo) |
|---|---|
| 5 quân | 1 000 000 |
| 4 quân, hai đầu mở | 100 000 |
| 4 quân, một đầu mở | 10 000 |
| 3 quân, hai đầu mở | 10 000 |
| 3 quân, một đầu mở | 1 000 |
| 2 quân, hai đầu mở | 500 |
| 2 quân, một đầu mở | 50 |

  Điểm thế cờ = điểm của bot − (hệ số 1,2 × điểm của người). Hệ số > 1 để bot ưu tiên phòng thủ.
- **Độ khó:** mức 1 = độ sâu 1 + chọn ngẫu nhiên trong top 3; mức 2 = độ sâu 2; mức 3 = độ sâu 3–4 (chỉnh theo kết quả đo FE-25).
- **Trước khi tìm kiếm:** nếu có nước thắng ngay thì đánh; nếu đối thủ có nước thắng ngay thì chặn.

