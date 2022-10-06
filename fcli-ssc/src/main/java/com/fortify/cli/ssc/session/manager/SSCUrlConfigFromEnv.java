package com.fortify.cli.ssc.session.manager;

import com.fortify.cli.common.rest.runner.config.UrlConfigFromEnv;
import com.fortify.cli.ssc.util.SSCConstants;

public class SSCUrlConfigFromEnv extends UrlConfigFromEnv {
    public SSCUrlConfigFromEnv() {
        super(SSCConstants.PRODUCT_ENV_ID);
    }
}
