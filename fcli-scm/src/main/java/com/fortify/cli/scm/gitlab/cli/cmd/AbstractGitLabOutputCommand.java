package com.fortify.cli.scm.gitlab.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.scm.gitlab.cli.mixin.GitLabProductHelperMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractGitLabOutputCommand extends AbstractOutputCommand 
    implements IProductHelperSupplier
{
    @Getter @Mixin GitLabProductHelperMixin productHelper;
}
