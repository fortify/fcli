/**
 * This package defines the 'auth' top-level command, its top-level hierarchy as shown below, and accompanying 
 * classes like mixins and (abstract) base classes.
 * <ul>
 *  <li>'auth' ({@link com.fortify.cli.common.picocli.command.auth.RootAuthCommand})
 *   <ul>
 *    <li>'login' {@link com.fortify.cli.common.picocli.command.auth.login.AuthLoginCommand}<br>
 *        Parent command for system-specific login sub-commands</li>
 *    <li>'logout' {@link com.fortify.cli.common.picocli.command.auth.logout.AuthLogoutCommand}<br>
 *        Parent command for system-specific logout sub-commands</li>
 *    <li>'sessions' {@link com.fortify.cli.common.picocli.command.auth.sessions.AuthSessionsCommand}<br>
 *        Executable command for listing active sessions</li>
 *   </ul>
 *  </li>
 * </ul>
 */
package com.fortify.cli.common.picocli.command.auth;

