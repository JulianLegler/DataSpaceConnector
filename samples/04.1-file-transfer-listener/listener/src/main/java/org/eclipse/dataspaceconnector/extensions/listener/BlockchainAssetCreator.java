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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

    private static class ReturnObject {
        public String status;
        public String hash;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }
    }

    private String transformToJSON(Asset asset) {

        monitor.info(String.format("[%s] Asset: '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), asset.getName()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        ObjectMapper mapper = new ObjectMapper();
        // Get the dataAddress because its not stored in the Asset Object for some reasons ...
        DataAddress dataAddress = assetIndex.resolveForAsset(asset.getId());

        // Using the already created Dto Classes from the Web API Datamangement Extension
        AssetRequestDto assetRequestDto = AssetRequestDto.Builder.newInstance().id(asset.getId()).properties(asset.getProperties()).build();
        DataAddressDto dataAddressDto = DataAddressDto.Builder.newInstance().properties(dataAddress.getProperties()).build();
        AssetEntryDto assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetRequestDto).dataAddress(dataAddressDto).build();

        String jsonString = "";
        // Format them to JSON and print them for debugging. Change later, for now the system out println looks prettier than using monitor
        try {
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(asset));
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataAddress));
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(assetEntryDto);
            System.out.println(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonString;
    }

    private ReturnObject sendToSmartContract(String jsonString) {
        monitor.debug(String.format("[%s] Sending data to Smart Contract, this may take some time ...", this.getClass().getSimpleName()));
        String returnJson = "";
        ReturnObject returnObject = null;
        try{
            URL url = new URL("http://localhost:3000/mint/asset");
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");

            byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);

            OutputStream stream = http.getOutputStream();
            stream.write(out);

            BufferedReader br = null;
            if (100 <= http.getResponseCode() && http.getResponseCode() <= 399) {
                br = new BufferedReader(new InputStreamReader(http.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
            }

            while ((returnJson = br.readLine()) != null) {
                System.out.println(returnJson);
                ObjectMapper mapper = new ObjectMapper();
                returnObject = mapper.readValue(returnJson, ReturnObject.class);
            }

            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
            http.disconnect();
        } catch(Exception e) {
            monitor.severe(e.toString());
        }

        return returnObject;
    }

    @Override
    public void created(Asset asset) {
        String jsonString = transformToJSON(asset);
        ReturnObject returnObject = sendToSmartContract(jsonString);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Asset creation of the Asset with id " + asset.getId());
        } else {
            System.out.printf("[%s] Created Asset %s and minted it successfully with the hash: %s", this.getClass().getSimpleName(), asset.getId(), returnObject.getHash());
        }
    }



}
