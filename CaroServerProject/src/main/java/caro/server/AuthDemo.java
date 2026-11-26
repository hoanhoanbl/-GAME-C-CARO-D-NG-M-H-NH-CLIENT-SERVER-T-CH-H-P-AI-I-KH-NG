package caro.server;

import caro.server.model.auth.AuthService;
import caro.server.model.auth.AuthService.AuthException;
import caro.server.model.dao.Database;
import caro.server.model.dao.PlayerDao;

/** File thử nhanh đăng ký/đăng nhập với MySQL, không cần client. Thử xong có thể xóa. */
public class AuthDemo {
    public static void main(String[] args) {
        AuthService auth = new AuthService(new PlayerDao(new Database()));

        try { auth.register("test1", "abcdef"); System.out.println("Đăng ký test1: OK"); }
        catch (AuthException e) { System.out.println("Đăng ký test1: " + e.code()); }

        try { auth.register("test1", "abcdef"); System.out.println("Đăng ký lần 2: OK"); }
        catch (AuthException e) { System.out.println("Đăng ký lần 2: " + e.code()); }          // USERNAME_TAKEN

        try { auth.register("a!", "abcdef"); }
        catch (AuthException e) { System.out.println("Username sai định dạng: " + e.code()); } // USERNAME_INVALID

        try { System.out.println("Đăng nhập an/123456: OK, tên = " + auth.login("an", "123456")); }
        catch (AuthException e) { System.out.println("Đăng nhập an/123456: " + e.code()); }

        try { auth.login("an", "123456"); }
        catch (AuthException e) { System.out.println("Đăng nhập an lần 2: " + e.code()); }    // ALREADY_LOGGED_IN

        try { auth.login("binh", "saimatkhau"); }
        catch (AuthException e) { System.out.println("Sai mật khẩu: " + e.code()); }          // INVALID_CREDENTIALS

        try { auth.login("khongtontai", "123456"); }
        catch (AuthException e) { System.out.println("Username không tồn tại: " + e.code()); } // INVALID_CREDENTIALS
    }
}
