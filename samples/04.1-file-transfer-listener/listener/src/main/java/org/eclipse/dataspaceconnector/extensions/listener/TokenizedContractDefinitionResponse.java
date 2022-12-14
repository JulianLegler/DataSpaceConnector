package org.eclipse.dataspaceconnector.extensions.listener;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;

public class TokenizedContractDefinitionResponse {
    ContractDefinitionResponseDto tokenData;
    String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ContractDefinitionResponseDto getTokenData() {
        return tokenData;
    }

    public void setTokenData(ContractDefinitionResponseDto tokenData) {
        this.tokenData = tokenData;
    }

}
