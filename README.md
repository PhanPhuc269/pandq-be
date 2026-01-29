# Hướng Dẫn Chạy Dự Án Backend (PandQ)

Tài liệu này hướng dẫn chi tiết cách cài đặt và chạy backend của dự án PandQ.

## 📋 Yêu Cầu Hệ Thống (Prerequisites)

Để chạy được dự án, máy tính cần cài đặt:
- **Java 21**: JDK 21 trở lên.
- **PostgreSQL**: Cơ sở dữ liệu chính.
- **Git**: Để clone source code.

---

## ⚙️ Cấu Hình & Cài Đặt

### 1. Chuẩn bị Cơ sở dữ liệu (PostgreSQL)
Tạo một database mới trong PostgreSQL:
```sql
CREATE DATABASE pandq;
```

### 2. Cấu hình Biến Môi trường (.env)

Dự án sử dụng file `.env` để quản lý các biến môi trường và thông tin nhạy cảm.

1.  Tại thư mục gốc `pandq-be`, tạo file `.env` từ file mẫu:
    
    *   **Windows**: Copy file `.env.example` và đổi tên thành `.env`
    *   **Linux/Mac**: `cp .env.example .env`

2.  Mở file `.env` và cập nhật các thông tin cấu hình. File `.env.example` đã có hướng dẫn chi tiết cho từng mục:

    *   **Database**: URL, Username, Password của PostgreSQL local.
    *   **Security**: Refresh Token Pepper (chuỗi ngẫu nhiên), JWT Secret Key (Base64), Expiration (ms).
    *   **ZaloPay & SePay** (Tùy chọn): Thông tin kết nối cổng thanh toán.
    *   **Admin Seed**: Email và tên admin mặc định.
    *   **Cloudinary** (Tùy chọn): Cấu hình upload ảnh.

    _Lưu ý: Không commit file `.env` lên git để bảo mật thông tin._

3.  **Firebase**:
    - Đảm bảo file `src/main/resources/firebase-service-account.json` đã tồn tại (đây là file chứa key kết nối Firebase Admin SDK).

### 3. Database Migration (Liquibase)
Dự án sử dụng **Liquibase** để quản lý version database. Khi chạy ứng dụng lần đầu, Liquibase sẽ tự động:
- Tạo các bảng (tables).
- Chèn dữ liệu mẫu (seed data) nếu có.

---

## 🚀 Chạy Ứng Dụng

### Cách 1: Chạy bằng Terminal (Khuyên dùng)
Mở terminal tại thư mục gốc của dự án (`.../pandq-be`) và chạy lệnh:

**Windows:**
```powershell
.\gradlew bootRun
```

**Linux / macOS:**
```bash
./gradlew bootRun
```

### Cách 2: Chạy bằng IntelliJ IDEA
1.  Mở file `src/main/java/pandq/GraduationProjectBeApplication.java`.
2.  Nhấn nút **Run** (biểu tượng tam giác xanh) bên cạnh tên class hoặc phương thức `main`.

---

## 🔍 Kiểm Tra Hoạt Động

Sau khi ứng dụng khởi động thành công (thường thấy log `Started GraduationProjectBeApplication in ... seconds`), bạn có thể kiểm tra:

- **Server URL**: `http://localhost:8080`
- **API Health Check**: Thử gọi `GET http://localhost:8080/api/v1/init-config` (nếu có) hoặc thử Login.

### API Endpoint Ví dụ (Login)
**POST** `http://localhost:8080/api/auth/login`
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

---

## 🛠 Các Lỗi Thường Gặp

1.  **Lỗi kết nối Database (`Connection refused`)**:
    - Kiểm tra PostgreSQL service đã chạy chưa.
    - Kiểm tra username/password trong `.env` hoặc `application-local.yaml`.
    - Đảm bảo DB `pandq` đã được tạo.

2.  **Lỗi Java Version**:
    - Chạy `java -version` để đảm bảo đang dùng Java 21.

---

## 📁 Cấu Trúc Dự Án Cơ Bản
Dự án tuân theo **Clean Architecture**:
- `adapter`: Controller, DTO (Giao tiếp bên ngoài).
- `application`: Use Cases, Service Interfaces (Business Logic & Luồng xử lý).
- `domain`: Entities, Core Business Rules (Logic nghiệp vụ cốt lõi).
- `infrastructure`: Repository Impl, External Services, Configurations (Cấu hình và Triển khai chi tiết).
