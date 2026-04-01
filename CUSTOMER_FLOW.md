# Luồng Chạy Chi Tiết Hệ Thống Nhóm Tính Năng Khách Hàng (Customer Flow)

Tài liệu này mô tả luồng hoạt động chi tiết cho các chức năng liên quan đến Khách hàng (Customer) trong hệ thống Cyber Gaming Management, bắt đầu từ lúc khách hàng đăng nhập thông qua `CustomerMenu` và sử dụng các tính năng của Cyber.

## 1. Màn Hình Tổng Quan (Customer Dashboard)

Khi khách hàng chọn vào menu sử dụng máy, vòng lặp hệ thống từ `CustomerMenu` sẽ hiện ra 9 tuỳ chọn:

1. **View available PCs**: Xem danh sách máy đang trống.
2. **Start / Finish session**: Nếu chưa có phiên chạy, chọn Start để tự ngồi vào máy (walk-in). Nếu đang ngồi chơi, Menu số 2 sẽ chuyển thành Finish để kết thúc phiên và thanh toán.
3. **Order (F&B)**: Đặt đồ ăn và thức uống.
4. **Order history**: Xem lại các đơn F&B đã đặt.
5. **Order details**: Xem chi tiết từng món trong một mã đơn.
6. **Wallet balance**: Kiểm tra ví điện tử / số dư.
7. **Wallet top up**: Nạp tiền vào ví.
8. **Transaction history**: Tra cứu biến động số dư.
9. **Back**: Đăng xuất tài khoản khỏi Menu trở về màn ban đầu.

---

## 2. Luồng Bắt Đầu Chơi (Start Session - Lên máy trực tiếp)

Khách hàng muốn bắt đầu sử dụng một máy tính (PC). Thuật toán cho phép khách trực tiếp lên máy (không yêu cầu phải đặt cược tiền từ trước hay tạo Booking trên web).

### Chi tiết chạy:
1. **Kiểm tra trạng thái phiên**: Hệ thống kiểm tra xem user này đã có 1 session nào đang `ACTIVE` không. Nếu có, thì chặn luôn bằng báo lỗi *"You already have an ACTIVE session"*.
2. **Lựa chọn PC**: Hiển thị danh sách các máy tính có trạng thái `AVAILABLE`. Khách nhập ID máy vào.
3. **Tính toán số dư tối thiểu**: Hệ thống xem xét giá cấu hình máy (VD: Atmin2 là 20.000 VNĐ/giờ). Chia nhỏ để lấy ra số tiền thu phí theo 1 phút.
4. **Kiểm tra ví (Wallet Check)**: Nếu Số dư tài khoản < Tiền chơi 1 phút, hệ thống từ chối cho phép Start (Insufficient Balance). 
5. **Giao dịch Database (`SessionBillingServiceImpl.startBooking`)**:
   - Khởi tạo trực tiếp một Ticket `Booking` với các thông số:
     - `CustomerID = account login`
     - `PC_ID = ID vừa chọn`
     - `Timestamp (Start) = Ngay hiện tại`
     - `Status = ACTIVE`
     - `TotalFee = 0` 
   - Đồng thời, Update Table **PCs**: set `Status = IN_USE`.
6. **Kết quả**: Thông báo ra màn hình thời gian có thể chơi ước tính từ `[Số Dư] / [Giá 1 phút]`.

---

## 3. Luồng Chủ Động Kết Thúc (Finish Session - Xuống máy)

Khách hàng chủ động dừng phiên và tắt máy. Hệ thống tự động trừ tiền và chốt sổ dữ liệu doanh thu thống kê.

### Chi tiết chạy:
1. **Tìm PC Đang Dùng**: Tìm `Booking` của khách hàng này ở trạng thái `ACTIVE` (qua hàm `stopActiveBookingAndCharge`).
2. **Tính Tiền (Calculate Fee)**: 
   - Hệ thống quét `Thời Điểm Bắt Đầu` cho tới `Thời Điểm Ấn Finish` => đổi ra Số Phút sử dụng.
   - `Tổng Tiền = [Số Phút Chơi Thực Tế] x [Giá Phòng 1 Phút]`.
