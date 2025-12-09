package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.ProductDTO;
import pandq.application.port.repositories.CategoryRepository;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.product.Category;
import pandq.domain.models.product.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO.Response> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductDTO.Response> getProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO.Response getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToResponse(product);
    }

    @Transactional
    public ProductDTO.Response createProduct(ProductDTO.CreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .costPrice(request.getCostPrice())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductDTO.Response updateProduct(UUID id, ProductDTO.UpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setStatus(request.getStatus());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

    private ProductDTO.Response mapToResponse(Product product) {
        ProductDTO.Response response = new ProductDTO.Response();
        response.setId(product.getId());
        response.setCategoryId(product.getCategory().getId());
        response.setCategoryName(product.getCategory().getName());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setThumbnailUrl(product.getThumbnailUrl());
        response.setStatus(product.getStatus());
        response.setAverageRating(product.getAverageRating());
        response.setReviewCount(product.getReviewCount());
        return response;
    }
}
