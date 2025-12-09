package pandq.application.port.repositories;

import pandq.domain.models.branch.Branch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository {
    Branch save(Branch branch);
    Optional<Branch> findById(UUID id);
    List<Branch> findAll();
    void deleteById(UUID id);
}
