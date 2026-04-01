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
CREATE TABLE rooms
(
    room_id    INT AUTO_INCREMENT PRIMARY KEY,
    room_name  VARCHAR(50),
    base_price DECIMAL(10, 2),
    type       ENUM ('standard', 'vip', 'stream')
);

-- 3. pcs (Đã thêm configuration, bỏ pc_specs)
CREATE TABLE pcs
(
    pc_id         INT PRIMARY KEY AUTO_INCREMENT,
    pc_name       VARCHAR(100)                                                 NOT NULL,
    zone_id       INT                                                          NOT NULL,
    status        ENUM ('AVAILABLE','IN_USE','MAINTENANCE','BOOKED','DELETED') NOT NULL DEFAULT 'AVAILABLE',
    configuration VARCHAR(255)                                                 NULL,
    FOREIGN KEY (zone_id) REFERENCES rooms (room_id)
);

-- 4. products
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
CREATE TABLE orders
(
    order_id            INT PRIMARY KEY AUTO_INCREMENT,
    booking_customer_id CHAR(36)                                     NULL,
    booking_pc_id       INT                                          NULL,
    booking_start_time  DATETIME                                     NULL,
    customer_id         CHAR(36)                                     NOT NULL,
    order_time          DATETIME                                     NOT NULL,
    status              ENUM ('PENDING','COMPLETED','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
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
CREATE TABLE order_details
(
    detail_id  INT PRIMARY KEY AUTO_INCREMENT,
    order_id   INT            NOT NULL,
    id         INT            NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (order_id),
    FOREIGN KEY (id) REFERENCES products (id),
    INDEX idx_order_details_order (order_id),
    INDEX idx_order_details_product (id)
);

-- 8. transactions
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

-- ==========================================
-- ĐỔ DỮ LIỆU MẪU (SEED DATA)
-- ==========================================

INSERT IGNORE INTO roles(role_name)
VALUES ('ADMIN'), ('STAFF'), ('CUSTOMER');

INSERT INTO rooms(room_name, base_price, type)
VALUES ('atmin1', 10000.00, 'standard'),
       ('atmin2', 20000.00, 'vip'),
       ('atmin3', 30000.00, 'stream'),
       ('atmin4', 10000.00, 'standard'),
       ('atmin5', 20000.00, 'vip'),
       ('atmin6', 30000.00, 'stream');

INSERT INTO pcs(pc_name, zone_id, status, configuration)
VALUES
    ('PC01', (SELECT room_id FROM rooms WHERE room_name = 'atmin1' LIMIT 1), 'AVAILABLE', 'i5-12400F | 16GB | RTX 3060 | 144Hz'),
    ('PC02', (SELECT room_id FROM rooms WHERE room_name = 'atmin1' LIMIT 1), 'AVAILABLE', 'i5-12400F | 16GB | RTX 3060 | 144Hz'),

    ('PC03', (SELECT room_id FROM rooms WHERE room_name = 'atmin2' LIMIT 1), 'AVAILABLE', 'i7-13700K | 32GB | RTX 4070 | 240Hz'),
    ('PC04', (SELECT room_id FROM rooms WHERE room_name = 'atmin2' LIMIT 1), 'AVAILABLE', 'i7-13700K | 32GB | RTX 4070 | 240Hz'),

    ('PC05', (SELECT room_id FROM rooms WHERE room_name = 'atmin3' LIMIT 1), 'AVAILABLE', 'i9-14900K | 64GB | RTX 4090 | 360Hz'),
    ('PC06', (SELECT room_id FROM rooms WHERE room_name = 'atmin3' LIMIT 1), 'AVAILABLE', 'i9-14900K | 64GB | RTX 4090 | 360Hz'),

    ('PC07', (SELECT room_id FROM rooms WHERE room_name = 'atmin4' LIMIT 1), 'AVAILABLE', 'i5-12400F | 16GB | RTX 3060 | 144Hz'),
    ('PC08', (SELECT room_id FROM rooms WHERE room_name = 'atmin4' LIMIT 1), 'AVAILABLE', 'i5-12400F | 16GB | RTX 3060 | 144Hz'),

    ('PC09', (SELECT room_id FROM rooms WHERE room_name = 'atmin5' LIMIT 1), 'AVAILABLE', 'i7-13700K | 32GB | RTX 4070 | 240Hz'),
    ('PC10', (SELECT room_id FROM rooms WHERE room_name = 'atmin5' LIMIT 1), 'AVAILABLE', 'i7-13700K | 32GB | RTX 4070 | 240Hz'),

    ('PC11', (SELECT room_id FROM rooms WHERE room_name = 'atmin6' LIMIT 1), 'AVAILABLE', 'i9-14900K | 64GB | RTX 4090 | 360Hz'),
    ('PC012', (SELECT room_id FROM rooms WHERE room_name = 'atmin6' LIMIT 1), 'AVAILABLE', 'i9-14900K | 64GB | RTX 4090 | 360Hz');

INSERT INTO users(username, password_hash, phone, fullname, balance)
VALUES ('admin', '$2a$12$N/q07apHWmd6kTvDbATnweoytjYUcCHqjgrBs/eSAiFmVxQ/QsQ4O', '0123456789', 'Administrator', 50000);

INSERT INTO user_roles(user_id, role_id)
VALUES ((SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), (SELECT role_id FROM roles WHERE role_name = 'ADMIN' LIMIT 1)),
       ((SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), (SELECT role_id FROM roles WHERE role_name = 'STAFF' LIMIT 1)),
       ((SELECT user_id FROM users WHERE username = 'admin' LIMIT 1), (SELECT role_id FROM roles WHERE role_name = 'CUSTOMER' LIMIT 1));

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


select * from transactions;