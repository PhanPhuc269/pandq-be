package pandq.application.port.repositories;

import org.springframework.data.domain.Page;
import pandq.adapter.web.api.dtos.ProductSearchDTO;
import pandq.domain.models.product.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    List<Product> findAll();
    List<Product> findByCategoryId(UUID categoryId);
    void deleteById(UUID id);
    Page<Product> search(ProductSearchDTO.SearchRequest request);
}
