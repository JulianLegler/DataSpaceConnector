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

package org.eclipse.dataspaceconnector.extensions.listener;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.AssetApiExtension;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetServiceImpl;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionService;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.definition.observe.ContractDefinitionObservable;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetListener;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservable;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservableImpl;
import org.eclipse.dataspaceconnector.spi.policy.observe.PolicyDefinitionObservable;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;

//import org.eclipse.dataspaceconnector.api.datamangement.*;

public class TransferListenerExtension implements ServiceExtension {

    @Inject
    private TransferProcessObservable transferProcessObservable;

    // Needs to be injected to get Access to AssetObservable
    @Inject
    private AssetService assetService;

    // Needs to be injected to get Access to PolicyDefinitionObservable
    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private ContractDefinitionService contractDefinitionService;

    @Inject
    private AssetIndex assetIndex;


    @Override
    public void initialize(ServiceExtensionContext context) {


        //var assetObservable = ((AssetServiceImpl) assetService).observable;

        var assetObservable = context.getService(AssetObservable.class);

        var policyObservable = context.getService(PolicyDefinitionObservable.class);

        var contractObservable = context.getService(ContractDefinitionObservable.class);

        var monitor = context.getMonitor();



        transferProcessObservable.registerListener(new MarkerFileCreator(monitor));
        assetObservable.registerListener(new BlockchainAssetCreator(monitor, assetService, assetIndex));

        policyObservable.registerListener(new BlockchainPolicyCreator(monitor));

        contractObservable.registerListener(new BlockchainContractCreator(monitor));
    }
}
