DROP DATABASE IF EXISTS cyber_gaming_db;
CREATE DATABASE cyber_gaming_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cyber_gaming_db;

-- 1. users
CREATE TABLE users
(
    user_id       CHAR(36) PRIMARY KEY    DEFAULT (UUID()),
    username      VARCHAR(50)    NOT NULL UNIQUE,
    password_hash VARCHAR(255)   NOT NULL,
    phone         VARCHAR(20) UNIQUE,
    fullname      VARCHAR(100)   NOT NULL,
    balance       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status        INT            NOT NULL DEFAULT 1,
    INDEX idx_users_username (username)
);

-- 1.1 roles
-- roles: danh mục quyền (một user có thể có nhiều role)
CREATE TABLE roles
(
    role_id   INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- 1.2 user_roles (quan hệ N-N)
CREATE TABLE user_roles
(
    user_id CHAR(36) NOT NULL,
    role_id INT      NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id),
    FOREIGN KEY (role_id) REFERENCES roles (role_id),
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
);

-- 2. rooms
-- rooms: khu vực/phòng máy, quyết định giá/giờ
CREATE TABLE rooms
(
    room_id    INT AUTO_INCREMENT PRIMARY KEY,
    room_name  VARCHAR(50),
    base_price DECIMAL(10, 2),
    type       ENUM ('standard', 'vip', 'stream')
);

-- 3. pcs
-- pcs: thông tin máy trạm
-- status: AVAILABLE / IN_USE / MAINTENANCE / BOOKED
CREATE TABLE pcs
(
    pc_id   INT PRIMARY KEY AUTO_INCREMENT,
    pc_name VARCHAR(100)                                                 NOT NULL,
    zone_id INT                                                          NOT NULL,
    status  ENUM ('AVAILABLE','IN_USE','MAINTENANCE','BOOKED','DELETED') NOT NULL DEFAULT 'AVAILABLE',
    FOREIGN KEY (zone_id) REFERENCES rooms (room_id)

);

-- 3.1 pc_specs
-- pc_specs: cấu hình phần cứng cho từng máy (quan hệ 1-1 với pcs)
CREATE TABLE pc_specs
(
    pc_id       INT PRIMARY KEY,
    cpu         VARCHAR(100) NULL,
    ram_gb      INT          NULL,
    gpu         VARCHAR(100) NULL,
    storage_gb  INT          NULL,
    monitor_hz  INT          NULL,
    os          VARCHAR(100) NULL,
    notes       VARCHAR(255) NULL,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pc_id) REFERENCES pcs (pc_id) ON DELETE CASCADE
);

-- 4. products
-- products: đồ ăn/nước/thẻ nạp...
-- unit price hiện tại nằm ở products.price; giá tại thời điểm mua nằm ở order_details.unit_price
CREATE TABLE products
(
    id             INT PRIMARY KEY                 AUTO_INCREMENT,
    product_name   VARCHAR(150)                 NOT NULL,
    description    TEXT,
    price          DECIMAL(15, 2)               NOT NULL,
    stock_quantity INT                          NOT NULL DEFAULT 0,
    category       ENUM ('FOOD','DRINK','CARD') NOT NULL
);

