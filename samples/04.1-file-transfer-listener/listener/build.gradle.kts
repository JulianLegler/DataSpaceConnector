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

plugins {
    `java-library`
    id("application")
}

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:data-plane-transfer-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:control-plane:contract-spi"))
    implementation(project(":extensions:common:http"))
    api(project(":extensions:control-plane:api:data-management"))
    implementation(project(":extensions:control-plane:api:data-management"))
    api(project(":spi"))
}