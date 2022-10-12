package org.eclipse.dataspaceconnector.extensions.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetListener;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BlockchainAssetCreator implements AssetListener {


    private final Monitor monitor;
    private int stateCounter;

    public BlockchainAssetCreator(Monitor monitor) {
        this.monitor = monitor;
        this.stateCounter = 0;
    }

    private class PostObject {
        public String consumerId;
        public String producerId;
        public String transactionId;
        public String timestamp;
        public String hashedContract;
        public String assetToken;

        public PostObject(String consumerId, String producerId, String transactionId, String timestamp, String hashedContract, String assetToken) {
            this.consumerId = consumerId;
            this.producerId = producerId;
            this.transactionId = transactionId;
            this.timestamp = timestamp;
            this.hashedContract = hashedContract;
            this.assetToken = assetToken;
        }
    }

    private void writeToContract(Asset asset) {

        monitor.info(String.format("[%s] Asset: '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), asset.getName()));

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
