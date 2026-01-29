package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pandq.domain.models.search.SearchKeyword;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSearchKeywordRepository extends JpaRepository<SearchKeyword, UUID> {
    SearchKeyword findByKeyword(String keyword);
    
    @Query("SELECT s FROM SearchKeyword s ORDER BY s.searchCount DESC")
    List<SearchKeyword> findTopKeywords(Pageable pageable);
}