3. **Thanh Toán (Wallet Charge)**: 
   - Hàm nội bộ `walletService.charge` sẽ trừ Total Tiền từ Ví khách hàng.
   - **Xử lý thiếu tiền**: Nếu khách hàng "chơi chập" trễ vài giây bù trừ làm phí bị nhích thêm vài đồng so với số dư -> Hệ thống cho phép "Tất Toán" giới hạn phí ở mức Số Dư còn lại (cap fee = currentBalance) và rút sạch ví xuống 0, thay vì kẹt máy ném exception.
4. **Lưu Doanh Thu (`BookingDao`)**:
   - Update Booking record: 
     - `actual_end_time = Now`
     - `total_fee = Tiền đã trừ vào ví`
     - `status = COMPLETED` (Phiên hoàn thành). 
     - **Lưu ý:** Việc set `total_fee` ở mốc này sẽ làm số liệu chuẩn cho Module thống kê `StatisticsService`.
5. **Mở Lại Máy**: Update **PCs** Table: `Status = AVAILABLE`. 

---

## 4. Luồng Hệ Thống: Cưỡng Chế Tắt Máy (Auto Stop - Background Worker)

Một thread ngầm (`AutoStopServiceImpl`) chạy nền liên tục quét 60 giây / 1 vòng, nhằm tắt máy tống khách khi khách **hết tiền** mà đang mải chơi không tự ấn Finish.

### Chi tiết chạy:
1. Duyệt tất cả các `Bookings` đang ở trạng thái `ACTIVE`.
2. Đối chiếu số phút đã dùng & Cấu hình giá phòng -> Ước lượng số phút khách hàng có thể dùng.
3. Nếu **Số Dư (Balance) < Tiền của 1 Phút** do chơi thâm lẹm:
   - System auto kết thúc phiên tự động.
   - Charge đứt khoản phí mà thời gian cho phép (trừ sạch ví).
   - Đóng `Booking` -> `COMPLETED`, mở Lock PC -> `AVAILABLE`. 

---

## 5. Luồng Đồ Ăn Thức Uống (Order F&B)

Tạo đơn hàng F&B thông qua tích hợp với kho (Products) và trừ tiền tại chỗ (tránh bùng tiền đồ ăn). 

### Chi tiết chạy:
1. **Validate Chỗ Ngồi**: Khách *bắt buộc* phải có đang ngồi dùng máy (Có 1 `Booking` đang `ACTIVE`).
2. **Giỏ Hàng (Cart)**: Shows Menu -> Khách chọn mã số Món và số lượng (Quy tắc lượng tồn kho > 0).
3. **Giữ Tiền (Deposit/Reserve)**: Khách chọn đến đâu (Từng món), hệ thống gọi ngay `walletService.charge(...)` trừ thẳng mớ tiền của món đó trong ví. 
   - (Transaction được lưu là `Payment` lý do: "F&B item reserved...").
4. **Xác Nhận (Checkout)**: Hệ thống in lại Bill. Hỏi "Confirm order? (Y/N)".
   - **Bấm Y (Đồng ý)**: Pass giỏ hàng vào `OrderDao` để lưu Hóa Đơn (`Order`) liên kết với Booking ID. Ghi nhận Doanh Thu F&B.
   - **Bấm N (Huỷ ngang)**: Nhét lại mớ tiền đã charge trở về ví: `walletService.topup(...)` (Hoàn tiền - Refund reserved) và xoá giỏ hàng.

---

## 6. Luồng Quản Lý Ví Điên Tử (Wallet & Transactions)

1. **Top Up (Nạp)**: Chọn số tiền (đơn vị VNĐ). Tiền cập nhật vào Backend `balance`. 
2. **Transaction History**: Lưu lại lịch sử theo ID.
   - Hiển thị đầy đủ mọi hành động Trừ Tiền (TOPUP, PAYMENT) kể từ lúc nạp tiền thẻ cho tới trả tiền thuê máy, order mỳ tôm... để khách có thể chủ động kiểm soát chi phí.
