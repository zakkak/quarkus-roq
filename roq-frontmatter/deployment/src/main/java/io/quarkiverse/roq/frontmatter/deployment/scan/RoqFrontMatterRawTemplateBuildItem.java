package io.quarkiverse.roq.frontmatter.deployment.scan;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.quarkiverse.roq.frontmatter.runtime.model.PageInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.json.JsonObject;

/**
 * A build item representing a Roq FM file.
 * This template is just extracted from the disk, data is not yet merged with layouts.
 *
 * Use {@link io.quarkiverse.roq.frontmatter.deployment.data.RoqFrontMatterTemplateBuildItem} to read all FM templates with
 * merged data.
 */
public final class RoqFrontMatterRawTemplateBuildItem extends MultiBuildItem {

    /**
     * The page details.
     */
    private final PageInfo info;

    /**
     * The layout used for this template.
     */
    private final String layout;

    /**
     * true if it's a page template.
     * or false if it's an include template.
     * <p>
     * Include template should not be published.
     */
    private final boolean isPage;

    /**
     * The FrontMatter data (it is not merged with parents at this stage).
     */
    private final JsonObject data;

    private final String collection;

    /**
     * The generated template content to be passed to be passed to Qute.
     */
    private final String generatedTemplate;

    /**
     * Should this template be published.
     */
    private final boolean published;

    /**
     * List of aliases to be used for redirecting to an url.
     */
    private final List<String> aliases;

    public RoqFrontMatterRawTemplateBuildItem(PageInfo info, String layout, boolean isPage, JsonObject data, String collection,
            String generatedTemplate,
            boolean published,
            List<String> aliases) {
        this.info = info;
        this.layout = layout;
        this.isPage = isPage;
        this.data = data;
        this.collection = collection;
        this.generatedTemplate = generatedTemplate;
        this.published = published;
        this.aliases = Objects.requireNonNullElseGet(aliases, Collections::emptyList);
    }

    public boolean isPage() {
        return isPage;
    }

    public String id() {
        return info.id();
    }

    public PageInfo info() {
        return info;
    }

    public String layout() {
        return layout;
    }

    public JsonObject data() {
        return data;
    }

    public String collection() {
        return collection;
    }

    public String generatedContent() {
        return generatedTemplate;
    }

    public boolean published() {
        return published;
    }

    public List<String> aliases() {
        return Collections.unmodifiableList(aliases);
    }
}