-- 5. bookings
-- bookings: phiên sử dụng máy
-- PK ghép (customer_id, pc_id, start_time) để đảm bảo không trùng phiên bắt đầu
-- Lưu ý: vì PK ghép nên các bảng muốn tham chiếu booking phải mang đủ 3 cột khóa
CREATE TABLE bookings
(
    customer_id       CHAR(36)                                          NOT NULL,
    pc_id             INT                                               NOT NULL,
    start_time        DATETIME                                          NOT NULL,
    expected_end_time DATETIME                                          NOT NULL,
    actual_end_time   DATETIME                                          NULL,
    status            ENUM ('PENDING','ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_fee         DECIMAL(15, 2)                                    NOT NULL DEFAULT 0.00,
    PRIMARY KEY (customer_id, pc_id, start_time),
    FOREIGN KEY (customer_id) REFERENCES users (user_id),
    FOREIGN KEY (pc_id) REFERENCES pcs (pc_id),
    INDEX idx_bookings_customer (customer_id),
    INDEX idx_bookings_pc (pc_id),
    INDEX idx_bookings_status (status)
);

-- 6. orders
-- orders: hóa đơn đồ ăn/nước
-- booking_* nullable: hóa đơn có thể gắn với 1 phiên chơi hoặc không
CREATE TABLE orders
(
    order_id            CHAR(36) PRIMARY KEY                                  DEFAULT (UUID()),
    booking_customer_id CHAR(36)                                     NULL,
    booking_pc_id       INT                                          NULL,
    booking_start_time  DATETIME                                     NULL,
    customer_id         CHAR(36)                                     NOT NULL,
    order_time          DATETIME                                     NOT NULL,
    status              ENUM ('PENDING','PREPARING','SERVED','PAID') NOT NULL DEFAULT 'PENDING',
    total_amount        DECIMAL(15, 2)                               NOT NULL DEFAULT 0.00,
    discount_code       VARCHAR(50)                                  NULL,
    FOREIGN KEY (booking_customer_id, booking_pc_id, booking_start_time)
        REFERENCES bookings (customer_id, pc_id, start_time),
    FOREIGN KEY (customer_id) REFERENCES users (user_id),
    INDEX idx_orders_customer (customer_id),
    INDEX idx_orders_booking (booking_customer_id, booking_pc_id, booking_start_time),
    INDEX idx_orders_status (status)
);

-- 7. order_details
-- order_details: chi tiết hóa đơn
-- ON DELETE CASCADE theo order_id: xóa order thì chi tiết tự xóa theo
CREATE TABLE order_details
(
    detail_id  CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    order_id   CHAR(36)       NOT NULL,
    id         INT            NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (order_id),
    FOREIGN KEY (id) REFERENCES products (id),
    INDEX idx_order_details_order (order_id),
    INDEX idx_order_details_product (id)
);

-- 8. transactions
-- transactions: lịch sử giao dịch ví
-- amount: số dương; transaction_type phân biệt DEPOSIT/PAYMENT/REFUND
CREATE TABLE transactions
(
    transaction_id   INT PRIMARY KEY AUTO_INCREMENT,
    user_id          CHAR(36)                            NOT NULL,
    amount           DECIMAL(15, 2)                      NOT NULL,
    transaction_type ENUM ('TOPUP','PAYMENT','REFUND') NOT NULL,
    description      VARCHAR(255),
    created_at       DATETIME                            NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id),
    INDEX idx_transactions_user (user_id),
    INDEX idx_transactions_type (transaction_type),
    INDEX idx_transactions_created (created_at)
);

-- Role mặc định khi user tự đăng ký: CUSTOMER
INSERT IGNORE INTO roles(role_name)
VALUES ('ADMIN'),
       ('STAFF'),
       ('CUSTOMER');

INSERT INTO rooms(room_name, base_price, type)
VALUES ('atmin1', 10000.00, 'standard'),
       ('atmin2', 20000.00, 'vip'),
       ('atmin3', 30000.00, 'stream'),
       ('atmin4', 10000.00, 'standard'),
       ('atmin5', 20000.00, 'vip'),
       ('atmin6', 30000.00, 'stream');

-- Seed PCs (manual): mỗi phòng 2 máy.
-- Bạn có thể đổi tên pc_name thoải mái ở đây.
INSERT INTO pcs(pc_name, zone_id, status)
VALUES ('PC01', (SELECT room_id FROM rooms WHERE room_name = 'atmin1' LIMIT 1), 'AVAILABLE'),
       ('PC02', (SELECT room_id FROM rooms WHERE room_name = 'atmin1' LIMIT 1), 'AVAILABLE'),

       ('PC03', (SELECT room_id FROM rooms WHERE room_name = 'atmin2' LIMIT 1), 'AVAILABLE'),
       ('PC04', (SELECT room_id FROM rooms WHERE room_name = 'atmin2' LIMIT 1), 'AVAILABLE'),

       ('PC05', (SELECT room_id FROM rooms WHERE room_name = 'atmin3' LIMIT 1), 'AVAILABLE'),
       ('PC06', (SELECT room_id FROM rooms WHERE room_name = 'atmin3' LIMIT 1), 'AVAILABLE'),

       ('PC07', (SELECT room_id FROM rooms WHERE room_name = 'atmin4' LIMIT 1), 'AVAILABLE'),
       ('PC08', (SELECT room_id FROM rooms WHERE room_name = 'atmin4' LIMIT 1), 'AVAILABLE'),

       ('PC09', (SELECT room_id FROM rooms WHERE room_name = 'atmin5' LIMIT 1), 'AVAILABLE'),
       ('PC10', (SELECT room_id FROM rooms WHERE room_name = 'atmin5' LIMIT 1), 'AVAILABLE'),

       ('PC11', (SELECT room_id FROM rooms WHERE room_name = 'atmin6' LIMIT 1), 'AVAILABLE'),
       ('PC012', (SELECT room_id FROM rooms WHERE room_name = 'atmin6' LIMIT 1), 'AVAILABLE');

