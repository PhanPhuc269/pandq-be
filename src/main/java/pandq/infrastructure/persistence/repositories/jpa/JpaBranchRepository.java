package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.branch.Branch;

import java.util.UUID;

public interface JpaBranchRepository extends JpaRepository<Branch, UUID> {
}
