package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.ReviewDTO;
import pandq.application.port.repositories.ProductRepository;
import pandq.application.port.repositories.ReviewRepository;
import pandq.domain.models.interaction.Review;
import pandq.domain.models.product.Product;
import pandq.domain.models.user.User;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final JpaUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReviewDTO.Response> getReviewsByProductId(UUID productId, Integer filterByRating, String sortBy) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        // Filter by rating if specified
        if (filterByRating != null && filterByRating >= 1 && filterByRating <= 5) {
            reviews = reviews.stream()
                    .filter(review -> review.getRating().equals(filterByRating))
                    .collect(Collectors.toList());
        }

        // Sort reviews
        reviews = sortReviews(reviews, sortBy);

        return reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<Review> sortReviews(List<Review> reviews, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "newest";
        }

        switch (sortBy.toLowerCase()) {
            case "highest":
                return reviews.stream()
                        .sorted((r1, r2) -> {
                            int ratingCompare = r2.getRating().compareTo(r1.getRating());
                            if (ratingCompare != 0)
                                return ratingCompare;
                            return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                        })
                        .collect(Collectors.toList());
            case "lowest":
                return reviews.stream()
                        .sorted((r1, r2) -> {
                            int ratingCompare = r1.getRating().compareTo(r2.getRating());
                            if (ratingCompare != 0)
                                return ratingCompare;
                            return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                        })
                        .collect(Collectors.toList());
            case "newest":
            default:
                return reviews.stream()
                        .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                        .collect(Collectors.toList());
        }
    }

    @Transactional
    public ReviewDTO.Response createReview(ReviewDTO.CreateRequest request) {
        log.info("Creating review - productId: {}, userId: {}, rating: {}",
                request.getProductId(), request.getUserId(), request.getRating());

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> {
                        log.error("User not found: {}", request.getUserId());
                        return new RuntimeException("User not found");
                    });
            log.info("User found: {}", user.getFullName());
        } else {
            // TODO: Get from security context
            log.error("User ID is required but not provided");
            throw new RuntimeException("User ID required");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found: {}", request.getProductId());
                    return new RuntimeException("Product not found");
                });
        log.info("Product found: {}", product.getName());

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrls(request.getImageUrls())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review saved with ID: {}", savedReview.getId());

        // Update Product stats
        updateProductRating(product);
        log.info("Product rating updated for product: {}", product.getId());

        return mapToResponse(savedReview);
    }

    private void updateProductRating(Product product) {
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        product.setReviewCount(reviews.size());
        product.setAverageRating(avg);
        productRepository.save(product);
    }

    @Transactional
    public void deleteReview(UUID id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        Product product = review.getProduct();
        reviewRepository.deleteById(id);

        // Update stats after delete
        updateProductRating(product);
    }

    private ReviewDTO.Response mapToResponse(Review review) {
        ReviewDTO.Response response = new ReviewDTO.Response();
        response.setId(review.getId());
        response.setUserId(review.getUser().getId());
        response.setUserName(review.getUser().getFullName());
        response.setUserAvatar(review.getUser().getAvatarUrl());
        response.setProductId(review.getProduct().getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setImageUrls(review.getImageUrls());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
