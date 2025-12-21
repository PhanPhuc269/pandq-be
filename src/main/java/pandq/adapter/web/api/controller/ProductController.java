package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.ProductDTO;
import pandq.adapter.web.api.dtos.ProductSearchDTO;
import pandq.adapter.web.api.dtos.response.PaginationMetaDto;
import pandq.adapter.web.api.dtos.response.PaginationResponseDto;
import pandq.application.services.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDTO.Response>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO.Response>> getProductsByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginationResponseDto<ProductSearchDTO.Response>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        ProductSearchDTO.SearchRequest request = new ProductSearchDTO.SearchRequest();
        request.setQuery(query);
        request.setCategoryId(categoryId);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setMinRating(minRating);
        request.setInStockOnly(inStockOnly);
        request.setSortBy(sortBy);
        request.setPage(page);
        request.setSize(size);

        Page<ProductSearchDTO.Response> result = productService.searchProducts(request);
        
        PaginationMetaDto paginationMeta = new PaginationMetaDto(
            result.getNumber(),
            result.getSize(),
            result.getTotalElements()
        );

        return ResponseEntity.ok(PaginationResponseDto.of(
            result.getContent(),
            paginationMeta,
            "OK",
            "Search successful"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO.Response> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDTO.Response> createProduct(@RequestBody ProductDTO.CreateRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO.Response> updateProduct(@PathVariable UUID id, @RequestBody ProductDTO.UpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

