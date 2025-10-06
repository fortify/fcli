package com.fortify.cli.ssc.custom_tag.cli.mixin;

import java.util.List;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Mixin providing --add-tags and --rm-tags options for specifying custom tags to
 * add to or remove from an entity. The values should be provided
 * as comma-separated list; each entry may be a custom tag name, guid, or id.
 */
public class SSCCustomTagAddRemoveMixin {
    public static abstract class AbstractSSCCustomTagMixin {
        public abstract List<String> getTagSpecs();
    }

    public static class OptionalTagAddOption extends AbstractSSCCustomTagMixin {
        @Option(names = {"--add-tags"}, required = false, split = ",", descriptionKey = "fcli.ssc.custom-tag.add")
        @Getter private List<String> tagSpecs;
    }

    public static class OptionalTagRemoveOption extends AbstractSSCCustomTagMixin {
        @Option(names = {"--rm-tags"}, required = false, split = ",", descriptionKey = "fcli.ssc.custom-tag.rm")
        @Getter private List<String> tagSpecs;
    }
}