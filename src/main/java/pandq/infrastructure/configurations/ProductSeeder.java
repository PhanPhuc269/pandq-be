package pandq.infrastructure.configurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.enums.Status;
import pandq.domain.models.product.Product;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Seeds test product data on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    
    private static final String TEST_PRODUCT_ID = "c3000001-0003-0003-0003-000000000001";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Check if test product already exists
        UUID testProductId = UUID.fromString(TEST_PRODUCT_ID);
        if (productRepository.findById(testProductId).isPresent()) {
            log.info("Test product already exists");
            return;
        }

        // Create test product
        Product testProduct = Product.builder()
                .id(testProductId)
                .name("Test Product")
                .description("Test product for chat functionality")
                .price(new BigDecimal("99.99"))
                .status(Status.ACTIVE)
                .thumbnailUrl("https://via.placeholder.com/300")
                .build();

        productRepository.save(testProduct);
        log.info("Test product created: {}", testProductId);
    }
}
