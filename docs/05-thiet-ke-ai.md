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

  - **Đoạn chết:** chỉ chấm quân nằm trong đoạn (dãy ô gồm quân cùng màu và ô trống, giới hạn bởi quân đối thủ hoặc biên) dài từ 5 ô trở lên. Ví dụ `O _ X X _ O` được 0 điểm.
  - **Thế gãy một ô:** hai chuỗi cách nhau đúng một ô trống, mỗi chuỗi dưới 4 quân, được gộp thành một thế. Tổng từ 4 quân trở lên (`XX_XX`, `X_XXX`, `XX_XXX`) chấm như bốn một đầu, vì lấp ô trống là thắng. Tổng 3 quân (`X_XX`) chấm như ba, mở hay một đầu tùy hai đầu ngoài. Gặp `X_X_X` thì gộp tham lam từ trái sang.
  - **Thế đôi:** đe dọa gồm bốn (liền hoặc gãy) và ba mở. Một bên có từ 2 đe dọa trở lên mà chưa có bốn mở thì được cộng thêm 100 000.

  Điểm thế cờ = điểm của bot − (hệ số 1,2 × điểm của người). Hệ số > 1 để bot ưu tiên phòng thủ. Hàm đánh giá biết **bên tới lượt**, các luật xét theo thứ tự:
  1. Bên nào đã có 5 quân → ±1 000 000 cho bên đó.
  2. Bên tới lượt có bốn → bên đó thắng ở nước kế: ±1 000 000.
  3. Bên còn lại có bốn mở hoặc từ hai bốn → bên tới lượt chặn không xuể: ±1 000 000.
  4. Bên tới lượt có ba mở và bên kia không có bốn → cộng 100 000 cho bên tới lượt.
  5. Còn lại dùng công thức trên.
- **Độ khó:** mức 1 = độ sâu 1 + chọn ngẫu nhiên trong top 3; mức 2 = độ sâu 2; mức 3 = độ sâu 3–4 (chỉnh theo kết quả đo FE-25).
- **Trước khi tìm kiếm:** nếu có nước thắng ngay thì đánh; nếu đối thủ có nước thắng ngay thì chặn.

