/**
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://sxc.codehaus.org/License
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * Copyright 2006 Envoi Solutions LLC
 */
package com.envoisolutions.sxc;

import java.util.HashMap;

/**
 * Entry point to SXC runtime.
 *
 * <p>
 * {@link Context} serves as the factory of {@link Reader} and {@link Writer},
 * as well as the generic configuration mechanism ({@link HashMap}) 
 */
public abstract class Context extends HashMap<String,Object> {

    /**
     * Obtains an instance of {@link Reader}.
     *
     * <p>
     * Stateless {@link Reader} implementation may return the same reader
     * for multiple invocations.
     *
     * @return
     *      null if the reader was not generated for this {@link Context}.
     */
    public abstract Reader createReader();
    
    public abstract Writer createWriter();
}
