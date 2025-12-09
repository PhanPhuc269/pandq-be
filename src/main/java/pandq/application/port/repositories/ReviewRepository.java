package pandq.application.port.repositories;

import pandq.domain.models.interaction.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(UUID id);
    List<Review> findByProductId(UUID productId);
    List<Review> findByUserId(UUID userId);
    void deleteById(UUID id);
}
