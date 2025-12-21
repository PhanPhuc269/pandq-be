package pandq.infrastructure.persistence.repositories;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import pandq.adapter.web.api.dtos.ProductSearchDTO;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.product.Product;
import pandq.infrastructure.persistence.repositories.jpa.JpaProductRepository;

import java.util.ArrayList;
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

    @Override
    public Page<Product> search(ProductSearchDTO.SearchRequest request) {
        Specification<Product> spec = buildSearchSpecification(request);
        Pageable pageable = buildPageable(request);
        return jpaProductRepository.findAll(spec, pageable);
    }

    private Specification<Product> buildSearchSpecification(ProductSearchDTO.SearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by query (name or description)
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                String searchPattern = "%" + request.getQuery().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            }

            // Filter by category
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // Filter by price range
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // Filter by minimum rating
            if (request.getMinRating() != null && request.getMinRating() > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("averageRating"), request.getMinRating()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(ProductSearchDTO.SearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        
        Sort sort = switch (request.getSortBy()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating" -> Sort.by(Sort.Direction.DESC, "averageRating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // newest
        };

        return PageRequest.of(page, size, sort);
    }
}
