package pandq.adapter.web.api.dtos;

import lombok.Data;
import java.util.UUID;

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
        private Integer quantity;
        private Integer minStock;
        private Integer reservedQuantity;
    }
}
