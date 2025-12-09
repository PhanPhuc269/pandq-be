package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.BranchDTO;
import pandq.application.port.repositories.BranchRepository;
import pandq.domain.models.branch.Branch;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchDTO.Response> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchDTO.Response getBranchById(UUID id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        return mapToResponse(branch);
    }

    @Transactional
    public BranchDTO.Response createBranch(BranchDTO.CreateRequest request) {
        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingHours(request.getOpeningHours())
                .status(request.getStatus())
                .build();

        Branch savedBranch = branchRepository.save(branch);
        return mapToResponse(savedBranch);
    }

    @Transactional
    public BranchDTO.Response updateBranch(UUID id, BranchDTO.UpdateRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setOpeningHours(request.getOpeningHours());
        branch.setStatus(request.getStatus());

        Branch savedBranch = branchRepository.save(branch);
        return mapToResponse(savedBranch);
    }

    @Transactional
    public void deleteBranch(UUID id) {
        branchRepository.deleteById(id);
    }

    private BranchDTO.Response mapToResponse(Branch branch) {
        BranchDTO.Response response = new BranchDTO.Response();
        response.setId(branch.getId());
        response.setName(branch.getName());
        response.setAddress(branch.getAddress());
        response.setPhone(branch.getPhone());
        response.setLatitude(branch.getLatitude());
        response.setLongitude(branch.getLongitude());
        response.setOpeningHours(branch.getOpeningHours());
        response.setStatus(branch.getStatus());
        return response;
    }
}
