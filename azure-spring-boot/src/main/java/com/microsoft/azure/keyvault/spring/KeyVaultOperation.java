/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.keyvault.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.models.SecretItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class KeyVaultOperation {
    private final long cacheRefreshIntervalInMs;
    private final String[] secretKeys;

    private final Object refreshLock = new Object();
    private final KeyVaultClient keyVaultClient;
    private final String vaultUri;

    private ArrayList<String> propertyNames = new ArrayList<>();
    private String[] propertyNamesArr;

    private final AtomicLong lastUpdateTime = new AtomicLong();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public KeyVaultOperation(final KeyVaultClient keyVaultClient,
                             String vaultUri,
                             final long refreshInterval,
                             final String secretKeysConfig) {
        this.cacheRefreshIntervalInMs = refreshInterval;
        this.secretKeys = parseSecretKeys(secretKeysConfig);
        this.keyVaultClient = keyVaultClient;
        // TODO(pan): need to validate why last '/' need to be truncated.
        this.vaultUri = StringUtils.trimTrailingCharacter(vaultUri.trim(), '/');
        fillSecretsList();
    }

    private String[] parseSecretKeys(String secretKeysConfig) {
        if (StringUtils.isEmpty(secretKeysConfig)) {
            log.info("specific secret keys haven't set, so apply global list mode");
            return new String[0];
        }

        final String[] split = secretKeysConfig.split(",");
        if (split.length == 0) {
            log.info("specific secret keys haven't set, so apply global list mode");
            return new String[0];
        }

        return split;
    }

    public String[] list() {
        try {
            this.rwLock.readLock().lock();
            return propertyNamesArr;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    private String getKeyvaultSecretName(@NonNull String property) {
        if (property.matches("[a-z0-9A-Z-]+")) {
            return property.toLowerCase(Locale.US);
        } else if (property.matches("[A-Z0-9_]+")) {
            return property.toLowerCase(Locale.US).replaceAll("_", "-");
        } else {
            return property.toLowerCase(Locale.US)
                    .replaceAll("-", "")     // my-project -> myproject
                    .replaceAll("_", "")     // my_project -> myproject
                    .replaceAll("\\.", "-"); // acme.myproject -> acme-myproject
        }
    }

    /**
     * For convention we need to support all relaxed binding format from spring, these may include:
     * <table>
     * <tr><td>Spring relaxed binding names</td></tr>
     * <tr><td>acme.my-project.person.first-name</td></tr>
     * <tr><td>acme.myProject.person.firstName</td></tr>
     * <tr><td>acme.my_project.person.first_name</td></tr>
     * <tr><td>ACME_MYPROJECT_PERSON_FIRSTNAME</td></tr>
     * </table>
     * But azure keyvault only allows ^[0-9a-zA-Z-]+$ and case insensitive, so there must be some conversion
     * between spring names and azure keyvault names.
     * For example, the 4 properties stated above should be convert to acme-myproject-person-firstname in keyvault.
     *
     * @param property of secret instance.
     * @return the value of secret with given name or null.
     */
    public String get(final String property) {
        Assert.hasText(property, "property should contain text.");
        final String secretName = getKeyvaultSecretName(property);

        //if user don't set specific secret keys, then refresh token
        if (this.secretKeys == null || secretKeys.length == 0) {
            // refresh periodically
            refreshPropertyNames();
        }
        if (this.propertyNames.contains(secretName)) {
            final SecretBundle secretBundle = this.keyVaultClient.getSecret(this.vaultUri, secretName);
            return secretBundle == null ? null : secretBundle.value();
        } else {
            return null;
        }
    }

    private void refreshPropertyNames() {
        if (System.currentTimeMillis() - this.lastUpdateTime.get() > this.cacheRefreshIntervalInMs) {
            synchronized (this.refreshLock) {
                if (System.currentTimeMillis() - this.lastUpdateTime.get() > this.cacheRefreshIntervalInMs) {
                    this.lastUpdateTime.set(System.currentTimeMillis());
                    fillSecretsList();
                }
            }
        }
    }

    private void fillSecretsList() {
        try {
            this.rwLock.writeLock().lock();
            if (this.secretKeys == null || secretKeys.length == 0) {
                this.propertyNames.clear();

                final PagedList<SecretItem> secrets = this.keyVaultClient.listSecrets(this.vaultUri);
                secrets.loadAll();

                secrets.forEach(s -> {
                    final String secretName = s.id().replace(vaultUri + "/secrets/", "");
                    addSecretIfNotExist(secretName);
                });

                this.lastUpdateTime.set(System.currentTimeMillis());
            } else {
                for (final String secretKey : secretKeys) {
                    addSecretIfNotExist(secretKey);
                }
            }
            propertyNamesArr = propertyNames.toArray(new String[0]);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    private void addSecretIfNotExist(final String secretName) {
        final String secretNameLowerCase = secretName.toLowerCase(Locale.US);
        
        if (!propertyNames.contains(secretNameLowerCase)) {
            propertyNames.add(secretNameLowerCase);
        }


        final String secretNameSeparatedByDot = secretNameLowerCase.replaceAll("-", ".");
        if (!propertyNames.contains(secretNameSeparatedByDot)) {
            propertyNames.add(secretNameSeparatedByDot);
        }
    }

}
