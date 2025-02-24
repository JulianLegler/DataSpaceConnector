/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       ZF Friedrichshafen AG - enable asset filtering
 *
 */

package org.eclipse.dataspaceconnector.ids.spi.service;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.jetbrains.annotations.NotNull;

/**
 * The IDS service is able to create a description of the EDC data catalog.
 */
@ExtensionPoint
public interface CatalogService {

    /**
     * Provides the data catalog
     *
     * @return data catalog
     */
    @NotNull
    Catalog getDataCatalog(ClaimToken claimToken, QuerySpec querySpec);
}
