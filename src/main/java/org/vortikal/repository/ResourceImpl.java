/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.util.codec.MD5;
import org.vortikal.util.repository.LocaleHelper;

public class ResourceImpl extends PropertySetImpl implements Resource {
    
    private Acl acl;
    private Lock lock = null;
    private List<Path> childURIs = null;
    
    private AuthorizationManager authorizationManager;
    private ResourceTypeTree resourceTypeTree;
    
    public ResourceImpl() {
        super();
    }
    
    public ResourceImpl(Path uri, ResourceTypeTree resourceTypeTree, 
            AuthorizationManager authorizationManager) {
        super();
        this.uri = uri;
        this.resourceTypeTree = resourceTypeTree;
        this.authorizationManager = authorizationManager;
    }

    public PrimaryResourceTypeDefinition getResourceTypeDefinition() {
        return (PrimaryResourceTypeDefinition) this.resourceTypeTree.getResourceTypeDefinitionByName(this.resourceType);
    }
    

    public boolean isOfType(ResourceTypeDefinition type) {
        return this.resourceTypeTree.isContainedType(type, this.resourceType);
    }
    

    public boolean isAuthorized(RepositoryAction action, Principal principal) 
    throws IOException {
        try {
            this.authorizationManager.authorizeAction(this.uri, action, principal);
            return true;
        } catch (AuthenticationException e) {
            return false;
        } catch (RepositoryException e) {
            return false;
        }
    }

