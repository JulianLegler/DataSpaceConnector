/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - name refactoring
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetEventListener;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.transform.AssetRequestDtoToAssetTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.transform.AssetToAssetResponseDtoTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.transform.DataAddressDtoToDataAddressTransformer;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservable;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservableImpl;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.time.Clock;

@Provides(AssetService.class)
@Extension(value = AssetApiExtension.NAME)
public class AssetApiExtension implements ServiceExtension {

    public static final String NAME = "Data Management API: Asset";
    @Inject
    WebService webService;

    @Inject
    DataManagementApiConfiguration config;

    @Inject
    AssetIndex assetIndex;

    @Inject
    ContractNegotiationStore contractNegotiationStore;

    @Inject
    DtoTransformerRegistry transformerRegistry;

    @Inject
    TransactionContext transactionContext;

    @Inject
    EventRouter eventRouter;

    @Inject
    Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var assetObservable = new AssetObservableImpl();
        assetObservable.registerListener(new AssetEventListener(clock, eventRouter));
        context.registerService(AssetObservable.class, assetObservable);

        var assetService = new AssetServiceImpl(assetIndex, contractNegotiationStore, transactionContext, assetObservable);
        context.registerService(AssetService.class, assetService);


        transformerRegistry.register(new AssetRequestDtoToAssetTransformer());
        transformerRegistry.register(new DataAddressDtoToDataAddressTransformer());
        transformerRegistry.register(new AssetToAssetResponseDtoTransformer());

        webService.registerResource(config.getContextAlias(), new AssetApiController(monitor, assetService, transformerRegistry));
    }

}
