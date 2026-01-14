package pandq.application.services;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class để tính phí vận chuyển theo vùng địa lý (Zonal Pricing)
 * Giống cách Shopee/Lazada tính phí ship cố định theo khu vực
 */
public class ShippingFeeCalculator {

    // Danh sách tỉnh/thành lân cận HCM
    private static final List<String> NEARBY_PROVINCES = Arrays.asList(
        "Bình Dương", "Đồng Nai", "Long An", "Bà Rịa", "Vũng Tàu",
        "Tây Ninh", "Bình Phước", "Tiền Giang"
    );
    
    // Danh sách tỉnh/thành miền Nam còn lại
    private static final List<String> SOUTHERN_PROVINCES = Arrays.asList(
        "Cần Thơ", "An Giang", "Bến Tre", "Cà Mau", "Đồng Tháp", "Hậu Giang",
        "Kiên Giang", "Sóc Trăng", "Trà Vinh", "Vĩnh Long", "Bạc Liêu",
        "Ninh Thuận", "Bình Thuận", "Lâm Đồng", "Đắk Lắk", "Đắk Nông", "Gia Lai", "Kon Tum"
    );
    
    // Danh sách tỉnh/thành miền Bắc
    private static final List<String> NORTHERN_PROVINCES = Arrays.asList(
        "Hà Nội", "Hải Phòng", "Quảng Ninh", "Hải Dương", "Hưng Yên", "Thái Bình",
        "Nam Định", "Ninh Bình", "Bắc Ninh", "Bắc Giang", "Vĩnh Phúc", "Phú Thọ",
        "Thái Nguyên", "Lạng Sơn", "Cao Bằng", "Bắc Kạn", "Tuyên Quang", "Hà Giang",
        "Lào Cai", "Yên Bái", "Điện Biên", "Lai Châu", "Sơn La", "Hòa Bình"
    );
    
    // Danh sách tỉnh miền Trung
    private static final List<String> CENTRAL_PROVINCES = Arrays.asList(
        "Đà Nẵng", "Thừa Thiên Huế", "Quảng Nam", "Quảng Ngãi", "Bình Định",
        "Phú Yên", "Khánh Hòa", "Quảng Bình", "Quảng Trị", "Hà Tĩnh",
        "Nghệ An", "Thanh Hóa"
    );
    
    /**
     * Tính phí vận chuyển dựa trên thành phố/tỉnh
     * @param city Tên thành phố/tỉnh từ địa chỉ giao hàng
     * @return Phí vận chuyển (VND) dạng BigDecimal
     */
    public static BigDecimal calculateShippingFee(String city) {
        if (city == null || city.trim().isEmpty()) {
            return BigDecimal.valueOf(30000); // Default fee
        }
        
        String normalizedCity = city.trim().toLowerCase();
        
        // TP.HCM
        if (isHoChiMinhCity(normalizedCity)) {
            return BigDecimal.valueOf(20000);
        }
        
        // Tỉnh lân cận HCM
        if (containsAny(normalizedCity, NEARBY_PROVINCES)) {
            return BigDecimal.valueOf(30000);
        }
        
        // Miền Nam còn lại
        if (containsAny(normalizedCity, SOUTHERN_PROVINCES)) {
            return BigDecimal.valueOf(35000);
        }
        
        // Hà Nội
        if (isHaNoi(normalizedCity)) {
            return BigDecimal.valueOf(45000);
        }
        
        // Miền Bắc
        if (containsAny(normalizedCity, NORTHERN_PROVINCES)) {
            return BigDecimal.valueOf(45000);
        }
        
        // Miền Trung
        if (containsAny(normalizedCity, CENTRAL_PROVINCES)) {
            return BigDecimal.valueOf(40000);
        }
        
        // Default - Các tỉnh khác
        return BigDecimal.valueOf(35000);
    }
    
    private static boolean isHoChiMinhCity(String city) {
        return city.contains("hồ chí minh") || city.contains("ho chi minh") 
            || city.contains("hcm") || city.contains("tphcm") || city.contains("tp.hcm")
            || city.contains("sài gòn") || city.contains("saigon");
    }
    
    private static boolean isHaNoi(String city) {
        return city.contains("hà nội") || city.contains("ha noi") || city.contains("hanoi");
    }
    
    private static boolean containsAny(String city, List<String> provinces) {
        return provinces.stream()
            .anyMatch(province -> city.contains(province.toLowerCase()));
    }
    
    /**
     * Lấy tên vùng để hiển thị
     */
    public static String getZoneName(String city) {
        if (city == null || city.trim().isEmpty()) {
            return "Tiêu chuẩn";
        }
        
        String normalizedCity = city.trim().toLowerCase();
        
        if (isHoChiMinhCity(normalizedCity)) return "Nội thành HCM";
        if (containsAny(normalizedCity, NEARBY_PROVINCES)) return "Vùng lân cận";
        if (isHaNoi(normalizedCity) || containsAny(normalizedCity, NORTHERN_PROVINCES)) return "Miền Bắc";
        if (containsAny(normalizedCity, CENTRAL_PROVINCES)) return "Miền Trung";
        
        return "Tiêu chuẩn";
    }
}
