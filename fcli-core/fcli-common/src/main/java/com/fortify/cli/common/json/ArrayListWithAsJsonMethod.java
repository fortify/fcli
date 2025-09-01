/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.json;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Ruud Senden
 */
public class ArrayListWithAsJsonMethod<E> extends ArrayList<E> implements IWithAsJsonMethod {
    private static final long serialVersionUID = 1L;

    public ArrayListWithAsJsonMethod() { super(); }
    public ArrayListWithAsJsonMethod(Collection<? extends E> c) { super(c); }
    public ArrayListWithAsJsonMethod(int initialCapacity) { super(initialCapacity); }
    
}
