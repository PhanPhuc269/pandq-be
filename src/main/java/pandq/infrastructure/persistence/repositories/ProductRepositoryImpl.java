package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.product.Product;
import pandq.infrastructure.persistence.repositories.jpa.JpaProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final JpaProductRepository jpaProductRepository;

    @Override
    public Product save(Product product) {
        return jpaProductRepository.save(product);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaProductRepository.findById(id);
    }

    @Override
    public List<Product> findAll() {
        return jpaProductRepository.findAll();
    }

    @Override
    public List<Product> findByCategoryId(UUID categoryId) {
        return jpaProductRepository.findByCategoryId(categoryId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaProductRepository.deleteById(id);
    }
}
