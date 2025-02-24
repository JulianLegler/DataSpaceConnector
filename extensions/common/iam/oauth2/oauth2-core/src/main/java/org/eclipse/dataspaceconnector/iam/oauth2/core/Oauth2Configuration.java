/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core;

import org.eclipse.dataspaceconnector.iam.oauth2.core.identity.Oauth2ServiceImpl;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

/**
 * Configuration values and dependencies for {@link Oauth2ServiceImpl}.
 */
public class Oauth2Configuration {

    private String tokenUrl;
    private String clientId;
    private PrivateKeyResolver privateKeyResolver;
    private CertificateResolver certificateResolver;
    private PublicKeyResolver identityProviderKeyResolver;
    private String privateKeyAlias;
    private String publicCertificateAlias;
    private String providerAudience;
    private int notBeforeValidationLeeway;
    private String endpointAudience;

    private Oauth2Configuration() {

    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public String getPublicCertificateAlias() {
        return publicCertificateAlias;
    }

    public String getProviderAudience() {
        return providerAudience;
    }

    public PrivateKeyResolver getPrivateKeyResolver() {
        return privateKeyResolver;
    }

    public CertificateResolver getCertificateResolver() {
        return certificateResolver;
    }

    public PublicKeyResolver getIdentityProviderKeyResolver() {
        return identityProviderKeyResolver;
    }

    public int getNotBeforeValidationLeeway() {
        return notBeforeValidationLeeway;
    }

    public String getEndpointAudience() {
        return endpointAudience;
    }

    public static class Builder {
        private final Oauth2Configuration configuration = new Oauth2Configuration();

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder tokenUrl(String url) {
            configuration.tokenUrl = url;
            return this;
        }

        public Builder clientId(String clientId) {
            configuration.clientId = clientId;
            return this;
        }

        public Builder privateKeyResolver(PrivateKeyResolver privateKeyResolver) {
            configuration.privateKeyResolver = privateKeyResolver;
            return this;
        }

        /**
         * Resolves this runtime's certificate containing its public key.
         */
        public Builder certificateResolver(CertificateResolver certificateResolver) {
            configuration.certificateResolver = certificateResolver;
            return this;
        }

        /**
         * Resolves the certificate containing the identity provider's public key.
         */
        public Builder identityProviderKeyResolver(PublicKeyResolver identityProviderKeyResolver) {
            configuration.identityProviderKeyResolver = identityProviderKeyResolver;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            configuration.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder publicCertificateAlias(String publicCertificateAlias) {
            configuration.publicCertificateAlias = publicCertificateAlias;
            return this;
        }

        public Builder providerAudience(String providerAudience) {
            configuration.providerAudience = providerAudience;
            return this;
        }

        public Builder notBeforeValidationLeeway(int notBeforeValidationLeeway) {
            configuration.notBeforeValidationLeeway = notBeforeValidationLeeway;
            return this;
        }

        public Builder endpointAudience(String endpointAudience) {
            configuration.endpointAudience = endpointAudience;
            return this;
        }

        public Oauth2Configuration build() {
            return configuration;
        }
    }
}
