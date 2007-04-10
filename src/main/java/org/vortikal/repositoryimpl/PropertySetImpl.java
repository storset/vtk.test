/* Copyright (c) 2006, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repositoryimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * Implementation of {@link org.vortikal.repository.PropertySet}.
 * 
 * @author oyviste
 *
 */

public class PropertySetImpl implements PropertySet, Cloneable {

    /**
     * Numeric ID used to represent the NULL resource ("no resource").
     */
    public static final int NULL_RESOURCE_ID = -1;
    
    protected String uri;
    protected String resourceType;
    protected Map propertyMap;
    
    // Numeric ID used by database 
    protected int id = NULL_RESOURCE_ID;
    
    protected boolean aclInherited = true;

    // Numeric ID of resource from which this resource inherits its ACL definition.
    private int aclInheritedFrom = NULL_RESOURCE_ID;
                               
    // Numeric ID of ancestors. Might be null. Used by indexing system.
    private int[] ancestorIds;  
   
    public PropertySetImpl(String uri) {
        this.uri = uri;
        this.propertyMap = new HashMap();
    }
 
    public int getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }
     
    public String getURI() {
        return this.uri;
    }

    public String getName() {
        if (this.uri.equals("/")) {
            return this.uri;
        } 
        return this.uri.substring(this.uri.lastIndexOf("/") + 1);
    }

    public String getResourceType() {
        return this.resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setAclInheritedFrom(int aclInheritedFrom) {
        this.aclInheritedFrom = aclInheritedFrom;
        if (aclInheritedFrom == NULL_RESOURCE_ID) {
            this.aclInherited = false;
        } else {
            this.aclInherited = true;
        }
    }

    public void setInheritedAcl(boolean aclInherited) {
        this.aclInherited = aclInherited;
        if (!aclInherited) {
            this.aclInheritedFrom = NULL_RESOURCE_ID;
        }
    }
    

    public int getAclInheritedFrom() {
        return this.aclInheritedFrom;
    }
    

    public boolean isInheritedAcl() {
//         return this.aclInheritedFrom != NULL_RESOURCE_ID;
        return this.aclInherited;
    }


    public void addProperty(Property property) {
        Map map = (Map) this.propertyMap.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            this.propertyMap.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);
    }
 
    public void removeProperty(Namespace namespace, String name) {
        Map map = (Map) this.propertyMap.get(namespace);
        if (map != null) {
            map.remove(name);
        }
    }
    
    public Property getProperty(PropertyTypeDefinition type) {
        return getProperty(type.getNamespace(), type.getName());
    }
    
    public Property getPropertyByPrefix(String prefix, String name) {
        Namespace namespace = Namespace.getNamespaceFromPrefix(prefix);
        Map map = (Map) this.propertyMap.get(namespace);
        if (map == null) return null;
        
        return (Property) map.get(name);
    }

    public Property getProperty(Namespace namespace, String name) {
        Map map = (Map) this.propertyMap.get(namespace);
        if (map == null) return null;
        
        return (Property) map.get(name);
    }

    public List getProperties(Namespace namespace) {
        Map map = (Map) this.propertyMap.get(namespace);
        if (map == null) return new ArrayList();
        return new ArrayList(map.values());
    }

    public List getProperties() {
        List props = new ArrayList();
        for (Iterator iter = this.propertyMap.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            props.addAll(map.values());
        }
        return props;
    }

    public Object clone() throws CloneNotSupportedException {
        
        PropertySetImpl clone = new PropertySetImpl(this.uri);
        clone.resourceType = this.resourceType;
        clone.setAclInheritedFrom(this.aclInheritedFrom);
        clone.setInheritedAcl(this.aclInherited);
        
        for (Iterator i = getProperties().iterator(); i.hasNext(); ){
            Property prop = (Property)i.next();
            clone.addProperty((Property) prop.clone());
        }
        
        return clone;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(" [").append(this.uri).append("]");
        return sb.toString();
    }
    
    public int[] getAncestorIds() {
        return this.ancestorIds;
    }

    public void setAncestorIds(int[] ancestorIds) {
        this.ancestorIds = ancestorIds;
    }


    public boolean equals(Object obj) {
        if (!(obj instanceof PropertySetImpl))
            return false;
        PropertySetImpl other = (PropertySetImpl) obj;
        if (!this.uri.equals(other.uri)) return false;
        if (!this.resourceType.equals(other.resourceType)) return false;
        if (!this.propertyMap.equals(other.propertyMap)) return false;
        if (this.id != other.id) return false;
        if (this.aclInheritedFrom != other.aclInheritedFrom) return false;
        if (this.ancestorIds == null && other.ancestorIds != null) return false;
        if (this.ancestorIds != null && other.ancestorIds == null) return false;
        if (this.ancestorIds != null) {
            for (int i = 0; i < this.ancestorIds.length; i++) {
                if (this.ancestorIds[i] != other.ancestorIds[i]) return false;
            }
        }
        return true;
    }
    
}
