package sample.cosmosdb.repository;

import com.microsoft.azure.spring.data.cosmosdb.repository.ReactiveCosmosRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import sample.cosmosdb.model.FixAccountMap;
import sample.cosmosdb.model.User;

@Repository
public interface FixAccountMapRepository extends SernovaMappableRepository<FixAccountMap, String> {

    Flux<FixAccountMap> findByInternalKey(String internalKey);
    Flux<FixAccountMap> findByExternalKey(String externalKey);

    @Override
    default String getRepositoryName() {
        return "FixAccountMap";
    }
}
