package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.SearchKeywordRepository;
import pandq.domain.models.search.SearchKeyword;
import pandq.infrastructure.persistence.repositories.jpa.JpaSearchKeywordRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SearchKeywordRepositoryImpl implements SearchKeywordRepository {
    
    private final JpaSearchKeywordRepository jpaRepository;

    @Override
    public SearchKeyword save(SearchKeyword searchKeyword) {
        return jpaRepository.save(searchKeyword);
    }

    @Override
    public Optional<SearchKeyword> findByKeyword(String keyword) {
        return Optional.ofNullable(jpaRepository.findByKeyword(keyword));
    }

    @Override
    public List<SearchKeyword> findTopKeywords(int limit) {
        return jpaRepository.findTopKeywords(PageRequest.of(0, limit));
    }
}
