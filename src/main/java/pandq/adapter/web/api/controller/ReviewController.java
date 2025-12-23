package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.ReviewDTO;
import pandq.application.services.ReviewService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDTO.Response>> getReviewsByProductId(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer filterByRating,
            @RequestParam(required = false, defaultValue = "newest") String sortBy) {
        log.info("GET /api/v1/reviews/product/{} - filterByRating: {}, sortBy: {}", productId, filterByRating, sortBy);
        List<ReviewDTO.Response> reviews = reviewService.getReviewsByProductId(productId, filterByRating, sortBy);
        log.info("Returning {} reviews for product {}", reviews.size(), productId);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping
    public ResponseEntity<ReviewDTO.Response> createReview(@RequestBody ReviewDTO.CreateRequest request) {
        log.info("POST /api/v1/reviews - Creating review for product: {}, user: {}, rating: {}",
                request.getProductId(), request.getUserId(), request.getRating());
        ReviewDTO.Response response = reviewService.createReview(request);
        log.info("Review created successfully with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
