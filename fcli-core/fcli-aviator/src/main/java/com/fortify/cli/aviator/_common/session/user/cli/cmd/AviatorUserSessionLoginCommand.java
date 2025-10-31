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
package com.fortify.cli.aviator._common.session.user.cli.cmd;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionLoginOptions;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionNameArgGroup;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserTokenResolverMixin;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionHelper;
import com.fortify.cli.aviator._common.util.AviatorJwtUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.grpc.token.TokenValidationResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Login.CMD_NAME, sortOptions = false)
public class AviatorUserSessionLoginCommand extends AbstractSessionLoginCommand<AviatorUserSessionDescriptor> {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorUserSessionLoginCommand.class);

    @Mixin @Getter private OutputHelperMixins.Login outputHelper;
    @Getter private AviatorUserSessionHelper sessionHelper = AviatorUserSessionHelper.instance();
    @Mixin @Getter private AviatorUserTokenResolverMixin tokenResolver;
    @Mixin private AviatorUserSessionLoginOptions sessionLoginOptions = new AviatorUserSessionLoginOptions();
    @Getter @ArgGroup(headingKey = "aviator.user-session.name.arggroup")
    private AviatorUserSessionNameArgGroup sessionNameSupplier;

    @Override
    protected void logoutBeforeNewLogin(String sessionName, AviatorUserSessionDescriptor sessionDescriptor) {}

    @Override
    protected AviatorUserSessionDescriptor login(String sessionName) {
        String resolvedToken = tokenResolver.getToken();
        Date expiryDate = AviatorJwtUtils.extractExpiryDateFromToken(resolvedToken);
        String tenantName = AviatorJwtUtils.extractTenantNameFromToken(resolvedToken);

        LOG.info("Default Aviator admin configuration found. Attempting to validate user token...");
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionLoginOptions.getAviatorUrl())) {

            TokenValidationResponse validationResponse = client.validateUserToken(resolvedToken, tenantName);

            if (!validationResponse.getValid()) {
                String errorMsg = validationResponse.getErrorMessage();
                String fullError = "Aviator user token validation failed: " +
                        (errorMsg == null || errorMsg.isBlank() ? "The token is invalid. Please verify the token is correct and try again." : errorMsg);                throw new AviatorSimpleException(fullError);
            }
            LOG.info("Aviator user token validated successfully with the Aviator server.");
        } catch (AviatorTechnicalException e) {
            if (e.getCause() instanceof StatusRuntimeException sre && sre.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                LOG.warn("WARN: Could not validate token with the Aviator server; this may be an older version that does not support this feature. " +
                        "Proceeding with session creation, but the token's server-side validity is unconfirmed.");
                LOG.debug("Token validation gRPC call exception details: ", e);
            } else {
                throw e;
            }
        }

        if (expiryDate == null) {
            LOG.warn("WARN: Could not extract expiry date from the provided token. The session may not have an accurate expiration time.");
        }

        return AviatorUserSessionDescriptor.builder()
                .aviatorUrl(sessionLoginOptions.getAviatorUrl())
                .aviatorToken(resolvedToken)
                .expiryDate(expiryDate)
                .build();
    }
}