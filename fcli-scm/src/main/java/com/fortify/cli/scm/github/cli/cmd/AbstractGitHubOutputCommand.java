package com.fortify.cli.scm.github.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.scm.github.cli.mixin.GitHubProductHelperMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractGitHubOutputCommand extends AbstractOutputCommand 
    implements IProductHelperSupplier
{
    @Getter @Mixin GitHubProductHelperMixin productHelper;
}
