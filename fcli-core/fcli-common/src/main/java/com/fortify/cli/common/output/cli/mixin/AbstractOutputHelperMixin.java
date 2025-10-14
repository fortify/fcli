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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.product.NoOpProductHelper;
import com.fortify.cli.common.output.writer.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.writer.IOutputWriterFactorySupplier;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.util.JavaHelper;

import picocli.CommandLine.Mixin;

public abstract class AbstractOutputHelperMixin implements IOutputHelper {
    @Mixin
    private CommandHelperMixin commandHelper;

    // TODO This should move to TransformationPipelineConfigFactoryMixin (after rename)
    public IProductHelper getProductHelper() {
        return commandHelper.getCommandAs(IProductHelperSupplier.class).map(IProductHelperSupplier::getProductHelper)
                .orElse(NoOpProductHelper.instance());
    }

    /**
     * This default implementation of {@link IOutputHelper#getBasicOutputConfig()}
     * tries to retrieve a basic output configuration from the command being
     * invoked, if it implements the {@link IBasicOutputConfigSupplier} interface.
     * If the command doesn't implement this interface, or if the
     * {@link IBasicOutputConfigSupplier#getBasicOutputConfig()} method returns
     * null, this method throws an exception. Note that most concrete
     * {@link IOutputHelper} implementations will override this method; this default
     * implementation is mostly used for {@link OutputHelperMixins.Other}.
     */
    @Override
    public StandardOutputConfig getBasicOutputConfig() {
        Object cmd = commandHelper.getCommand();
        return applyWithDefaultSupplier(cmd, IBasicOutputConfigSupplier.class, IBasicOutputConfigSupplier::getBasicOutputConfig, () -> {
            throw new FcliBugException(cmd.getClass().getName()
                    + " must implement IBasicOutputConfigSupplier, or use an IOutputHelper implementation that provides a basic output configuration");
        });
    }

    /**
     * This default implementation of {@link IOutputHelper#getOutputWriterFactory()}
     * tries to retrieve an {@link IOutputWriterFactory} instance from the command
     * being invoked, if it implements the {@link IOutputWriterFactorySupplier}
     * interface. If the command doesn't implement this interface, or if the
     * {@link IOutputWriterFactorySupplier#getOutputWriterFactory()} method returns
     * null, this method throws an exception. Note that most concrete
     * {@link IOutputHelper} implementations will override this method; this default
     * implementation is mostly used for {@link OutputHelperMixins.Other}.
     */
    @Override
    public IOutputWriterFactory getOutputWriterFactory() {
        Object cmd = commandHelper.getCommand();
        return applyWithDefaultSupplier(cmd, IOutputWriterFactorySupplier.class, IOutputWriterFactorySupplier::getOutputWriterFactory,
                () -> {
                    throw new FcliBugException(cmd.getClass().getName()
                            + " must implement IOutputWriterFactorySupplier, or use an IOutputHelper implementation that provides an output factory");
                });
    }

    /**
     * Utility method for applying the given function on the given object and
     * returning the result, if the given object is an instance of the given type.
     * If the given object is not of the given type, or if the provided function
     * returns null, this method returns the value returned by the given
     * defaultValueSupplier if it is not null. Otherwise, this method returns null.
     *
     * @param <T>
     * @param <R>
     * @param obj
     * @param type
     * @param function
     * @param defaultValueSupplier
     * @return
     */
    private static final <T, R> R applyWithDefaultSupplier(Object obj, Class<T> type, Function<T, R> function, Supplier<R> defaultValueSupplier) {
        var result = JavaHelper.as(obj, type).map(function);
        if (defaultValueSupplier != null) { result = result.or(() -> Optional.of(defaultValueSupplier.get())); }
        return result.orElse(null);
    }
}
