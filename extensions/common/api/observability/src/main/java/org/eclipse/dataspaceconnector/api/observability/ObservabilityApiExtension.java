/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

@Extension(value = ObservabilityApiExtension.NAME)
public class ObservabilityApiExtension implements ServiceExtension {

    public static final String NAME = "Observability API";
    @Inject
    private WebService webService;
    @Inject
    private HealthCheckService healthCheckService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {


        webService.registerResource(new ObservabilityApiController(healthCheckService));

        // contribute to the liveness probe
        healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
        healthCheckService.addLivenessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
    }

}
