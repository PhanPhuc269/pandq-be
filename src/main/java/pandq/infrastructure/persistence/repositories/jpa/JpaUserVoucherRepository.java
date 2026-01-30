package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pandq.domain.models.marketing.UserVoucher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserVoucherRepository extends JpaRepository<UserVoucher, UUID> {
    
    List<UserVoucher> findByUserId(UUID userId);
    
    List<UserVoucher> findByUserIdAndIsUsedFalse(UUID userId);
    
    Optional<UserVoucher> findByUserIdAndPromotionId(UUID userId, UUID promotionId);
    
    boolean existsByUserIdAndPromotionId(UUID userId, UUID promotionId);
}
