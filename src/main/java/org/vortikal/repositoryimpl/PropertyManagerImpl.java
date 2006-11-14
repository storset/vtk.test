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

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.Principal;


public class PropertyManagerImpl implements PropertyManager, InitializingBean {

    private static Log logger = LogFactory.getLog(PropertyManagerImpl.class);

    private ValueFactory valueFactory;
    private ResourceTypeTree resourceTypeTree;
    
    public void afterPropertiesSet() throws Exception {
        if (this.valueFactory == null) {
            throw new BeanInitializationException("Property 'valueFactory' not set.");
        }
        if (this.resourceTypeTree == null) {
            throw new BeanInitializationException("Property 'resourceTypeTree' not set.");
        }
    }

    public Property createProperty(Namespace namespace, String name) {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        PropertyTypeDefinition def = this.resourceTypeTree.findPropertyTypeDefinition(namespace, name);
        prop.setDefinition(def);
        
        if (def != null && def.getDefaultValue() != null) {
            if (logger.isDebugEnabled())
                logger.debug("Setting default value of prop " + prop + " to "
                             + def.getDefaultValue());

            prop.setValue(def.getDefaultValue());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Created property: " + prop + " from definition: " + def);
        }

        return prop;
    }


    public Property createProperty(Namespace namespace, String name, Object value) 
        throws ValueFormatException {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        // Set definition (may be null)
        prop.setDefinition(this.resourceTypeTree.findPropertyTypeDefinition(namespace, name));
        
        if (value instanceof Date) {
            Date date = (Date) value;
            prop.setDateValue(date);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            prop.setBooleanValue(bool.booleanValue());
        } else if (value instanceof Long) {
            Long l = (Long) value;
            prop.setLongValue(l.longValue());
        } else if (value instanceof Integer) {
            Integer i = (Integer)value;
            prop.setIntValue(i.intValue());
        } else if (value instanceof Principal) {
            Principal p = (Principal) value;
            prop.setPrincipalValue(p);
        } else {
            if (! (value instanceof String)) {
                throw new ValueFormatException(
                    "Supplied value of property [namespaces: "
                    + namespace + ", name: " + name
                    + "] not of any supported type " 
                    + "(type was: " + value.getClass() + ")");
            }
            prop.setStringValue((String) value);
        } 
        
        return prop;
    }
    

    public Property createProperty(String namespaceUrl, String name, 
                                   String[] stringValues, int type) 
        throws ValueFormatException {
        
        Namespace namespace = this.resourceTypeTree.getNamespace(namespaceUrl);        
        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        PropertyTypeDefinition def = this.resourceTypeTree.findPropertyTypeDefinition(namespace, name);
        prop.setDefinition(def);
        
        if (def != null && def.isMultiple()) {
            Value[] values = this.valueFactory.createValues(stringValues, type);
            prop.setValues(values);

            if (logger.isDebugEnabled())
                logger.debug("Created multi-value property: " + prop);
        } else {
            // Not multi-value, stringValues must be of length 1, otherwise there are
            // inconsistency problems between data store and config.
            if (stringValues.length > 1) {
                logger.error("Cannot convert multiple values to a single-value prop"
                             + " for property " + prop);
                throw new ValueFormatException(
                    "Cannot convert multiple values: " + Arrays.asList(stringValues)
                    + " to a single-value property"
                    + " for property " + prop);
            }
            if (def == null)
                // Dead, ensure value is interpreted as string:
                type = PropertyType.TYPE_STRING;
            
            Value value = this.valueFactory.createValue(stringValues[0], type);
            prop.setValue(value);
        }
        
        return prop;
        
    }
    
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public ResourceTypeTree getResourceTypeTree() {
        return this.resourceTypeTree;
    }

}
