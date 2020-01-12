/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package sample.cosmosdb.repository;

import com.microsoft.azure.spring.data.cosmosdb.repository.ReactiveCosmosRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import sample.cosmosdb.model.SernovaMappable;
import sample.cosmosdb.model.User;

@Repository
public interface UserRepository extends SernovaMappableRepository<User, String> {

    Flux<User> findByFirstName(String firstName);

    @Override
    default String getRepositoryName() {
        return "User";
    }
}