    public Property createProperty(Namespace namespace, String name) {
        PropertyTypeDefinition propDef = 
            this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);
        return createProperty(propDef);
    }

    public Property createProperty(PropertyTypeDefinition propDef) {
        Property prop = propDef.createProperty();
        addProperty(prop);
        return prop;
    }
    
    /**
     * Creates and adds a property with a given namespace, name
     * and value. The type is set according to its {@link
     * PropertyTypeDefinition property definition}, or {@link
     * PropertyType.TYPE_STRING} if it has no definition
     *
     * @param namespace the namespace
     * @param name the name
     * @return a property instance
     * @throws ValueFormatException if the supplied value's type does
     * not match that of the property definition
     */
    public Property createProperty(Namespace namespace, String name,
            Object value) throws ValueFormatException {
        PropertyTypeDefinition propDef = 
            this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);
        Property prop = propDef.createProperty(value);
        addProperty(prop);
        return prop;
    }

    public Property createProperty(String namespaceUrl, String name, 
            String[] stringValues) {
        Namespace namespace = this.resourceTypeTree.getNamespace(namespaceUrl);
        PropertyTypeDefinition propDef = 
            this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);
        Property prop = propDef.createProperty(stringValues);
        addProperty(prop);
        return prop;
    }

    public Property createProperty(String prefix, String name, List<String> values) {
        PropertyTypeDefinition propDef = 
            this.resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);
        if (propDef == null) {
            return null;
        }
        Property prop = propDef.createProperty(values.toArray(new String[values.size()]));
        addProperty(prop);
        return prop;
    }
    
    public void removeProperty(Namespace namespace, String name) {
        Map<String, Property> props = this.propertyMap.get(namespace);
        
        if (props == null) return;
        
        Property prop = props.get(name);
        
        if (prop == null) return;

        PropertyTypeDefinition def = prop.getDefinition();
        
        if (def != null && def.isMandatory())
            throw new ConstraintViolationException("Property is mandatory"); 

        props.remove(name);
    }
    
    public void removeProperty(PropertyTypeDefinition propDef) {
        removeProperty(propDef.getNamespace(), propDef.getName());
    }    

    public String getContentLanguage() {
        return getPropValue(PropertyType.CONTENTLOCALE_PROP_NAME);
    }

    public void setChildURIs(List<Path> childURIs) {
        this.childURIs = childURIs;
    }
    
    public List<Path> getChildURIs() {
        if (this.childURIs != null) {
            return Collections.unmodifiableList(this.childURIs);
        }
            
        return null; // Not a collection
    }

    public Acl getAcl() {
        return this.acl;
    }

    public void setAcl(Acl acl) {
        this.acl = acl;
    }

    public Lock getLock() {
        return this.lock;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    public String getSerial() {
        String serial = getURI().toString() 
                            + getContentLastModified()
                            + getPropertiesLastModified()
                            + getContentLength();
        serial = MD5.md5sum(serial);
        serial = "vortex-" + serial;
        return serial;
    }
    
    public String getEtag() {
        return "\"" + getSerial() + "\"";
    }
    
    public Principal getOwner() {
        return getPrincipalPropValue(PropertyType.OWNER_PROP_NAME);
    }

    public Principal getCreatedBy() {
        return getPrincipalPropValue(PropertyType.CREATEDBY_PROP_NAME);
    }

    public Principal getContentModifiedBy() {
        return getPrincipalPropValue(PropertyType.CONTENTMODIFIEDBY_PROP_NAME);
    }

    public Principal getPropertiesModifiedBy() {
        return getPrincipalPropValue(PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME);
    }

    public Date getCreationTime() {
        return getDatePropValue(PropertyType.CREATIONTIME_PROP_NAME);
    }

    public Date getContentLastModified() {
        return getDatePropValue(PropertyType.CONTENTLASTMODIFIED_PROP_NAME);
    }

    public Date getPropertiesLastModified() {
        return getDatePropValue(PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME);
    }

    public String getContentType() {
        return getPropValue(PropertyType.CONTENTTYPE_PROP_NAME);
    }

    public String getCharacterEncoding() {
        return getPropValue(PropertyType.CHARACTERENCODING_PROP_NAME);
    }

    public String getUserSpecifiedCharacterEncoding() {
        return getPropValue(PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
    }

    public String getGuessedCharacterEncoding() {
        return getPropValue(PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME);
    }

    public String getTitle() {
        return getPropValue(PropertyType.TITLE_PROP_NAME);
    }

    public boolean isCollection() {
        return getBooleanPropValue(PropertyType.COLLECTION_PROP_NAME);
    }

    /**
     * Gets the date of this resource's last modification. The date
     * returned is either that of the
     * <code>getContentLastModified()</code> or the
     * <code>getPropertiesLastModified()</code> method, depending on
     * which one is the most recent.
     *
     * @return the time of last modification
     */
    public Date getLastModified() {
        return getDatePropValue(PropertyType.LASTMODIFIED_PROP_NAME);
    }

    /**
     * Gets the name of the principal that last modified either the
     * content or the properties of this resource.
     */
    public Principal getModifiedBy() {
        return getPrincipalPropValue(PropertyType.MODIFIEDBY_PROP_NAME);
    }

    public long getContentLength() {
        return getLongPropValue(PropertyType.CONTENTLENGTH_PROP_NAME);
    }
    
    public Locale getContentLocale() {
        return LocaleHelper.getLocale(this.getContentLanguage());
    }

    public void setUserSpecifiedCharacterEncoding(String characterEncoding) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME, characterEncoding);

    }
    
    public void setContentLocale(String locale) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME, locale);
    }

    public void setContentType(String contentType) {
        setProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME, contentType);
    }

    public void setOwner(Principal principal) {
        Property prop = getProperty(Namespace.DEFAULT_NAMESPACE, 
                PropertyType.OWNER_PROP_NAME);
        prop.setPrincipalValue(principal);
    }

    public Object clone() throws CloneNotSupportedException {
        ResourceImpl clone = cloneWithoutProperties(); 
        for (Property prop: getProperties()) {
            clone.addProperty((Property) prop.clone());
        }
        return clone;
    }

    
    public ResourceImpl createCopy(Path newUri) {
        ResourceImpl resource = new ResourceImpl(newUri, this.resourceTypeTree,
                                                 this.authorizationManager);
        resource.setResourceType(getResourceType());
        for (Property prop: getProperties()) {
            resource.addProperty(prop);
        }
        resource.setAcl(new AclImpl());
        return resource;
    }
    



    /**
     * Temp. way of getting a "clean" resource clone
     * XXX: Should be renamed to something other than "cloneXyz", like
     *      createCopyWithoutProperties. Clone implies identical, as defined by equals(), and
     *      a "clone" without props is not identical. 
     */
    public ResourceImpl cloneWithoutProperties() 
        throws CloneNotSupportedException {

        LockImpl lock = null;
        if (this.lock != null)
            lock = (LockImpl) this.lock.clone();

        ResourceImpl clone = new ResourceImpl(this.uri, this.resourceTypeTree, this.authorizationManager);
        clone.setID(this.id);

        if (this.acl != null) {
            Acl acl = (Acl) this.acl.clone();
            clone.setAcl(acl);
        }
        clone.setInheritedAcl(this.aclInherited);
        clone.setAclInheritedFrom(this.getAclInheritedFrom());
        clone.setLock(lock);
        clone.setChildURIs(this.childURIs);
        clone.setResourceType(super.resourceType);
        return clone;
    }
    
    private String getPropValue(String name) {
        Property prop = this.propertyMap.get(Namespace.DEFAULT_NAMESPACE).get(name);
        if (prop == null) return null;
        return prop.getStringValue();
    }

    private Date getDatePropValue(String name) {
        Property prop = this.propertyMap.get(Namespace.DEFAULT_NAMESPACE).get(name);
        if (prop == null) return null;
        return prop.getDateValue();
    }

    private long getLongPropValue(String name) {
        Property prop = this.propertyMap.get(Namespace.DEFAULT_NAMESPACE).get(name);
        if (prop == null) return -1;
        return prop.getLongValue();
    }

    private boolean getBooleanPropValue(String name) {
        Property prop = this.propertyMap.get(Namespace.DEFAULT_NAMESPACE).get(name);
        return prop.getBooleanValue();
    }

    private Principal getPrincipalPropValue(String name) {
        Property prop = this.propertyMap.get(Namespace.DEFAULT_NAMESPACE).get(name);
        return prop.getPrincipalValue();
    }

    private void setProperty(Namespace namespace, String name, String value) {
        if (value == null) { 
            removeProperty(namespace, name);
            return;
        }
        Property prop = getProperty(namespace, name);
        if (prop == null) {
            prop = createProperty(namespace, name);
        }
        prop.setStringValue(value);
    }
    
    /**
     * Adds a URI to the child URI list.
     *
     * @param childURI a <code>String</code> value
     */
