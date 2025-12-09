package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.marketing.Promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPromotionRepository extends JpaRepository<Promotion, UUID> {
    Optional<Promotion> findByCode(String code);
    List<Promotion> findByEndDateAfter(LocalDateTime date);
}
