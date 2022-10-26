package org.eclipse.dataspaceconnector.extensions.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.model.CriterionDto;
import org.eclipse.dataspaceconnector.spi.contract.definition.observe.ContractDefinitionListener;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.LinkedList;
import java.util.List;

public class BlockchainContractCreator implements ContractDefinitionListener {

    private final Monitor monitor;
    public BlockchainContractCreator(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void created(ContractDefinition contractDefinition) {
        String jsonString = transformToJSON(contractDefinition);
        ReturnObject returnObject = BlockchainHelper.sendToContractSmartContract(jsonString, monitor);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Contract Definition creation of the Contract with id " + contractDefinition.getId());
        } else {
            System.out.printf("[%s] Created Contract %s and minted it successfully with the hash: %s", this.getClass().getSimpleName(), contractDefinition.getId(), returnObject.getHash());
        }

    }

    private String transformToJSON(ContractDefinition contractDefinition) {
        monitor.info(String.format("[%s] ContractDefinition: for '%s' and '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), contractDefinition.getContractPolicyId(), contractDefinition.getAccessPolicyId()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        ObjectMapper mapper = new ObjectMapper();

        // Cast List of Criterion to List of CriterionDto
        List<CriterionDto> criterionDtoList = new LinkedList<CriterionDto>();
        for (Criterion criterion : contractDefinition.getSelectorExpression().getCriteria()) {
            CriterionDto criterionDto = CriterionDto.Builder.newInstance()
                    .operandLeft(criterion.getOperandLeft())
                    .operandRight(criterion.getOperandRight())
                    .operator(criterion.getOperator())
                    .build();
            criterionDtoList.add(criterionDto);
        }
        // Create Conctract Definition Dto
        ContractDefinitionResponseDto contractDefinitionResponseDto = ContractDefinitionResponseDto.Builder.newInstance()
                .contractPolicyId(contractDefinition.getContractPolicyId())
                .accessPolicyId(contractDefinition.getAccessPolicyId())
                .createdAt(contractDefinition.getCreatedAt())
                .id(contractDefinition.getId())
                .criteria(criterionDtoList).build();


        String jsonString = "";
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(contractDefinitionResponseDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonString;
    }

}
