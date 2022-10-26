package org.eclipse.dataspaceconnector.extensions.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.observe.PolicyDefinitionListener;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

public class BlockchainPolicyCreator implements PolicyDefinitionListener {

    private final Monitor monitor;
    public BlockchainPolicyCreator(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void created(PolicyDefinition policyDefinition) {
        String jsonString = transformToJSON(policyDefinition);
        System.out.println(jsonString);
        ReturnObject returnObject = BlockchainHelper.sendToPolicySmartContract(jsonString, monitor);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Policy creation of the Policy with id " + policyDefinition.getId());
        } else {
            System.out.printf("[%s] Created Policy %s and minted it successfully with the hash: %s", this.getClass().getSimpleName(), policyDefinition.getId(), returnObject.getHash());
        }
    }

    private String transformToJSON(PolicyDefinition policyDefinition) {
        monitor.info(String.format("[%s] Policy: '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), policyDefinition.getUid()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        ObjectMapper mapper = new ObjectMapper();

        PolicyDefinitionResponseDto policyDefinitionResponseDto = PolicyDefinitionResponseDto.Builder.newInstance()
                .policy(policyDefinition.getPolicy())
                .id(policyDefinition.getUid())
                .createdAt(policyDefinition.getCreatedAt())
                .build();
        // Format them to JSON and print them for debugging. Change later, for now the system out println looks prettier than using monitor
        String jsonString = "";
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyDefinitionResponseDto);
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyDefinitionResponseDto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonString;
    }

}
