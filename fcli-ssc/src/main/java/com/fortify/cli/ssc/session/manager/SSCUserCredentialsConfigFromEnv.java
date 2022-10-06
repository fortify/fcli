package com.fortify.cli.ssc.session.manager;

import java.time.OffsetDateTime;

import com.fortify.cli.common.rest.runner.config.UserCredentialsConfigFromEnv;
import com.fortify.cli.ssc.util.SSCConstants;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper=true) @ToString(callSuper=true)
public final class SSCUserCredentialsConfigFromEnv extends UserCredentialsConfigFromEnv implements ISSCUserCredentialsConfig {
    public SSCUserCredentialsConfigFromEnv() {
        super(SSCConstants.PRODUCT_ENV_ID);
    }

    @Override
    public OffsetDateTime getExpiresAt() {
        return null;
    }
}