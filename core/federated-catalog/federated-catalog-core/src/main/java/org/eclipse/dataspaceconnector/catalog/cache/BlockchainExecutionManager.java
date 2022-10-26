package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.dataspaceconnector.catalog.cache.crawler.CatalogCrawler;
import org.eclipse.dataspaceconnector.catalog.spi.*;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.extensions.listener.BlockchainHelper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
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
        AssetEntryDto assetDto = BlockchainHelper.getAssetWithIdFromSmartContract("20");
        monitor.info(String.format("[%s] fetched Asset %s from Smart Contract", this.getClass().getSimpleName(), assetDto.getAsset().getId()));

        PolicyDefinitionResponseDto policyDto = BlockchainHelper.getPolicyWithIdFromSmartContract("1");
        monitor.info(String.format("[%s] fetched Asset %s from Smart Contract", this.getClass().getSimpleName(), policyDto.getPolicy().getTarget()));

        //ContractDefinitionResponseDto contractDefinitionResponseDto = BlockchainHelper.getAllContractDefinitionsFromSmartContract();

        // var contract = ContractDefinition.Builder.newInstance().contractPolicyId(policy.getId()).accessPolicyId(policy.getId()).selectorExpression(AssetSelectorExpression.SELECT_ALL).build();
        Asset asset = Asset.Builder.newInstance().id(assetDto.getAsset().getId()).properties(assetDto.getAsset().getProperties()).build();
        Policy policy = Policy.Builder.newInstance()
                .target(policyDto.getPolicy().getTarget())
                .assignee(policyDto.getPolicy().getAssignee())
                .assigner(policyDto.getPolicy().getAssigner())
                .prohibitions(policyDto.getPolicy().getProhibitions())
                .permissions(policyDto.getPolicy().getPermissions())
                .extensibleProperties(policyDto.getPolicy().getExtensibleProperties())
                .duties(policyDto.getPolicy().getObligations()).build();

        ContractOffer contractOffer = ContractOffer.Builder.newInstance().asset(asset).policy(policy).id(UUID.randomUUID().toString()).build();
        List<ContractOffer> contractOfferList = new ArrayList<>();
        contractOfferList.add(contractOffer);
        Catalog catalog = Catalog.Builder.newInstance().contractOffers(contractOfferList).id(UUID.randomUUID().toString()).build();
        var updateResponse = new UpdateResponse("localhost", catalog);
        monitor.info("Trying now to artifically insert newly created contraft offer: " + contractOffer.getId());
        successHandler.accept(updateResponse);

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
        return "ExecutionManager: " + input;
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
    }

}
