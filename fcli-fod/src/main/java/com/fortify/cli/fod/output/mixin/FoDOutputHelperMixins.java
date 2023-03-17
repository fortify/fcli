package com.fortify.cli.fod.output.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins.FoDProductHelper;
import com.fortify.cli.fod.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod.rest.helper.FoDPagingHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>This class provides standard, FoD-specific {@link IUnirestOutputHelper} implementations,
 * replicating the product-agnostic {@link IUnirestOutputHelper} implementations provided in
 * {@link UnirestOutputHelperMixins}, adding product-specific functionality through the
 * {@link ProductHelperClass} annotation on this enclosing class. In addition to the
 * {@link IUnirestOutputHelper} implementations provided by the common {@link UnirestOutputHelperMixins},
 * this class may define some additional implementations specific for FoD.</p>
 *
 * @author rsenden
 */
@ProductHelperClass(FoDProductHelper.class)
public class FoDOutputHelperMixins {
    @ReflectiveAccess
    public static class FoDProductHelper implements IProductHelper, IInputTransformerSupplier, INextPageUrlProducerSupplier {
        @Getter @Setter private IUnirestOutputHelper outputHelper;
        @Getter private UnaryOperator<JsonNode> inputTransformer = FoDInputTransformer::getItems;

        @Override
        public INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
            return FoDPagingHelper.nextPageUrlProducer(originalRequest);
        }
    }

     public static class Create
               extends UnirestOutputHelperMixins.Create {}

     public static class Delete
               extends UnirestOutputHelperMixins.Delete {}

     public static class List
               extends UnirestOutputHelperMixins.List {}

     public static class Get
               extends UnirestOutputHelperMixins.Get {}

     public static class Set
               extends UnirestOutputHelperMixins.Set {}

     public static class Update
               extends UnirestOutputHelperMixins.Update {}

     public static class Enable
               extends UnirestOutputHelperMixins.Enable {}

     public static class Disable
               extends UnirestOutputHelperMixins.Disable {}

     public static class Start
               extends UnirestOutputHelperMixins.Start {}

     public static class Pause
               extends UnirestOutputHelperMixins.Pause {}

     public static class Resume
               extends UnirestOutputHelperMixins.Resume {}

     public static class Cancel
               extends UnirestOutputHelperMixins.Cancel {}

     public static class WaitFor
            extends BasicOutputHelperMixins.WaitFor {}

     public static class Upload
               extends UnirestOutputHelperMixins.Upload {}

     public static class Download
               extends UnirestOutputHelperMixins.Download {}

     public static class Install
               extends UnirestOutputHelperMixins.Install {}

     public static class Uninstall
               extends UnirestOutputHelperMixins.Uninstall {}

     public static class Import
            extends UnirestOutputHelperMixins.Import {}

     public static class Export
            extends UnirestOutputHelperMixins.Export {}

     public static class Setup
            extends UnirestOutputHelperMixins.Setup {}

     public static class ListSast extends UnirestOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-sast";
    }
     public static class ListDast extends UnirestOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-dast";
    }
     public static class ListOss extends UnirestOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-oss";
    }
     public static class ListMobile extends UnirestOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "list-mobile";
    }

     public static class SetupSast extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-sast";
    }
     public static class SetupDast extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-dast";
    }
     public static class SetupOss extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-oss";
    }
     public static class SetupMobile extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "setup-mobile";
    }

     public static class StartSast extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-sast";
    }
     public static class StartDast extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-dast";
    }
     public static class StartOss extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-oss";
    }
     public static class StartMobile extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "start-mobile";
    }

     public static class ImportSast extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-sast";
    }
     public static class ImportDast extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-dast";
    }
     public static class ImportOss extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-oss";
    }
     public static class ImportMobile extends UnirestOutputHelperMixins.TableNoQuery {
        public static final String CMD_NAME = "import-mobile";
    }

}
