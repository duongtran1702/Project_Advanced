# CYBER GAMING MANAGEMENT (Java 17 + JDBC + Console)

**Kiến trúc tổng quát:** `presentation (menu) → controller → service → dao → db (MySQL)`.

## 1) Yêu cầu môi trường

- Java **17**
- MySQL (schema trong `src/main/resources/db.sql`)
- Gradle Wrapper (`gradlew.bat`)

## 2) UTF-8 / Tiếng Việt

Dự án đã cấu hình UTF-8 trong:

- `build.gradle`: encoding cho compile/resources/javadoc/test
- `gradle.properties`: ép JVM Gradle chạy `-Dfile.encoding=UTF-8`

## 3) Database schema (MySQL)

File schema + sample data: `src/main/resources/db.sql`

### 3.1 `users` + phân quyền (ADMIN/STAFF/CUSTOMER)

`users`:

- `user_id` CHAR(36) (UUID)
- `username` VARCHAR(50) UNIQUE
- `password_hash` VARCHAR(255)
- `phone` VARCHAR(20) UNIQUE
- `fullname` VARCHAR(100) NOT NULL
- `balance` DECIMAL(15,2)
- `status` INT (mặc định 1)

Thiết kế role theo chuẩn N-N:

- `roles(role_id, role_name)` (ADMIN/STAFF/CUSTOMER)
- `user_roles(user_id, role_id)` (PK ghép)

### Query: lấy user kèm roles theo username

```sql
SELECT u.user_id, u.username, u.password_hash, u.phone, u.fullname, u.balance, u.status, r.role_name
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.user_id
LEFT JOIN roles r ON r.role_id = ur.role_id
WHERE u.username = ?;
```

## 4) Chức năng Auth (Đăng ký/Đăng nhập) + phân quyền

### 4.1 Password hashing

- Dùng BCrypt (`org.springframework.security:spring-security-crypto`)
- Util: `src/main/java/com/atmin/saber/util/HashUtil.java`

### 4.2 Register

- Entry (UI): `AuthController.handleRegister()`
- Entry (business): `AuthService.register(username, password, phone, fullname)`

Luồng đăng ký hiện tại:

- Nhập lần lượt: `username` → `password` → `fullname` → `phone`
- Validate ngay tại UI (nhập sai sẽ bắt nhập lại ngay, chương trình không bị crash):
  - `username`: không rỗng, **không trùng**
  - `password`: không rỗng, **>= 6 ký tự**
  - `fullname`: không rỗng
  - `phone`: **10 chữ số**, **bắt đầu bằng 0** (`0\d{9}`), **không trùng**
- Sau khi tạo user sẽ gán role mặc định: **CUSTOMER**

### 4.3 Login

- Entry (UI): `AuthController.handleLogin()`
- Entry (business): `AuthService.login(username, password)`

Luồng đăng nhập hiện tại:

- User nhập `username/password`
- Nếu nhập sai hoặc login fail:
  - Cho chọn **Try again** (nhập lại ngay tại màn login)
  - Hoặc **Return to Main Menu** (quay về menu để có thể chọn Register)
- Nếu login đúng:
  - Lưu `currentUser` vào `SessionContext`
  - Nếu user có nhiều role → bắt buộc chọn role trong `AuthMenu.roleMenu(...)`
  - Nếu user bấm `0` ở màn chọn role → hệ thống **clear session** và coi như login chưa hoàn tất (quay về flow login fail)

### 4.4 Phân quyền 3 vai trò

Enum: `src/main/java/com/atmin/saber/model/enums/UserRole.java`

Routing hiện tại (console):

- Có `ADMIN` → `ADMIN MENU`
- else có `STAFF` → `STAFF MENU`
- else → `CUSTOMER MENU`

> Ghi chú: phần menu theo role có thể là placeholder, tuỳ bạn nối tiếp các module booking/order/product/pc...

## 5) Cấu trúc project

```text
Atmin_Project_JavaAdvanced/
└─ src/
   ├─ main/
   │  ├─ java/
   │  │  └─ com/atmin/saber/
   │  │     ├─ Main.java                         # Entry point (chạy console)
   │  │     │
   │  │     ├─ config/                           # Cấu hình kết nối DB
   │  │     │  └─ DBConfig.java
   │  │     │
   │  │     ├─ presentation/                     # UI console + session
   │  │     │  ├─ CyberColors.java               # ANSI colors
   │  │     │  ├─ AuthMenu.java                  # Menu Login/Register + chọn role
   │  │     │  └─ SessionContext.java            # currentUser/currentRole
   │  │     │
   │  │     ├─ controller/                       # Điều phối luồng (UI → service)
   │  │     │  └─ AuthController.java
   │  │     │
   │  │     ├─ service/                          # Business logic
   │  │     │  ├─ AuthService.java               # API đăng nhập/đăng ký
   │  │     │  └─ impl/
   │  │     │     └─ AuthServiceImpl.java
   │  │     │
   │  │     ├─ dao/                              # JDBC/SQL
   │  │     │  ├─ UserDao.java
   │  │     │  └─ impl/
   │  │     │     └─ UserDaoImpl.java            # login(load roles) + register(create CUSTOMER)
   │  │     │
   │  │     ├─ model/                            # Domain models
   │  │     │  ├─ Booking.java
   │  │     │  ├─ Order.java
   │  │     │  ├─ OrderDetail.java
   │  │     │  ├─ PC.java
   │  │     │  ├─ PCZone.java
   │  │     │  ├─ Product.java
   │  │     │  ├─ Transaction.java
   │  │     │  ├─ User.java                      # có Set<UserRole>
   │  │     │  └─ enums/
   │  │     │     ├─ BookingStatus.java
   │  │     │     ├─ OrderStatus.java
   │  │     │     ├─ PCStatus.java
   │  │     │     ├─ ProductCategory.java
   │  │     │     ├─ TransactionType.java
   │  │     │     └─ UserRole.java               # ADMIN / STAFF / CUSTOMER
   │  │     │
   │  │     └─ util/                             # Utils
   │  │        ├─ DBConnection.java              # JDBC singleton
   │  │        ├─ HashUtil.java                  # BCrypt hash/verify
   │  │        ├─ ConsoleInput.java              # readNonEmpty(...) helper
   │  │        └─ TestConnection.java
   │  └─ resources/
   │     └─ db.sql
   └─ test/
      └─ java/
         └─ com/atmin/saber/util/
            └─ HashUtilTest.java
```

## 6) Chạy dự án

### 6.1 Tạo database

Chạy file SQL `src/main/resources/db.sql` trên MySQL để tạo database `cyber_gaming_db` và seed `roles`.

### 6.2 Cấu hình DB

Chỉnh trong `src/main/java/com/atmin/saber/config/DBConfig.java`:

- `URL`
- `USER`
- `PASSWORD`

### 6.3 Build & test

```powershell
cd D:\Atmin_Project_JavaAdvanced
./gradlew.bat test --no-daemon
```

### 6.4 Run

Chạy class `com.atmin.saber.Main` bằng IntelliJ.

## 7) Ghi chú

- Schema hiện tại **đã có** cột `fullname` trong bảng `users` (xem `src/main/resources/db.sql`).
- Project chạy console trên Windows/IDE: hiện tại **không dùng clear console** (đã loại bỏ hàm `clear()` vì không hoạt động ổn định trong môi trường này).

