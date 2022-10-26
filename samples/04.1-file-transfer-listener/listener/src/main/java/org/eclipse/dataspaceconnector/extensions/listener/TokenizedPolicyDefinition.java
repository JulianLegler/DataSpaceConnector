package org.eclipse.dataspaceconnector.extensions.listener;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;

public class TokenizedPolicyDefinition {
    String token_id;
    String name;
    String decimals;
    PolicyDefinitionResponseDto policyData;

    public String getToken_id() {
        return token_id;
    }

    public void setToken_id(String token_id) {
        this.token_id = token_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDecimals() {
        return decimals;
    }

    public void setDecimals(String decimals) {
        this.decimals = decimals;
    }

    public PolicyDefinitionResponseDto getPolicyData() {
        return policyData;
    }

    public void setPolicyData(PolicyDefinitionResponseDto policyData) {
        this.policyData = policyData;
    }
}
