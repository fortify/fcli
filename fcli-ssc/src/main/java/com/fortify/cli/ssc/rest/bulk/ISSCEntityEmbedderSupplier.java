package com.fortify.cli.ssc.rest.bulk;

/**
 * Interface for supplying an {@link ISSCEntityEmbedder} instance.
 * @author rsenden
 */
@FunctionalInterface
public interface ISSCEntityEmbedderSupplier {
    ISSCEntityEmbedder createEntityEmbedder();
}
