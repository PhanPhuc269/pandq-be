package pandq.application.port.repositories;

import pandq.domain.models.marketing.Promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    Promotion save(Promotion promotion);
    Optional<Promotion> findById(UUID id);
    Optional<Promotion> findByCode(String code);
    List<Promotion> findAll();
    List<Promotion> findActivePromotions(LocalDateTime now);
    void deleteById(UUID id);
}
