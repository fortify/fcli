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
