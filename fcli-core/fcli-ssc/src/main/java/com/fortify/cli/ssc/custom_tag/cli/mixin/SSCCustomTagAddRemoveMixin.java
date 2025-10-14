/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
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