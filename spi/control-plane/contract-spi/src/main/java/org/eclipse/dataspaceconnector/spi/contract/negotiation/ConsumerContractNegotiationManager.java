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
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - minor modifications
 *
 */

package org.eclipse.dataspaceconnector.spi.contract.negotiation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

/**
 * Manages contract negotiations on the consumer participant.
 * <p>
 * All operations are idempotent.
 */

@ExtensionPoint
public interface ConsumerContractNegotiationManager extends ContractNegotiationManager {

    /**
     * Initiates a contract negotiation for the given provider offer. The offer will have been obtained from a previous contract offer request sent to the provider.
     */
    StatusResult<ContractNegotiation> initiate(ContractOfferRequest contractOffer);

    /**
     * An offer was received from the provider.
     */
    StatusResult<ContractNegotiation> offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash);

    /**
     * The negotiation has been confirmed by the provider and the final contract received.
     */
    StatusResult<ContractNegotiation> confirmed(ClaimToken token, String negotiationId, ContractAgreement agreement, Policy policy);
}
