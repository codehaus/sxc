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

public abstract class Context extends HashMap<String, Object> {
    
    public abstract Reader createReader();
    
    public abstract Writer createWriter();
}
