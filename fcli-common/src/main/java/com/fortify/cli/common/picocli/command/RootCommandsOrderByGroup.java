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
package com.fortify.cli.common.picocli.command;

import com.fortify.cli.common.picocli.command.crud.CRUDCommandsOrder;

/**
 * This class defines the order of top-level command groups, with 
 * each group representing either a single top-level command or
 * a group of top-level commands. As an example, {@link #CRUD}
 * is a group of entity-related commands providing CRUD operations,
 * with {@link CRUDCommandsOrder} defining the relative order
 * of the individual commands in this group.
 * 
 * @author Ruud Senden
 */
public final class RootCommandsOrderByGroup {
	public static final int 
		CONFIG    = 100,
		AUTH      = 200,
		SAST_SCAN = 300,
		DAST_SCAN = 400,
		CRUD      = 500,
		TRANSFER  = 600,
		RUN       = 700,
		SOFTWARE  = 800,
		API       = 900;
}
