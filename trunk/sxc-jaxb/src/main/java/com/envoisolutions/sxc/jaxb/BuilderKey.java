/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.envoisolutions.sxc.jaxb;

import javax.xml.namespace.QName;

import com.sun.xml.bind.v2.model.runtime.RuntimePropertyInfo;

class BuilderKey {
    QName type;
    Class parentClass;
    RuntimePropertyInfo property;
    public Class<?> typeClass;

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((parentClass == null) ? 0 : parentClass.hashCode());
        result = PRIME * result + ((property == null) ? 0 : property.hashCode());
        result = PRIME * result + ((type == null) ? 0 : type.hashCode());
        result = PRIME * result + ((typeClass == null) ? 0 : typeClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        
        final BuilderKey other = (BuilderKey)obj;
        if (parentClass == null) {
            if (other.parentClass != null)
                return false;
        } else if (!parentClass.equals(other.parentClass))
            return false;
        if (property == null) {
            if (other.property != null)
                return false;
        } else if (!property.equals(other.property))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (typeClass == null) {
            if (other.typeClass != null)
                return false;
        } else if (!typeClass.equals(other.typeClass))
            return false;
        return true;
    }

}
