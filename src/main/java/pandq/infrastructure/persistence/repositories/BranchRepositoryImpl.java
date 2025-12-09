package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.BranchRepository;
import pandq.domain.models.branch.Branch;
import pandq.infrastructure.persistence.repositories.jpa.JpaBranchRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BranchRepositoryImpl implements BranchRepository {

    private final JpaBranchRepository jpaBranchRepository;

    @Override
    public Branch save(Branch branch) {
        return jpaBranchRepository.save(branch);
    }

    @Override
    public Optional<Branch> findById(UUID id) {
        return jpaBranchRepository.findById(id);
    }

    @Override
    public List<Branch> findAll() {
        return jpaBranchRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaBranchRepository.deleteById(id);
    }
}
