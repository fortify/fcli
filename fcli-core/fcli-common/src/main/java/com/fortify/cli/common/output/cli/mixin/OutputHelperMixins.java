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
package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Provides standard {@link IOutputHelper} mixins for output formatting and query support.
 * <p>
 * Mixins are grouped by output format and query support:
 * <ul>
 *   <li>{@link TableNoQuery}: Table output, no query support</li>
 *   <li>{@link TableWithQuery}: Table output, with query support</li>
 *   <li>{@link DetailsNoQuery}: Details output, no query support</li>
 *   <li>{@link DetailsWithQuery}: Details output, with query support</li>
 * </ul>
 * Each command should use the appropriate mixin (where {@code CMD_NAME} matches the command name),
 * either from the standard implementations provided here or from product-specific implementations.
 * <p>
 * Example usage:
 * <pre>
 * &#64;Command(name = OutputHelperMixins.List.CMD_NAME)
 * public class SomeListCommand extends AbstractMyProductOutputCommand implements IBaseHttpRequestSupplier {
 *     &#64;Getter &#64;Mixin private OutputHelperMixins.List outputHelper;
 *     ...
 * }
 * </pre>
 * <p>
 * Individual product modules may provide additional output helper mixins as needed.
 *
 * @author rsenden
 */
public class OutputHelperMixins {
    public static class TableWithQuery extends AbstractOutputHelperMixin {
        @Mixin private QueryOptionMixin queryOptionMixin = new QueryOptionMixin();
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table();
    }

    public static class TableNoQuery extends AbstractOutputHelperMixin {
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.table();
    }

    public static class DetailsNoQuery extends AbstractOutputHelperMixin {
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.details();
    }

    public static class DetailsWithQuery extends AbstractOutputHelperMixin {
        @Mixin private QueryOptionMixin queryOptionMixin = new QueryOptionMixin();
        @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
        @Getter private StandardOutputConfig basicOutputConfig = StandardOutputConfig.details();
    }

    public static class Add extends TableNoQuery {
        public static final String CMD_NAME = "add";
    }

    public static class Create extends TableNoQuery {
        public static final String CMD_NAME = "create";
    }
    public static class CreateWithDetailsOutput extends DetailsNoQuery {
        public static final String CMD_NAME = "create";
    }

    public static class CreateConfig extends TableNoQuery {
        public static final String CMD_NAME = "create-config";
    }

    public static class CreateTemplate extends TableNoQuery {
        public static final String CMD_NAME = "create-template";
    }

    public static class CreateTemplateConfig extends TableNoQuery {
        public static final String CMD_NAME = "create-template-config";
    }

    @Command(aliases = {"rm"})
    public static class Delete extends TableNoQuery {
        public static final String CMD_NAME = "delete";
    }

    @Command(aliases = {"rmt"})
    public static class DeleteTemplate extends TableNoQuery {
        public static final String CMD_NAME = "delete-template";
    }

    public static class Clear extends TableNoQuery {
        public static final String CMD_NAME = "clear";
    }

    public static class Revoke extends TableNoQuery {
        public static final String CMD_NAME = "revoke";
    }

    @Command(aliases = {"ls"})
    public static class List extends TableWithQuery {
        public static final String CMD_NAME = "list";
    }

    @Command(aliases = {"ls"})
    public static class ListNoQuery extends TableNoQuery {
        public static final String CMD_NAME = "list";
    }

    @Command(aliases = {"lsd"})
    public static class ListDefinitions extends TableWithQuery {
        public static final String CMD_NAME = "list-definitions";
    }

    @Command(aliases = {"lst"})
    public static class ListTemplates extends TableWithQuery {
        public static final String CMD_NAME = "list-templates";
    }

    public static class Get extends DetailsNoQuery {
        public static final String CMD_NAME = "get";
    }

    public static class GetDefinition extends DetailsNoQuery {
        public static final String CMD_NAME = "get-definition";
    }

    public static class GetTemplate extends DetailsNoQuery {
        public static final String CMD_NAME = "get-template";
    }

    public static class Status extends TableNoQuery {
        public static final String CMD_NAME = "status";
    }

    public static class Set extends TableNoQuery {
        public static final String CMD_NAME = "set";
    }

    public static class Update extends TableNoQuery {
        public static final String CMD_NAME = "update";
    }

    public static class UpdateTemplate extends TableNoQuery {
        public static final String CMD_NAME = "update-template";
    }

    public static class Enable extends TableNoQuery {
        public static final String CMD_NAME = "enable";
    }

    public static class Disable extends TableNoQuery {
        public static final String CMD_NAME = "disable";
    }

    public static class Start extends TableNoQuery {
        public static final String CMD_NAME = "start";
    }

    public static class Pause extends TableNoQuery {
        public static final String CMD_NAME = "pause";
    }

    public static class Resume extends TableNoQuery {
        public static final String CMD_NAME = "resume";
    }

    public static class Cancel extends TableNoQuery {
        public static final String CMD_NAME = "cancel";
    }

    public static class Upload extends TableNoQuery {
        public static final String CMD_NAME = "upload";
    }

    public static class Download extends TableNoQuery {
        public static final String CMD_NAME = "download";
    }

    public static class DownloadTemplate extends TableNoQuery {
        public static final String CMD_NAME = "download-template";
    }

    public static class Install extends TableNoQuery {
        public static final String CMD_NAME = "install";
    }

    public static class Uninstall extends TableNoQuery {
        public static final String CMD_NAME = "uninstall";
    }

    public static class Import extends TableNoQuery {
        public static final String CMD_NAME = "import";
    }

    public static class Export extends TableNoQuery {
        public static final String CMD_NAME = "export";
    }

    public static class Setup extends TableNoQuery {
        public static final String CMD_NAME = "setup";
    }

    public static class WaitFor extends TableNoQuery {
        public static final String CMD_NAME = "wait-for";
    }

    public static class Login extends TableNoQuery {
        public static final String CMD_NAME = "login";
    }

    public static class Logout extends TableNoQuery {
        public static final String CMD_NAME = "logout";
    }

    public static class RestCall extends DetailsWithQuery {
        public static final String CMD_NAME = "call";
    }

}
