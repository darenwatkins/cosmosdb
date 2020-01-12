package sample.cosmosdb.repository;

import com.microsoft.azure.spring.data.cosmosdb.repository.ReactiveCosmosRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SernovaMappableRepository<T, K> extends ReactiveCosmosRepository<T, K> {

    public abstract String getRepositoryName();
}
