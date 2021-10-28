/**    
 * This package defines the 'software'/'sw' top-level command, its top-level hierarchy as shown below, and accompanying 
 * classes like mixins and (abstract) base classes.
 * <ul>
 *  <li>'software'/'sw' ({@link com.fortify.cli.common.picocli.command.software.RootSoftwareCommand})
 *   <ul>
 *    <li>'install' TODO<br>
 *        Parent command for product-specific sub-commands that allow for installing Fortify software components</li>
 *    <li>'upgrade' TODO<br>
 *        Parent command for product-specific sub-commands that allow for upgrading software components to a later version<br>
 *        TODO do we need a separate upgrade-command, or could we have something like install --upgrade?</li>
 *    <li>'update' TODO<br>
 *        Parent command for product-specific sub-commands that allow for updating data like rule packs or running SmartUpdate</li>
 *   </ul>
 *  </li>
 * </ul>
 */
package com.fortify.cli.common.picocli.command.software;

