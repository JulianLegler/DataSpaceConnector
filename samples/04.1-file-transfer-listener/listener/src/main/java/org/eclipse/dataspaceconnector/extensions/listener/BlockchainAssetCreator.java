package org.eclipse.dataspaceconnector.extensions.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetListener;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BlockchainAssetCreator implements AssetListener {


    private final Monitor monitor;
    private int stateCounter;

    private final AssetService assetService;

    private final AssetIndex assetIndex;

    public BlockchainAssetCreator(Monitor monitor, AssetService assetService, AssetIndex assetIndex) {
        this.monitor = monitor;
        this.assetService = assetService;
        this.assetIndex = assetIndex;

        this.stateCounter = 0;
    }

    private void writeToContract(Asset asset) {

        monitor.info(String.format("[%s] Asset: '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), asset.getName()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        ObjectMapper mapper = new ObjectMapper();
        // Get the dataAddress because its not stored in the Asset Object for some reasons ...
        DataAddress dataAddress = assetIndex.resolveForAsset(asset.getId());

        // Using the already created Dto Classes from the Web API Datamangement Extension
        AssetRequestDto assetRequestDto = AssetRequestDto.Builder.newInstance().id(asset.getId()).properties(asset.getProperties()).build();
        DataAddressDto dataAddressDto = DataAddressDto.Builder.newInstance().properties(dataAddress.getProperties()).build();
        AssetEntryDto assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetRequestDto).dataAddress(dataAddressDto).build();
        // Format them to JSON and print them for debugging. Change later, for now the system out println looks prettier than using monitor
        try {
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(asset));
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataAddress));
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(assetEntryDto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }




        /*
        String agreementId = process.getDataRequest().getContractId();
        String consumerAddress = process.getDataRequest().getConnectorAddress();
        String producerAddress = "producer placeholder";
        String assetId = process.getDataRequest().getAssetId();
        String unixTime = Long.toString(Instant.now().getEpochSecond());
        String hashedContract = state;

        PostObject body = new PostObject(consumerAddress, producerAddress, String.valueOf(this.stateCounter), unixTime, hashedContract, assetId);

        ObjectMapper ow = new ObjectMapper();
        String jsonString;
        try {
            jsonString = ow.writeValueAsString(body);
            System.out.println(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try{
            URL url = new URL("http://localhost:3000/contractAgreementMap/add");
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");

            //String data = "{\"consumerId\": \"" + consumerAddress + "\", \"producerId\": \"" + producerAddress + "\", \"transactionId\": \"" + agreementId + "\", \"hashedLog\": \"" + assetId + "\"}";
            byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);

            OutputStream stream = http.getOutputStream();
            stream.write(out);
            // dismiss http response
            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
            http.disconnect();
        } catch(Exception e) {
            monitor.severe(e.toString());
        }
        // increment state counter
        this.stateCounter ++;

         */
    }

    @Override
    public void created(Asset asset) {
        writeToContract(asset);
    }



}
