package org.eclipse.dataspaceconnector.extensions.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BlockchainHelper {

    public static ReturnObject sendToAssetSmartContract(String jsonString, Monitor monitor) {
        return sendToSmartContract(jsonString, monitor, "http://localhost:3000/mint/asset");
    }

    public static ReturnObject sendToPolicySmartContract(String jsonString, Monitor monitor) {
        return sendToSmartContract(jsonString, monitor, "http://localhost:3000/mint/policy");
    }

    public static ReturnObject sendToContractSmartContract(String jsonString, Monitor monitor) {
        return sendToSmartContract(jsonString, monitor, "http://localhost:3000/mint/contract");
    }

    public static ReturnObject sendToSmartContract(String jsonString, Monitor monitor, String smartContractUrl) {
        monitor.debug(String.format("[%s] Sending data to Smart Contract, this may take some time ...", BlockchainHelper.class.getSimpleName()));
        String returnJson = "";
        ReturnObject returnObject = null;
        try{
            URL url = new URL(smartContractUrl);
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

    public static AssetEntryDto getAssetWithIdFromSmartContract(String id) {
        Asset asset = null;
        ObjectMapper mapper = new ObjectMapper();

        HttpURLConnection c = null;
        try {
            URL u = new URL("http://localhost:3000/asset/"+id);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();

                    return mapper.readValue(sb.toString(), TokenziedAsset.class).getAssetData();
            }

        } catch (MalformedURLException ex) {
           System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }

    public static ContractDefinitionResponseDto getAllContractDefinitionsFromSmartContract() {
        return null;
    }

    public static PolicyDefinitionResponseDto getPolicyWithIdFromSmartContract(String id) {
        PolicyDefinition policy = null;
        ObjectMapper mapper = new ObjectMapper();

        HttpURLConnection c = null;
        try {
            URL u = new URL("http://localhost:3000/policy/"+id);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();

                    return mapper.readValue(sb.toString(), TokenizedPolicyDefinition.class).getPolicyData();
            }

        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }
}
