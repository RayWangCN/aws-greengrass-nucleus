/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.componentmanager.plugins.docker;

import com.aws.greengrass.componentmanager.plugins.docker.exceptions.RegistryAuthException;
import com.aws.greengrass.deployment.DeviceConfiguration;
import com.aws.greengrass.tes.LazyCredentialProvider;
import com.aws.greengrass.util.Coerce;
import com.aws.greengrass.util.ProxyUtils;
import com.aws.greengrass.util.Utils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.AuthorizationData;
import software.amazon.awssdk.services.ecr.model.EcrException;
import software.amazon.awssdk.services.ecr.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.ecr.model.ServerException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import javax.inject.Inject;

/**
 * AWS ECR SDK client wrapper.
 */
public class EcrAccessor {
    private final EcrClient injectedClient;
    private final DeviceConfiguration deviceConfiguration;
    private final LazyCredentialProvider lazyCredentialProvider;

    /**
     * Constructor.
     *
     * @param deviceConfiguration    Device config
     * @param lazyCredentialProvider AWS credentials provider
     */
    @Inject
    @SuppressWarnings("PMD.NullAssignment")
    public EcrAccessor(DeviceConfiguration deviceConfiguration, LazyCredentialProvider lazyCredentialProvider) {
        this.deviceConfiguration = deviceConfiguration;
        this.lazyCredentialProvider = lazyCredentialProvider;
        this.injectedClient = null;
    }

    /**
     * Constructor for testing with a mocked client.
     *
     * @param client EcrClient
     */
    @SuppressWarnings("PMD.NullAssignment")
    public EcrAccessor(EcrClient client) {
        this.injectedClient = client;
        this.deviceConfiguration = null;
        this.lazyCredentialProvider = null;
    }

    private EcrClient getClient(String actualRegion) {
        if (injectedClient != null) {
            return injectedClient;
        }

        if (Utils.isEmpty(actualRegion)) {
            actualRegion = Coerce.toString(deviceConfiguration.getAWSRegion());
        }

        return EcrClient.builder().httpClient(ProxyUtils.getSdkHttpClient())
                .region(Region.of(actualRegion))
                .credentialsProvider(lazyCredentialProvider).build();
    }

    /**
     * Get credentials(auth token) for a private docker registry in ECR.
     *
     * @param registryId Registry id
     * @param actualRegion actual region
     * @return Registry.Credentials - Registry's authorization information
     * @throws RegistryAuthException When authentication fails
     */
    @SuppressWarnings("PMD.AvoidRethrowingException")
    public Registry.Credentials getCredentials(String registryId,String actualRegion) throws RegistryAuthException {
        try (EcrClient client = getClient(actualRegion)) {
            AuthorizationData authorizationData = client.getAuthorizationToken(
                    GetAuthorizationTokenRequest.builder().registryIds(Collections.singletonList(registryId)).build())
                    .authorizationData().get(0);
            // Decoded auth token is of the format <username>:<password>
            String[] authTokenParts = new String(Base64.getDecoder().decode(authorizationData.authorizationToken()),
                    StandardCharsets.UTF_8).split(":");
            return new Registry.Credentials(authTokenParts[0], authTokenParts[1], authorizationData.expiresAt());
        } catch (ServerException | SdkClientException e) {
            // Errors we can retry on
            throw e;
        } catch (EcrException e) {
            throw new RegistryAuthException(
                    String.format("Failed to get credentials for ECR registry - %s", registryId), e);
        }
    }
}
