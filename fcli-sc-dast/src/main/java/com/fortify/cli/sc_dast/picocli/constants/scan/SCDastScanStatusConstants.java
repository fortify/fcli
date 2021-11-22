/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.sc_dast.picocli.constants.scan;

import com.fortify.cli.sc_dast.picocli.constants._SCDastConstants;

public class SCDastScanStatusConstants extends _SCDastConstants {
	public static class Constants {
		public static final String CMD_SINGULAR    = "status";
		public static final String ALIAS_SINGULAR  = null;
		public static final String NAME_SINGULAR   = "scan status";
		public static final String OUTPUT_COLUMNS  = null;
	}
	
	// Boilerplate code
	public static class Singular extends Constants {
		public static final String CMD   = CMD_SINGULAR;
		public static final String ALIAS = ALIAS_SINGULAR;
		public static final String NAME  = NAME_SINGULAR;
		
		public static final String DESC_CREATE   = PFX_CREATE+NAME+SFX_CREATE;
		public static final String DESC_DELETE   = PFX_DELETE+NAME+SFX_DELETE;
		public static final String DESC_GET      = PFX_GET+NAME+SFX_GET;
		public static final String DESC_UPDATE   = PFX_UPDATE+NAME+SFX_UPDATE;
		public static final String DESC_DOWNLOAD = PFX_DOWNLOAD+NAME+SFX_DOWNLOAD;
		public static final String DESC_UPLOAD   = PFX_UPLOAD+NAME+SFX_UPLOAD;
	}
}
