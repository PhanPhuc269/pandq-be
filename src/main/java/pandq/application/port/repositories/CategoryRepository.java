package pandq.application.port.repositories;

import pandq.domain.models.product.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    List<Category> findAll();
    List<Category> findByParentId(UUID parentId);
    void deleteById(UUID id);
}
