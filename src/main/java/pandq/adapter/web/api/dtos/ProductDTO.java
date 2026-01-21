package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductDTO {

    @Data
    public static class CreateRequest {
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal costPrice;
        private String thumbnailUrl;
        private Status status;
        private List<String> images;
        private List<ProductSpecificationDTO> specifications;
        private Integer stockQuantity;
    }

    @Data
    public static class UpdateRequest {
        private UUID categoryId;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal costPrice;
        private String thumbnailUrl;
        private Status status;
        private List<String> images;
        private List<ProductSpecificationDTO> specifications;
        private Integer stockQuantity;
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
        private Status status;
        private Double averageRating;
        private Integer reviewCount;
        private Integer stockQuantity;
        private List<ProductImageDTO> images;
        private List<ProductSpecificationDTO> specifications;
        private List<RelatedProductDTO> relatedProducts;
    }

    @Data
    public static class ProductImageDTO {
        private UUID id;
        private String imageUrl;
        private Integer displayOrder;
    }

    @Data
    public static class ProductSpecificationDTO {
        private String specKey;
        private String specValue;
    }

    @Data
    public static class RelatedProductDTO {
        private UUID id;
        private String name;
        private String thumbnailUrl;
        private BigDecimal price;
    }
}
