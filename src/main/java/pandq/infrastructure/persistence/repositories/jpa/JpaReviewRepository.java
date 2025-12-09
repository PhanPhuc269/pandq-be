package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.interaction.Review;

import java.util.List;
import java.util.UUID;

public interface JpaReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByProductId(UUID productId);
    List<Review> findByUserId(UUID userId);
}
