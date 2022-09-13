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
package com.fortify.cli.common.util;

public final class EnvHelper {
    private EnvHelper() {}
    
    public static final void checkSecondaryWithoutPrimary(String secondaryEnvName, String primaryEnvName) {
        if ( env(primaryEnvName)==null && env(secondaryEnvName)!=null ) {
            throw new IllegalStateException("Environment variable "+secondaryEnvName+" requires "+primaryEnvName+" to be set as well");
        }
    }
    
    public static final void checkBothOrNone(String envName1, String envName2) {
        checkSecondaryWithoutPrimary(envName1, envName2);
        checkSecondaryWithoutPrimary(envName2, envName1);
    }
    
    public static final void checkExclusive(String envName1, String envName2) {
        if ( env(envName1)!=null && env(envName2)!=null ) {
            throw new IllegalStateException("Only one of "+envName1+" and "+envName2+" environment variables may be configured");
        }
    }
    
    public static final String envName(String prefix, String suffix) {
        return String.format("%s_%s", prefix, suffix);
    }
    
    public static final String env(String name) {
        return System.getenv(name);
    }

    public static final boolean asBoolean(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    public static final char[] asCharArray(String s) {
        return s==null ? null : s.toCharArray();
    }

    public static final Integer asInteger(String s) {
        return s==null ? null : Integer.parseInt(s);
    }
}
