package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.product.Product;

import java.util.List;
import java.util.UUID;

public interface JpaProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByCategoryId(UUID categoryId);
}
