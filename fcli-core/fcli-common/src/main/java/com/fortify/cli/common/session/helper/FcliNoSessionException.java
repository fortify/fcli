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
package com.fortify.cli.common.session.helper;

import java.util.regex.Pattern;

import com.fortify.cli.common.exception.FcliSimpleException;

public class FcliNoSessionException extends FcliSessionException {
    private static final long serialVersionUID = 1L;
    private static final String LOGIN_MSG_FMT = "Please log in using the '%s' command";
    private static final String LOGIN_CMD_REGEX = LOGIN_MSG_FMT.replace("%s", "(.*)");
    private static final Pattern LOGIN_CMD_PATTERN = Pattern.compile(LOGIN_CMD_REGEX);

    public FcliNoSessionException(String loginCmd, String fmt, Object... args) { 
        super(createMessage(loginCmd, fmt, args));
    }

    private static String createMessage(String loginCmd, String fmt, Object[] args) {
        var baseMsg = String.format(fmt, args);
        return String.format("%s\nPlease log in using the '%s' command", baseMsg, loginCmd);
    }
    
    public static final String getLoginCmd(String s) {
        var matcher = LOGIN_CMD_PATTERN.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new FcliSimpleException("Cannot extract login command from message: "+s);
    }
}
