package com.fortify.cli.common.cli.util;

import com.fortify.cli.common.cli.util.FortifyCLIInitializerRunner.FortifyCLIInitializerCommand;

public interface IFortifyCLIInitializer {
    void initializeFortifyCLI(FortifyCLIInitializerCommand initializerCommand);
}
