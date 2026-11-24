# Định nghĩa hoàn thành và kiểm thử (mục 15)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

## 15. Định nghĩa hoàn thành và kiểm thử

**Một việc được coi là xong khi:** chạy được, có test (với lớp logic), đã merge vào `dev`, `dev` vẫn build, và người kia đã chạy thử ít nhất một lần.

### Kịch bản kiểm thử hệ thống (FE-24)

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| T-01 | Kết nối sai IP / server tắt | Báo lỗi, không treo |
| T-02a | Đăng ký username đã tồn tại | `USERNAME_TAKEN` |
| T-02b | Đăng nhập sai mật khẩu / username không tồn tại | Cùng một lỗi `INVALID_CREDENTIALS` |
| T-02c | Cùng một tài khoản đăng nhập trên hai máy | Máy thứ hai nhận `ALREADY_LOGGED_IN` |
| T-02d | Gửi `CREATE_ROOM` khi chưa đăng nhập (qua telnet) | `NOT_LOGGED_IN` |
| T-02e | Mất kết nối rồi đăng nhập lại cùng tài khoản | Đăng nhập được (phiên cũ đã được dọn) |
| T-02f | Kiểm tra bảng `players` và log server | Không thấy mật khẩu thô |
| T-02g | Khách bấm "Chơi với máy" không đăng nhập, chơi hết ván | Chơi được; hết ván về màn hình đăng nhập; bảng `matches` không có dòng mới |
| T-02h | Khách gửi `CREATE_ROOM` / `CHAT` (qua telnet) | `NOT_LOGGED_IN` |
| T-02i | Người đã đăng nhập chơi với máy từ sảnh | Chơi được; hết ván về sảnh; bảng `matches` có dòng mới với `vs_bot = TRUE` |
| T-03 | Tạo phòng, client khác thấy phòng mới | `ROOM_LIST` cập nhật tức thì |
| T-04 | Hai người vào cùng một phòng cùng lúc | Chỉ một người vào được, người kia nhận `ROOM_FULL` |
| T-05 | Đánh khi không đến lượt | `NOT_YOUR_TURN`, bàn cờ không đổi |
| T-06 | Đánh vào ô đã có quân / ngoài bàn | `CELL_OCCUPIED` / `OUT_OF_BOARD` |
| T-07 | Thắng ngang, dọc, chéo xuôi, chéo ngược, sát biên | `GAME_OVER` đúng người thắng, tô đúng 5 ô |
| T-08 | Bàn đầy không ai thắng | `GAME_OVER\|DRAW\|FULL` |
| T-09 | Đầu hàng | Đối thủ thắng, lý do `SURRENDER` |
| T-10 | Tắt client giữa ván | Đối thủ thắng, lý do `LEFT`; phòng bị xóa |
| T-11 | Rút dây mạng một máy giữa ván | Như T-10 sau tối đa 15 giây (PING timeout) |
| T-12 | Chơi lại | Ván mới, đổi bên X/O |
| T-13 | Bot thắng ngay khi có 4 quân mở | Bot đánh nước thắng |
| T-14 | Người có 4 quân, bot phải chặn | Bot chặn |
| T-15 | 10 client, 5 ván cùng lúc, 1 ván với bot mức 3 | Các ván không chậm lẫn nhau |
| T-16 | Gửi chuỗi rác qua telnet | `ERROR\|BAD_MESSAGE`, server vẫn chạy |
| T-17 | Tắt MySQL giữa chừng | Ván đang chơi vẫn chạy, đăng nhập mới nhận `DB_UNAVAILABLE`, khách vẫn chơi với máy được, log báo lỗi DB |
| T-18 | Chat có tiếng Việt và ký tự `\|` | Hiển thị đúng (`\|` thành `/`) |

