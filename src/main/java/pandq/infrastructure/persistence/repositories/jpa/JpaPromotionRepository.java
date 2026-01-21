package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.marketing.Promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPromotionRepository extends JpaRepository<Promotion, UUID> {
    Optional<Promotion> findByCode(String code);
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Promotion p WHERE p.status = pandq.domain.models.enums.Status.ACTIVE " +
            "AND (p.startDate IS NULL OR p.startDate <= :now) " +
            "AND (p.endDate IS NULL OR p.endDate > :now)")
    List<Promotion> findActivePromotions(@org.springframework.data.repository.query.Param("now") LocalDateTime now);

    List<Promotion> findByEndDateAfter(LocalDateTime date);
}
