package pandq.adapter.web.api.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

public class ProductSearchDTO {

    @Data
    public static class SearchRequest {
        private String query;
        private UUID categoryId;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Double minRating;
        private Boolean inStockOnly = false;
        private String sortBy = "newest"; // newest, price_asc, price_desc, rating
        private Integer page = 0;
        private Integer size = 20;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private String name;
        private String description;
        private BigDecimal price;
        private String thumbnailUrl;
        private Double averageRating;
        private Integer reviewCount;
        private Boolean isBestSeller;
        private Integer stockQuantity;
    }
}
