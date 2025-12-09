package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.product.Category;

import java.util.List;
import java.util.UUID;

public interface JpaCategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByParentId(UUID parentId);
}
