package pandq.adapter.web.api.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public class InventoryDTO {

    @Data
    public static class UpdateRequest {
        private Integer quantity;
        private Integer minStock;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID branchId;
        private String branchName;
        private UUID productId;
        private String productName;
        private String productThumbnail;
        private String productSku;
        private BigDecimal productPrice;
        private Integer quantity;
        private Integer minStock;
        private Integer reservedQuantity;
    }

    @Data
    public static class StatsResponse {
        private BigDecimal totalInventoryValue;
        private Integer totalProductsInStock;
        private Integer lowStockCount;
        private List<Response> lowStockItems;
        private List<Response> allItems;
    }
}