-- Seed PC specs (sample): bạn có thể chỉnh theo thực tế
INSERT INTO pc_specs(pc_id, cpu, ram_gb, gpu, storage_gb, monitor_hz, os, notes)
SELECT pc_id,
       'Intel Core i5-12400F',
       16,
       'RTX 3060',
       512,
       144,
       'Windows 11',
       NULL
FROM pcs;

INSERT INTO users(username, password_hash, phone, fullname, balance)
VALUES ('admin', '$2a$12$N/q07apHWmd6kTvDbATnweoytjYUcCHqjgrBs/eSAiFmVxQ/QsQ4O', '0123456789', 'Administrator', 50000);
-- password plaintext: admin1702

INSERT INTO user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM users WHERE username = 'admin' LIMIT 1),
        (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1));

INSERT INTO user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM users WHERE username = 'admin' LIMIT 1),
        (SELECT role_id FROM roles WHERE role_name = 'STAFF' LIMIT 1));

INSERT INTO user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM users WHERE username = 'admin' LIMIT 1),
        (SELECT role_id FROM roles WHERE role_name = 'CUSTOMER' LIMIT 1));

-- Quick checks (optional)
SELECT *
FROM users;
SELECT *
FROM roles;
SELECT u.username, ur.role_id, r.role_name
FROM users u
         JOIN user_roles ur ON u.user_id = ur.user_id
         JOIN roles r ON ur.role_id = r.role_id;

SELECT *
FROM rooms;
SELECT *
FROM pcs
WHERE status != 'DELETED';

INSERT INTO products(product_name, description, price, stock_quantity, category)
VALUES
    ('Bánh Snack', 'Bánh khoai tây vị phô mai', 20000.00, 30, 'FOOD'),
    ('Kẹo Gum', 'Kẹo cao su vị trái cây', 10000.00, 60, 'FOOD'),
    ('Pepsi', 'Nước ngọt có ga', 12000.00, 75, 'DRINK'),
    ('Sting', 'Nước tăng lực', 15000.00, 45, 'DRINK'),
    ('Trà sữa', 'Trà sữa trân châu', 25000.00, 25, 'DRINK'),
    ('Cà phê', 'Cà phê đen đá', 18000.00, 40, 'DRINK'),
    ('Thẻ Garena 50k', 'Thẻ nạp Garena 50.000đ', 50000.00, 30, 'CARD'),
    ('Thẻ Garena 200k', 'Thẻ nạp Garena 200.000đ', 200000.00, 15, 'CARD'),
    ('Thẻ Steam 50k', 'Thẻ nạp Steam 50.000đ', 50000.00, 25, 'CARD'),
    ('Thẻ Steam 100k', 'Thẻ nạp Steam 100.000đ', 100000.00, 20, 'CARD'),
    ('Thẻ Zing 100k', 'Thẻ nạp Zing 100.000đ', 100000.00, 18, 'CARD'),
    ('Thẻ LMHT 50k', 'Thẻ nạp Liên Minh Huyền Thoại', 50000.00, 35, 'CARD');

-- Display products summary
select * from products;
SELECT COUNT(*) as total_products FROM products;
SELECT category, COUNT(*) as count FROM products GROUP BY category;