//    public synchronized void addChildURI(Path childURI) {
//            Path[] newChildren = new Path[this.childURIs.length + 1];
//            
//            System.arraycopy(this.childURIs, 0, 
//                             newChildren,    0,
//                             this.childURIs.length);
//            
//            newChildren[this.childURIs.length] = childURI;
//            
//            this.childURIs = newChildren;
//    }
   
    // Mumble mumble. this.childURIs should never be null if this method is called.
    // However, this method is only called from RepositoryImpl.create*(). Keeping old behaviour for now.
    public synchronized void addChildURI(Path childURI) {
        if (this.childURIs == null) {
            this.childURIs = new ArrayList<Path>();
        }
        
        this.childURIs.add(childURI);
    }
    
    // Mumble mumble. this.childURIs should never be null if this method is called.
    // However, this method is only called from RepositoryImpl.delete(). Keeping old behaviour for now.
    public synchronized void removeChildURI(Path childURI) {
        if (this.childURIs == null) { // 
            this.childURIs = new ArrayList<Path>(); 
            return;
        }
        
        this.childURIs.remove(childURI);
    }
    
    /**
     * Remove a URI from the child URI list.
     * 
     * @param childURI
     */
//    public synchronized void removeChildURI(Path childURI) {
//        for (int i=0; i<this.childURIs.length; i++) {
//            if (this.childURIs[i].equals(childURI)) {
//                Path[] newChildren = new Path[this.childURIs.length-1];
//                
//                if (i > 0) {
//                    System.arraycopy(this.childURIs, 0, newChildren, 0, i);
//                }
//                
//                if (i+1 < this.childURIs.length) {
//                    System.arraycopy(this.childURIs, i+1, newChildren, i, 
//                                                (this.childURIs.length-i-1));
//                }
//                
//                this.childURIs = newChildren;
//                return;
//            }
//        }
//    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceImpl)) return false;
        if (!super.equals(obj)) return false;
        ResourceImpl other = (ResourceImpl) obj;
        if (!this.acl.equals(other.acl)) return false;
        if (this.lock == null && other.lock != null) return false;
        if (this.lock != null && other.lock == null) return false;
        if (this.lock != null && !this.lock.equals(other.lock)) return false;
        
        if ((this.childURIs == null && other.childURIs != null) 
                ||  (this.childURIs != null && other.childURIs == null)) return false;
        
        if (this.childURIs != null && other.childURIs != null) {
            if (this.childURIs.size() != other.childURIs.size()) return false;
            for (int i = 0; i < this.childURIs.size(); i++) {
                if (!this.childURIs.get(i).equals(other.childURIs.get(i)))
                    return false;
            }
        }
        
        return true;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append(": [").append(this.uri).append("]");
        return sb.toString();
    }

    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
