package pandq.application.services;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final JpaUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReviewDTO.Response> getReviewsByProductId(UUID productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDTO.Response createReview(ReviewDTO.CreateRequest request) {
        User user = null;
        if (request.getUserId() != null){
             user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
             // TODO: Get from security context
             throw new RuntimeException("User ID required");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrls(request.getImageUrls())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Update Product stats
        updateProductRating(product);

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
