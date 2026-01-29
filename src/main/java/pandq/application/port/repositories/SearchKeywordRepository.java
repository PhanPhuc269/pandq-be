package pandq.application.port.repositories;

import pandq.domain.models.search.SearchKeyword;
import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository {
    SearchKeyword save(SearchKeyword searchKeyword);
    Optional<SearchKeyword> findByKeyword(String keyword);
    List<SearchKeyword> findTopKeywords(int limit);
}
