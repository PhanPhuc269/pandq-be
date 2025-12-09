package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.ReviewRepository;
import pandq.domain.models.interaction.Review;
import pandq.infrastructure.persistence.repositories.jpa.JpaReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final JpaReviewRepository jpaReviewRepository;

    @Override
    public Review save(Review review) {
        return jpaReviewRepository.save(review);
    }

    @Override
    public Optional<Review> findById(UUID id) {
        return jpaReviewRepository.findById(id);
    }

    @Override
    public List<Review> findByProductId(UUID productId) {
        return jpaReviewRepository.findByProductId(productId);
    }

    @Override
    public List<Review> findByUserId(UUID userId) {
        return jpaReviewRepository.findByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaReviewRepository.deleteById(id);
    }
}
