package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.PromotionRepository;
import pandq.domain.models.marketing.Promotion;
import pandq.infrastructure.persistence.repositories.jpa.JpaPromotionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PromotionRepositoryImpl implements PromotionRepository {

    private final JpaPromotionRepository jpaPromotionRepository;

    @Override
    public Promotion save(Promotion promotion) {
        return jpaPromotionRepository.save(promotion);
    }

    @Override
    public Optional<Promotion> findById(UUID id) {
        return jpaPromotionRepository.findById(id);
    }

    @Override
    public Optional<Promotion> findByCode(String code) {
        return jpaPromotionRepository.findByCode(code);
    }

    @Override
    public List<Promotion> findAll() {
        return jpaPromotionRepository.findAll();
    }

    @Override
    public List<Promotion> findActivePromotions(LocalDateTime now) {
        return jpaPromotionRepository.findByEndDateAfter(now);
    }

    @Override
    public void deleteById(UUID id) {
        jpaPromotionRepository.deleteById(id);
    }
}
