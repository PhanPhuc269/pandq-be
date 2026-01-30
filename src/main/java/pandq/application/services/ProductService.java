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
import pandq.domain.models.product.ProductImage;
import pandq.domain.models.product.ProductSpecification;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final pandq.application.port.repositories.BranchRepository branchRepository;
    private final pandq.application.port.repositories.InventoryRepository inventoryRepository;
    private final pandq.application.port.repositories.SearchKeywordRepository searchKeywordRepository;

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
    public Page<ProductSearchDTO.Response> searchProducts(ProductSearchDTO.SearchRequest request) {
        // Log search query for trending analysis
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            logSearchKeyword(request.getQuery());
        }
        
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

        if (request.getImages() != null) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImages().size(); i++) {
                images.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImages().get(i))
                        .displayOrder(i)
                        .build());
            }
            product.setImages(images);
        }

        if (request.getSpecifications() != null) {
            List<ProductSpecification> specifications = request.getSpecifications().stream()
                    .map(spec -> ProductSpecification.builder()
                            .product(product)
                            .specKey(spec.getSpecKey())
                            .specValue(spec.getSpecValue())
                            .build())
                    .collect(Collectors.toList());
            product.setSpecifications(specifications);
        }
        
        Product savedProduct = productRepository.save(product);

        if (request.getStockQuantity() != null && request.getStockQuantity() > 0) {
            // Add to the first available branch (default logic for now)
            pandq.domain.models.branch.Branch defaultBranch = branchRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No branch found to add inventory"));

            pandq.domain.models.branch.Inventory inventory = pandq.domain.models.branch.Inventory.builder()
                    .branch(defaultBranch)
                    .product(savedProduct)
                    .quantity(request.getStockQuantity())
                    .minStock(0)
                    .reservedQuantity(0)
                    .build();
            
            inventoryRepository.save(inventory);
        }

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

        if (request.getImages() != null) {
            if (product.getImages() == null) {
                product.setImages(new ArrayList<>());
            }
            product.getImages().clear();
            for (int i = 0; i < request.getImages().size(); i++) {
                product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImages().get(i))
                        .displayOrder(i)
                        .build());
            }
        }

        if (request.getSpecifications() != null) {
            if (product.getSpecifications() == null) {
                product.setSpecifications(new ArrayList<>());
            }
            product.getSpecifications().clear();
            product.getSpecifications().addAll(request.getSpecifications().stream()
                    .map(spec -> ProductSpecification.builder()
                            .product(product)
                            .specKey(spec.getSpecKey())
                            .specValue(spec.getSpecValue())
                            .build())
                    .collect(Collectors.toList()));
        }


        Product savedProduct = productRepository.save(product);

        // Update inventory if stockQuantity is provided
        if (request.getStockQuantity() != null && request.getStockQuantity() >= 0) {
            pandq.domain.models.branch.Branch defaultBranch = branchRepository.findAll().stream()
                    .findFirst()
                    .orElse(null);

            if (defaultBranch != null) {
                pandq.domain.models.branch.Inventory inventory = inventoryRepository
                        .findByBranchIdAndProductId(defaultBranch.getId(), savedProduct.getId())
                        .orElseGet(() -> pandq.domain.models.branch.Inventory.builder()
                                .branch(defaultBranch)
                                .product(savedProduct)
                                .quantity(0)
                                .minStock(0)
                                .reservedQuantity(0)
                                .build());

                inventory.setQuantity(request.getStockQuantity());
                inventoryRepository.save(inventory);
            }
        }

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

        // Get stock quantity from inventory
        List<pandq.domain.models.branch.Inventory> inventories = inventoryRepository.findByProductId(product.getId());
        int totalStock = inventories.stream().mapToInt(pandq.domain.models.branch.Inventory::getQuantity).sum();
        response.setStockQuantity(totalStock);

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

    @Transactional(readOnly = true)
    public List<String> getTrendingSearches() {
        // Get popular keywords from search history
        return searchKeywordRepository.findTopKeywords(10).stream()
                .map(pandq.domain.models.search.SearchKeyword::getKeyword)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logSearchKeyword(String query) {
        if (query == null || query.isBlank()) return;
        
        String cleanQuery = query.trim().toLowerCase();
        // Ignore very short queries
        if (cleanQuery.length() < 2) return;

        pandq.domain.models.search.SearchKeyword keyword = searchKeywordRepository.findByKeyword(cleanQuery)
                .orElse(pandq.domain.models.search.SearchKeyword.builder()
                        .keyword(cleanQuery)
                        .searchCount(0L)
                        .build());
        
        keyword.setSearchCount(keyword.getSearchCount() + 1);
        searchKeywordRepository.save(keyword);
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
