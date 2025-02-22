/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.transferprocess;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.TransferProcessStoreStatements;
import org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;

import java.time.Clock;

@Provides(TransferProcessStore.class)
@Extension(value = "SQL transfer process store")
public class SqlTransferProcessStoreExtension implements ServiceExtension {

    @EdcSetting
    private static final String DATASOURCE_NAME_SETTING = "edc.datasource.transferprocess.name";
    private static final String DEFAULT_DATASOURCE_NAME = "transferprocess";

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;
    @Inject
    private Clock clock;

    @Inject(required = false)
    private TransferProcessStoreStatements statements;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new SqlTransferProcessStore(dataSourceRegistry, getDataSourceName(context), trxContext, context.getTypeManager().getMapper(), getStatementImpl(), context.getConnectorId(), clock);
        context.registerService(TransferProcessStore.class, store);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private TransferProcessStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_NAME_SETTING, DEFAULT_DATASOURCE_NAME);
    }
}
