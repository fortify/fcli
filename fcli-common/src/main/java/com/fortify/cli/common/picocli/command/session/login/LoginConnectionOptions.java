/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.picocli.command.session.login;

import com.fortify.cli.common.rest.data.IConnectionConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure connection options to a remote system
 * Usually this would be included in a {@link SessionLoginCommand} implementation
 * as follows:
 * <pre>
 * {@code
 *   {@literal @}ArgGroup(exclusive = false, multiplicity = "1", heading = "<System> Connection Options:%n")
 *   {@literal @}Getter private LoginConnectionOptions conn;
 * }
 * </pre>
 * @author Ruud Senden
 */
@ReflectiveAccess
public class LoginConnectionOptions implements IConnectionConfig {
	@Option(names = {"--url"}, required = true, order=1)
	@Getter private String url;
	
	@Option(names = {"--proxy-host"}, required = false, order=2)
	@Getter private String proxyHost;
	
	@Option(names = {"--proxy-port"}, required = false, order=3)
	@Getter private Integer proxyPort;
	
	@Option(names = {"--proxy-user"}, required = false, order=4)
	@Getter private String proxyUser;
	
	@Option(names = {"--proxy-password"}, required = false, interactive = true, echo = false, order=5)
	@Getter private char[] proxyPassword;
	
	@Option(names = {"--insecure", "-k"}, required = false, description = "Disable SSL checks", defaultValue = "false", order=6)
	@Getter private boolean insecureModeEnabled;
}
