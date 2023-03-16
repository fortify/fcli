package com.fortify.cli.ssc.appversion_user.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class SSCAppVersionAuthEntityMixin {
    public static abstract class AbstractSSCAppVersionAuthEntityMixin {
        public abstract String[] getAuthEntitySpecs();
    }
    
    public static class OptionalUserAddOption extends AbstractSSCAppVersionAuthEntityMixin {
        @Option(names = {"--useradd"}, required = false, arity="0..")
        @Getter private String[] authEntitySpecs;
    }
    
    public static class OptionalUserDelOption extends AbstractSSCAppVersionAuthEntityMixin {
        @Option(names = {"--userdel"}, required = false, arity="0..")
        @Getter private String[] authEntitySpecs;
    }
    
    public static class RequiredPositionalParameter extends AbstractSSCAppVersionAuthEntityMixin {
        @Parameters(index = "0..*", arity = "1..*")
        @Getter private String[] authEntitySpecs;
    }
    
    
}
