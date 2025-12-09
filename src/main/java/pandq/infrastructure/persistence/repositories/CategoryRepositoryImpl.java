package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.CategoryRepository;
import pandq.domain.models.product.Category;
import pandq.infrastructure.persistence.repositories.jpa.JpaCategoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;

    @Override
    public Category save(Category category) {
        return jpaCategoryRepository.save(category);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id);
    }

    @Override
    public List<Category> findAll() {
        return jpaCategoryRepository.findAll();
    }

    @Override
    public List<Category> findByParentId(UUID parentId) {
        return jpaCategoryRepository.findByParentId(parentId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaCategoryRepository.deleteById(id);
    }
}
