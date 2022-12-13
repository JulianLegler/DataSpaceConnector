package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CatalogCrawler;
import org.eclipse.dataspaceconnector.catalog.spi.*;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.extensions.listener.BlockchainHelper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class BlockchainExecutionManager {
    private Monitor monitor;
    private Runnable preExecutionTask;
    private Runnable postExecutionTask;
    private FederatedCacheNodeDirectory directory;
    private FederatedCacheNodeFilter nodeFilter;
    private int numCrawlers = 1;
    private NodeQueryAdapterRegistry nodeQueryAdapterRegistry;
    private CrawlerSuccessHandler successHandler;
    private FederatedCacheStore store;

    private BlockchainExecutionManager() {
        nodeFilter = n -> true;
    }

    public void executePlan(ExecutionPlan plan) {
        plan.run(() -> {

            monitor.info(message("Just here to hijack this worker ... nothing to see ... "));

            monitor.info(message("Run pre-execution task"));
            runPreExecution();

            monitor.info(message("Run execution"));
            doWork();

            monitor.info(message("Run post-execution task"));
            runPostExecution();
        });

    }

    private void doWork() {

        Criterion criterion = new Criterion("asset:prop:id", "=", "test-document");
        Criterion criterion1 = new Criterion("asset:prop:id", "=", "test-document-64");
        ArrayList<Criterion> queryList = new ArrayList<>();
        queryList.add(criterion);
        queryList.add(criterion1);

        List<ContractOffer> contractOfferListFromStorage = (List<ContractOffer>) store.query(queryList);
        monitor.info(format("Fetched %d enties from local cataloge storage", contractOfferListFromStorage.size()));

        List<AssetEntryDto> assetEntryDtoList = BlockchainHelper.getAllAssetsFromSmartContract();
        monitor.info(String.format("[%s] fetched %d Assets from Smart Contract", this.getClass().getSimpleName(), assetEntryDtoList.size()));

        List<PolicyDefinitionResponseDto> policyDefinitionResponseDtoList = BlockchainHelper.getAllPolicyDefinitionsFromSmartContract();
        monitor.info(String.format("[%s] fetched %d Policies from Smart Contract", this.getClass().getSimpleName(), policyDefinitionResponseDtoList.size()));

        List<ContractDefinitionResponseDto> contractDefinitionResponseDtoList = BlockchainHelper.getAllContractDefinitionsFromSmartContract();

        HashMap<String, List<ContractDefinitionResponseDto>> contractDefinitionResponseDtoGroupedBySource = BlockchainHelper.getAllContractDefinitionsFromSmartContractGroupedBySource();



        monitor.info(format("[%s] fetched %s Contracts from Smart Contract", this.getClass().getSimpleName(), contractDefinitionResponseDtoList.size()));

        // iterate over all sources of contractDefinitionResponseDtoGroupedBySource and fetch all contracts for each source
        for (Map.Entry<String, List<ContractDefinitionResponseDto>> entry : contractDefinitionResponseDtoGroupedBySource.entrySet()) {
        	monitor.info(format("[%s] fetching contracts for source %s", this.getClass().getSimpleName(), entry.getKey()));

            List<ContractOffer> contractOfferList = new ArrayList<>();
        	List<ContractDefinitionResponseDto> contractDefinitionResponseDtoListForSource = entry.getValue();

        	// iterate over all contracts for a source
        	for (ContractDefinitionResponseDto contract : contractDefinitionResponseDtoListForSource) {

        		monitor.info(format("[%s] fetching contract %s", this.getClass().getSimpleName(), contract.getId()));

                ContractOffer contractOffer = getContractOfferFromContractDefinitionDto(contract, assetEntryDtoList, policyDefinitionResponseDtoList);
        		contractOfferList.add(contractOffer);
        	}

            Catalog catalog = Catalog.Builder.newInstance().contractOffers(contractOfferList).id(UUID.randomUUID().toString()).build();
            var updateResponse = new UpdateResponse(entry.getKey(), catalog);
            monitor.info("Trying now to artifically insert newly created multiple contract offers from " + entry.getKey() + " : " + catalog.getContractOffers().size());
            successHandler.accept(updateResponse);
        }



        contractOfferListFromStorage = (List<ContractOffer>) store.query(queryList);
        // count number of contract offers with unique contract.provider URIs
        int count = (int) contractOfferListFromStorage.stream().map(ContractOffer::getProvider).distinct().count();
        monitor.info(format("Fetched %d offers by %d unique providers from local cataloge storage", contractOfferListFromStorage.size(), count));


    }

    /**
     * Connecting the actual Asset Objects with Policy Objects to the ContractOffer Object
     * @param contract the contract to be created
     * @param assetEntryDtoList the list of all existing assets in the blockchain
     * @param policyDefinitionResponseDtoList the list of all existing policies in the blockchain
     * @return ContractOffer created from combining the Asset and Policy Objects identified by the ids in the ContractDefinitionResponseDto, null if not found
     */
    private ContractOffer getContractOfferFromContractDefinitionDto(ContractDefinitionResponseDto contract, List<AssetEntryDto> assetEntryDtoList, List<PolicyDefinitionResponseDto> policyDefinitionResponseDtoList) {
        String assetId = String.valueOf(contract.getCriteria().get(0).getOperandRight());
        if(contract.getCriteria().get(0).getOperandRight() instanceof ArrayList) {
            assetId =  String.valueOf(((ArrayList<?>)contract.getCriteria().get(0).getOperandRight()).get(0)); // WHAT THE FUCK WARUM MUSS ICH DAS HIER TUN WENN DIE ÜBER DAS EDC DASHBOARD ERSTELLT WERDEN?!?=?!
        }
        String policyId = contract.getContractPolicyId();

        AssetEntryDto assetEntryDto = null;
        PolicyDefinitionResponseDto policyDefinitionResponseDto = null;

        assetEntryDto = getAssetById(assetId, assetEntryDtoList);

        policyDefinitionResponseDto = getPolicyById(policyId, policyDefinitionResponseDtoList);



        if(assetEntryDto == null || policyDefinitionResponseDto == null) {
            monitor.severe(String.format("[%s] Not able to find the Asset with id %s or policy with id %s for the contract %s", this.getClass().getSimpleName(), assetId, policyId, contract.getId()));
            return null;
        }

        Asset asset = Asset.Builder.newInstance().id(assetEntryDto.getAsset().getId()).properties(assetEntryDto.getAsset().getProperties()).build();
        Policy policy = Policy.Builder.newInstance()
                .target(policyDefinitionResponseDto.getPolicy().getTarget())
                .assignee(policyDefinitionResponseDto.getPolicy().getAssignee())
                .assigner(policyDefinitionResponseDto.getPolicy().getAssigner())
                .prohibitions(policyDefinitionResponseDto.getPolicy().getProhibitions())
                .permissions(policyDefinitionResponseDto.getPolicy().getPermissions())
                .extensibleProperties(policyDefinitionResponseDto.getPolicy().getExtensibleProperties())
                .duties(policyDefinitionResponseDto.getPolicy().getObligations()).build();
        return  ContractOffer.Builder.newInstance().asset(asset).policy(policy).id(contract.getId()).build();
    }

    private PolicyDefinitionResponseDto getPolicyById(String policyId, List<PolicyDefinitionResponseDto> policyDefinitionResponseDtoList) {
        for (PolicyDefinitionResponseDto p: policyDefinitionResponseDtoList) {
            if(p.getId().equals(policyId)) {
                return p;
            }
        }
        return null;
    }

    private AssetEntryDto getAssetById(String assetId, List<AssetEntryDto> assetEntryDtoList) {
        for (AssetEntryDto a: assetEntryDtoList) {
            if(a.getAsset().getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }

    private void runPostExecution() {
        if (postExecutionTask != null) {
            try {
                postExecutionTask.run();
            } catch (Throwable thr) {
                monitor.severe("Error running post execution task", thr);
            }
        }
    }

    private void runPreExecution() {
        if (preExecutionTask != null) {
            try {
                preExecutionTask.run();
            } catch (Throwable thr) {
                monitor.severe("Error running pre execution task", thr);
            }
        }
    }

    @NotNull
    private ArrayBlockingQueue<CatalogCrawler> createCrawlers(CrawlerErrorHandler errorHandler, int numCrawlers) {
        return new ArrayBlockingQueue<>(numCrawlers, true, IntStream.range(0, numCrawlers).mapToObj(i -> new CatalogCrawler(monitor, errorHandler, successHandler)).collect(Collectors.toList()));
    }

    private List<WorkItem> fetchWorkItems() {
        // use all nodes EXCEPT self
        return directory.getAll().stream().filter(nodeFilter).map(n -> new WorkItem(n.getTargetUrl(), selectProtocol(n.getSupportedProtocols()))).collect(Collectors.toList());
    }

    private String selectProtocol(List<String> supportedProtocols) {
        //just take the first matching one.
        return supportedProtocols.isEmpty() ? null : supportedProtocols.get(0);
    }

    @NotNull
    private CrawlerErrorHandler createErrorHandlers(Monitor monitor, Queue<WorkItem> workItems) {
        return workItem -> {
            if (workItem.getErrors().size() > 7) {
                monitor.severe(message(format("The following workitem has errored out more than 5 times. We'll discard it now: [%s]", workItem)));
            } else {
                var random = new Random();
                var to = 5 + random.nextInt(20);
                monitor.debug(message(format("The following work item has errored out. Will re-queue after a small delay: [%s]", workItem)));
                Executors.newSingleThreadScheduledExecutor().schedule(() -> workItems.offer(workItem), to, TimeUnit.SECONDS);
            }
        };
    }

    private String message(String input) {
        return "[BlockchainExecutionManager]: " + input;
    }


    public static final class Builder {

        private final BlockchainExecutionManager instance;

        private Builder() {
            instance = new BlockchainExecutionManager();
        }

        public static BlockchainExecutionManager.Builder newInstance() {
            return new BlockchainExecutionManager.Builder();
        }

        public BlockchainExecutionManager.Builder monitor(Monitor monitor) {
            instance.monitor = monitor;
            return this;
        }

        public BlockchainExecutionManager.Builder preExecutionTask(Runnable preExecutionTask) {
            instance.preExecutionTask = preExecutionTask;
            return this;
        }

        public BlockchainExecutionManager.Builder numCrawlers(int numCrawlers) {
            instance.numCrawlers = numCrawlers;
            return this;
        }

        public BlockchainExecutionManager.Builder postExecutionTask(Runnable postExecutionTask) {
            instance.postExecutionTask = postExecutionTask;
            return this;
        }

        public BlockchainExecutionManager.Builder nodeQueryAdapterRegistry(NodeQueryAdapterRegistry registry) {
            instance.nodeQueryAdapterRegistry = registry;
            return this;
        }

        public BlockchainExecutionManager.Builder nodeDirectory(FederatedCacheNodeDirectory directory) {
            instance.directory = directory;
            return this;
        }

        public BlockchainExecutionManager.Builder nodeFilterFunction(FederatedCacheNodeFilter filter) {
            instance.nodeFilter = filter;
            return this;
        }

        public BlockchainExecutionManager.Builder onSuccess(CrawlerSuccessHandler successConsumer) {
            instance.successHandler = successConsumer;
            return this;
        }

        public BlockchainExecutionManager build() {
            Objects.requireNonNull(instance.monitor, "BlockchainExecutionManager.Builder: Monitor cannot be null");
            Objects.requireNonNull(instance.nodeQueryAdapterRegistry, "BlockchainExecutionManager.Builder: nodeQueryAdapterRegistry cannot be null");
            Objects.requireNonNull(instance.directory, "BlockchainExecutionManager.Builder: nodeDirectory cannot be null");
            return instance;
        }

        public BlockchainExecutionManager.Builder nodeStore(FederatedCacheStore store) {
            instance.store = store;
            return this;
        }
    }

}
