package io.quarkiverse.roq.frontmatter.deployment.record;

import static io.quarkiverse.roq.util.PathUtils.removeTrailingSlash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.roq.frontmatter.deployment.RoqFrontMatterOutputBuildItem;
import io.quarkiverse.roq.frontmatter.deployment.RoqFrontMatterRootUrlBuildItem;
import io.quarkiverse.roq.frontmatter.deployment.publish.RoqFrontMatterPublishDerivedCollectionBuildItem;
import io.quarkiverse.roq.frontmatter.deployment.publish.RoqFrontMatterPublishDocumentPageBuildItem;
import io.quarkiverse.roq.frontmatter.deployment.publish.RoqFrontMatterPublishPageBuildItem;
import io.quarkiverse.roq.frontmatter.runtime.RoqFrontMatterRecorder;
import io.quarkiverse.roq.frontmatter.runtime.config.RoqSiteConfig;
import io.quarkiverse.roq.frontmatter.runtime.model.*;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;

class RoqFrontMatterInitProcessor {
    private static final Logger LOGGER = Logger.getLogger(RoqFrontMatterInitProcessor.class);

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void bindCollections(
            RoqFrontMatterRootUrlBuildItem rootUrlItem,
            List<RoqFrontMatterPublishDocumentPageBuildItem> documents,
            List<RoqFrontMatterPublishDerivedCollectionBuildItem> generatedCollections,
            BuildProducer<RoqFrontMatterCollectionBuildItem> collectionsProducer,
            BuildProducer<RoqFrontMatterPageBuildItem> pagesProducer,
            RoqFrontMatterRecorder recorder) {
        if (rootUrlItem == null) {
            return;
        }

        // Published collections
        final Map<String, List<RoqFrontMatterPublishDocumentPageBuildItem>> byCollection = documents.stream()
                .collect(Collectors.groupingBy(i -> i.collection().id()));
        final Map<String, Supplier<DocumentPage>> documentsById = new HashMap<>();
        for (Map.Entry<String, List<RoqFrontMatterPublishDocumentPageBuildItem>> e : byCollection.entrySet()) {
            List<Supplier<DocumentPage>> docs = new ArrayList<>();
            for (RoqFrontMatterPublishDocumentPageBuildItem item : e.getValue()) {
                final RoqUrl url = item.url();
                final Supplier<DocumentPage> document = recorder.createDocument(item.collection().id(),
                        url,
                        item.info(), item.data(), item.collection().hidden());
                documentsById.put(item.info().id(), document);
                pagesProducer
                        .produce(new RoqFrontMatterPageBuildItem(item.info().id(), url, item.collection().hidden(), document));
                docs.add(document);
            }
            collectionsProducer.produce(new RoqFrontMatterCollectionBuildItem(e.getKey(), docs));
        }

        // Derived collections (referencing existing documents)
        for (RoqFrontMatterPublishDerivedCollectionBuildItem i : generatedCollections) {
            List<Supplier<DocumentPage>> docs = new ArrayList<>();
            for (String id : i.documentIds()) {
                final Supplier<DocumentPage> doc = documentsById.get(id);
                if (doc == null) {
                    throw new IllegalStateException("No document found for id " + id);
                }
                docs.add(doc);
            }
            collectionsProducer.produce(new RoqFrontMatterCollectionBuildItem(i.collection(), docs));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void bindPages(
            RoqFrontMatterRootUrlBuildItem rootUrlItem,
            List<RoqFrontMatterPublishPageBuildItem> pages,
            BuildProducer<RoqFrontMatterNormalPageBuildItem> normalPagesProducer,
            BuildProducer<RoqFrontMatterPageBuildItem> pagesProducer,
            BuildProducer<RoqFrontMatterIndexPageBuildItem> indexPageProducer,
            RoqFrontMatterRecorder recorder) {
        if (rootUrlItem == null) {
            return;
        }
        for (RoqFrontMatterPublishPageBuildItem page : pages) {
            final Supplier<NormalPage> recordedPage = recorder.createPage(page.url(),
                    page.info(), page.data(), page.paginator());
            pagesProducer.produce(new RoqFrontMatterPageBuildItem(page.info().id(), page.url(), false, recordedPage));
            normalPagesProducer
                    .produce(new RoqFrontMatterNormalPageBuildItem(page.info().id(), page.url(), recordedPage));
            if (page.info().id().startsWith("index.")) {
                indexPageProducer.produce(new RoqFrontMatterIndexPageBuildItem(recordedPage));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    RoqFrontMatterOutputBuildItem bindSite(
            LaunchModeBuildItem launchMode,
            RoqFrontMatterRootUrlBuildItem rootUrlItem,
            RoqFrontMatterIndexPageBuildItem indexPageItem,
            List<RoqFrontMatterCollectionBuildItem> collectionItems,
            List<RoqFrontMatterPageBuildItem> pageItems,
            List<RoqFrontMatterNormalPageBuildItem> normalPageItems,
            BuildProducer<SyntheticBeanBuildItem> beansProducer,
            RoqFrontMatterRecorder recorder) {
        if (rootUrlItem == null) {
            return null;
        }
        // Create Site bean
        if (indexPageItem == null) {
            throw new RuntimeException("Roq site must declare an index.html page");
        }
        final Map<String, List<Supplier<DocumentPage>>> collectionsMap = collectionItems.stream().collect(
                Collectors.toMap(RoqFrontMatterCollectionBuildItem::name, RoqFrontMatterCollectionBuildItem::documents));
        final Supplier<RoqCollections> collectionsSupplier = recorder.createRoqCollections(collectionsMap);
        final List<Supplier<NormalPage>> pages = normalPageItems.stream()
                .map(RoqFrontMatterNormalPageBuildItem::page)
                .toList();
        final Supplier<Site> siteSupplier = recorder.createSite(rootUrlItem.rootUrl(), indexPageItem.page(), pages,
                collectionsSupplier);
        beansProducer.produce(SyntheticBeanBuildItem.configure(Site.class)
                .named("site")
                .scope(Singleton.class)
                .unremovable()
                .supplier(siteSupplier)
                .done());
        Map<String, RoqFrontMatterPageBuildItem> itemsMap = new HashMap<>();
        Map<String, Supplier<? extends Page>> allPagesByPath = new HashMap<>();
        for (RoqFrontMatterPageBuildItem i : pageItems) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Published %s page '%s' on '%s'", i.hidden() ? "hidden" : "", i.id(), i.url().toString());
            }
            if (i.hidden()) {
                continue;
            }
            final RoqFrontMatterPageBuildItem prev = itemsMap.put(i.url().path(), i);
            allPagesByPath.put(i.url().path(), i.page());
            if (prev != null) {
                if (launchMode.getLaunchMode().isDevOrTest()) {
                    LOGGER.warnf("The same url (%s) has been found more than once in %s and %s", i.url().path(), prev.id(),
                            i.id());
                } else {
                    throw new RuntimeException("The same url (%s) has been found more than once in %s and %s"
                            .formatted(i.url().path(), prev.id(), i.id()));
                }
            }
        }
        return new RoqFrontMatterOutputBuildItem(allPagesByPath);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    public RouteBuildItem produceRoute(RoqSiteConfig config, RoqFrontMatterRecorder recorder,
            HttpRootPathBuildItem httpRootPath, RoqFrontMatterOutputBuildItem roqFrontMatterOutput) {
        if (roqFrontMatterOutput == null || roqFrontMatterOutput.allPagesByPath().isEmpty()) {
            // There are no templates to serve
            return null;
        }
        return httpRootPath.routeBuilder()
                .routeFunction(httpRootPath.relativePath(removeTrailingSlash(config.rootPath()) + "/*"),
                        recorder.initializeRoute())
                .handlerType(HandlerType.BLOCKING)
                .handler(recorder.handler(httpRootPath.getRootPath(), roqFrontMatterOutput.allPagesByPath()))
                .build();
    }
}
