# Quy trình Git (mục 14)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

## 14. Quy trình Git chống conflict

1. **`dev` là nhánh làm việc (mặc định), luôn build được.** Sau GĐ0 không push thẳng lên `dev`. **`main` là nhánh ổn định:** chỉ đẩy từ `dev` sang khi đã chạy ổn (cuối mỗi mốc M2, M3 và lúc nộp), không ai làm việc trực tiếp trên `main`.
2. **Nhánh:** `hoang/fe-09-game-session`, `phi/fe-07-lobby`. Một nhánh một FE, sống ≤ 3 ngày.
3. **Commit:** `FE-09: kiểm tra nước đi đúng lượt`. Mỗi commit một ý.
4. **Trước khi merge:**
   ```bash
   git fetch origin
   git rebase origin/dev
   mvn clean test
   ```
   Rồi merge vào `dev` (qua Pull Request để người kia xem nhanh, hoặc merge local nếu cả hai đồng ý).
5. **Chỉ sửa file của mình** theo bảng mục 8.
6. **File hợp đồng** (`Player`, `GameEndListener`, `MatchRecorder`, `Command`, các `pom.xml`, `Screen`, `ErrorMessages`, 3 dòng đăng ký controller trong `CaroServer`):
   - Nhắn người kia trước.
   - Sửa trong **commit riêng** có tiền tố `CONTRACT:`, không trộn với code tính năng.
   - Merge ngay, người kia rebase ngay.
7. **Thêm lệnh giao thức mới:** sửa mục 10 trong [03-giao-thuc.md](03-giao-thuc.md) trước → P thêm vào `Command` (thêm ở cuối enum) → mỗi người cài phần của mình.
8. **FXML:** mỗi màn hình một file, không bao giờ hai người cùng sửa một file FXML. Chat được nhúng bằng `fx:include`.
9. **Không commit:** `target/`, `.idea/`, `*.iml`, `.vscode/`, file mật khẩu DB (dùng biến môi trường hoặc `server.properties` nằm trong `.gitignore` + file mẫu `server.properties.example`).
10. **Họp nhanh 10 phút mỗi tối:** hôm nay xong gì, mai làm gì, có cần sửa hợp đồng không.

