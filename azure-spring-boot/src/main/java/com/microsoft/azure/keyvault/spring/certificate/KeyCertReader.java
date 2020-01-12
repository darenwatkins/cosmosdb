/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.keyvault.spring.certificate;

import org.springframework.core.io.Resource;

public interface KeyCertReader {
    KeyCert read(Resource resource, String password);
}
