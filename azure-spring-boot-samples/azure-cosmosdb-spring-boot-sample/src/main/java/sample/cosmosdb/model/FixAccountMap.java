/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package sample.cosmosdb.model;

import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.core.mapping.PartitionKey;
import lombok.*;
import org.springframework.data.annotation.Id;

@Document(collection = "fix-account-map")
@NoArgsConstructor
@Getter
@Setter
@ToString
@AllArgsConstructor
public class FixAccountMap implements SernovaMappable{
    @Id
    private String internalKey;
    private String externalKey;

}

