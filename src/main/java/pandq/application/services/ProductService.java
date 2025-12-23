package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.ProductDTO;
import pandq.adapter.web.api.dtos.ProductSearchDTO;
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

    @Transactional(readOnly = true)
    public Page<ProductSearchDTO.Response> searchProducts(ProductSearchDTO.SearchRequest request) {
        Page<Product> products = productRepository.search(request);
        return products.map(this::mapToSearchResponse);
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

        // Map images
        if (product.getImages() != null) {
            response.setImages(product.getImages().stream()
                    .map(img -> {
                        ProductDTO.ProductImageDTO imageDTO = new ProductDTO.ProductImageDTO();
                        imageDTO.setId(img.getId());
                        imageDTO.setImageUrl(img.getImageUrl());
                        imageDTO.setDisplayOrder(img.getDisplayOrder());
                        return imageDTO;
                    })
                    .collect(Collectors.toList()));
        }

        // Map specifications
        if (product.getSpecifications() != null) {
            response.setSpecifications(product.getSpecifications().stream()
                    .map(spec -> {
                        ProductDTO.ProductSpecificationDTO specDTO = new ProductDTO.ProductSpecificationDTO();
                        specDTO.setSpecKey(spec.getSpecKey());
                        specDTO.setSpecValue(spec.getSpecValue());
                        return specDTO;
                    })
                    .collect(Collectors.toList()));
        }

        // Map related products (limit to 4)
        if (product.getRelatedProducts() != null) {
            response.setRelatedProducts(product.getRelatedProducts().stream()
                    .limit(4)
                    .map(rel -> {
                        ProductDTO.RelatedProductDTO relDTO = new ProductDTO.RelatedProductDTO();
                        relDTO.setId(rel.getId());
                        relDTO.setName(rel.getName());
                        relDTO.setThumbnailUrl(rel.getThumbnailUrl());
                        relDTO.setPrice(rel.getPrice());
                        return relDTO;
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private ProductSearchDTO.Response mapToSearchResponse(Product product) {
        ProductSearchDTO.Response response = new ProductSearchDTO.Response();
        response.setId(product.getId());
        response.setCategoryId(product.getCategory().getId());
        response.setCategoryName(product.getCategory().getName());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setThumbnailUrl(product.getThumbnailUrl());
        response.setAverageRating(product.getAverageRating());
        response.setReviewCount(product.getReviewCount());
        // TODO: Implement isBestSeller and stockQuantity logic when Inventory module is
        // ready
        response.setIsBestSeller(product.getReviewCount() != null && product.getReviewCount() > 100);
        response.setStockQuantity(100); // Placeholder - integrate with Inventory later
        return response;
    }
}
