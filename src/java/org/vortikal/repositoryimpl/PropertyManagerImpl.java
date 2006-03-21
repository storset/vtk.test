package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertiesModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.web.service.RepositoryAssertion;


/**
 * XXX: Add resource type to resource
 * XXX: Validation is missing
 * XXX: Validate all logic!
 * XXX: catch or declare evaluation and authorization exceptions on a reasonable level
 */
public class PropertyManagerImpl implements InitializingBean, ApplicationContextAware {

    private Log logger = LogFactory.getLog(this.getClass());

    private RoleManager roleManager;
    private PrincipalManager principalManager;

    private ResourceTypeDefinition rootResourceTypeDefinition;
    
    // Currently maps a parent resource type def. to its children (arrays)
    private Map resourceTypeDefinitions;
    
    // Currently maps namespaceUris to maps which map property names to defs.
    private Map propertyTypeDefinitions;
    
    private ApplicationContext applicationContext;
    
    private ResourceTypeDefinition[] getResourceTypeDefinitionChildren(ResourceTypeDefinition rt) {
        return (ResourceTypeDefinition[])resourceTypeDefinitions.get(rt);
    }
    
    public void afterPropertiesSet() throws Exception {
        if (roleManager == null) {
            throw new BeanInitializationException("Property 'roleManager' not set.");
        } else if (principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        } else if (rootResourceTypeDefinition == null) {
            throw new BeanInitializationException("Property 'rootResourceTypeDefinition' not set.");
        }

        List resourceTypeDefinitionList = 
            new ArrayList(applicationContext.getBeansOfType(ResourceTypeDefinition.class, 
                false, false).values());

        
        this.propertyTypeDefinitions = new HashMap();
        this.resourceTypeDefinitions = new HashMap();
        for (Iterator i = resourceTypeDefinitionList.iterator(); i.hasNext();) {
            ResourceTypeDefinition def = (ResourceTypeDefinition)i.next();
            
            // Populate map of property type definitions
            PropertyTypeDefinition[] propDefs = def.getPropertyTypeDefinitions();
            Namespace namespace = def.getNamespace();
            Map propDefMap = new HashMap();
            this.propertyTypeDefinitions.put(namespace, propDefMap);
            for (int u=0; u<propDefs.length; u++) {
                propDefMap.put(propDefs[u].getName(), propDefs[u]);
            }
            
            // Populate map of resourceTypeDefiniton parent -> children
            ResourceTypeDefinition parent = def.getParentTypeDefinition();
            ResourceTypeDefinition[] children = 
                    (ResourceTypeDefinition[]) this.resourceTypeDefinitions.get(parent);
            
            // Array append (or create if not exists for given parent)
            ResourceTypeDefinition[] newChildren = null;
            if (children == null) {
                newChildren = new ResourceTypeDefinition[1];
                newChildren[0] = def;
            } else {
                newChildren = new ResourceTypeDefinition[children.length+1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                newChildren[newChildren.length-1] = def;
            }
            this.resourceTypeDefinitions.put(parent, newChildren);
           
        }
    }        

    private boolean checkAssertions(ResourceTypeDefinition rt, Resource resource, Principal principal) {
        RepositoryAssertion[] assertions = rt.getAssertions();

        if (assertions != null) {
            for (int i = 0; i < assertions.length; i++) {
                if (!assertions[i].matches(resource, principal))
                    return false;
            }
        }
        return true;
    }

    public ResourceImpl create(Principal principal, String uri, boolean collection) {
        // XXX: Add resource type to resource
        ResourceImpl newResource = new ResourceImpl(uri, this.principalManager, this);
        ResourceTypeDefinition rt = create(principal, newResource, new Date(), 
                collection, rootResourceTypeDefinition);
        return newResource;
    }

    private ResourceTypeDefinition create(Principal principal, 
            ResourceImpl newResource, Date time, boolean isCollection, 
            ResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            CreatePropertyEvaluator evaluator = propertyDef.getCreateEvaluator();
            if (evaluator != null) {
                Property prop = createProperty(rt.getNamespace(), propertyDef.getName());
                if (evaluator.create(principal, prop, newResource, isCollection, time)) {
                    newProps.add(prop);
                }
                
            }
        }
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        
        if (children == null) return rt;
        
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = create(principal, newResource, time, isCollection, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    private void addToPropsMap(Map parent, Property property) {
        Map map = (Map) parent.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            parent.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);

    }
    
    public ResourceImpl storeProperties(ResourceImpl resource, Principal principal,
            Resource dto) throws AuthenticationException, AuthorizationException, CloneNotSupportedException {
        // For all properties, check if they are modified, deleted or created
        Map allreadySetProperties = new HashMap();
        List deadProperties = new ArrayList();
        Authorization authorization = new Authorization(principal, resource.getAcl(), this.roleManager);

        for (Iterator iter = resource.getProperties().iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            Property userProp = dto.getProperty(prop.getNamespace(), prop.getName());

            if (userProp == null) {
                // Deleted
                if (prop.getDefinition() == null) {
                    // Dead - ok
                } else {
                    if (prop.getDefinition().isMandatory()) {
                        throw new ConstraintViolationException("Property is mandatory: " + prop);
                    }
                    // check if allowed
                    try {
                        authorization.authorize(prop.getDefinition().getProtectionLevel());
                    } catch (AuthorizationException e) {
                        throw new ConstraintViolationException("Not authorized to edit property " + prop, e);
                    }
                        addToPropsMap(allreadySetProperties, userProp);
                }
            } else if (!prop.equals(userProp)) {
                // Changed value
                if (prop.getDefinition() == null) {
                    // Dead
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    try {
                        authorization.authorize(prop.getDefinition().getProtectionLevel());
                    } catch (AuthorizationException e) {
                        throw new ConstraintViolationException("Not authorized to edit property " + prop, e);
                    }
                    addToPropsMap(allreadySetProperties, userProp);
                }
            } else {
                // Unchanged - to be evaluated
            }
        }
        for (Iterator iter = dto.getProperties().iterator(); iter.hasNext();) {
            Property userProp = (Property) iter.next();
            Property prop = resource.getProperty(userProp.getNamespace(), userProp.getName());

            if (prop == null) {
                // Added
                if (userProp.getDefinition() == null) {
                    // Dead
                    deadProperties.add(userProp);
                } else {
                    // check if allowed
                    try {
                        authorization.authorize(prop.getDefinition().getProtectionLevel());
                    } catch (AuthorizationException e) {
                        throw new ConstraintViolationException("Not authorized to edit property " + prop, e);
                    }
                    addToPropsMap(allreadySetProperties, userProp);
                }
            }
        }
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), 
                this.principalManager, this);
        newResource.setID(resource.getID());
        newResource.setACL((Acl)resource.getAcl().clone());
        if (resource.getLock() != null) newResource.setLock((Lock)resource.getLock().clone());
        
        // Evaluate resource tree, for all live props not overridden, evaluate
        ResourceTypeDefinition rt = propertiesModification(principal, newResource, dto,
                new Date(), allreadySetProperties, rootResourceTypeDefinition);

        for (Iterator iter = deadProperties.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }
        for (Iterator iter = allreadySetProperties.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            for (Iterator iterator = map.values().iterator(); iterator
                    .hasNext();) {
                Property prop = (Property) iterator.next();
                newResource.addProperty(prop);
            }
        }
        
        return newResource;
}
    
    private ResourceTypeDefinition propertiesModification(Principal principal, 
            ResourceImpl newResource, Resource dto, Date time, Map allreadySetProperties, 
            ResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            // If property allready set, don't evaluate
            Map propsMap = (Map)allreadySetProperties.get(rt.getNamespace());
            if (propsMap != null) {
                Property p = (Property) propsMap.get(propertyDef.getName());
                if (p != null) {
                    newProps.add(p);
                    propsMap.remove(propertyDef.getName());
                    continue;
                }
            }

            // Not set, evaluate
            Property prop = dto.getProperty(rt.getNamespace(), propertyDef.getName());
            PropertiesModificationPropertyEvaluator evaluator = propertyDef.getPropertiesModificationEvaluator();
            if (evaluator != null) {
                if (prop == null) 
                    prop = createProperty(rt.getNamespace(), propertyDef.getName());
                if (evaluator.propertiesModification(principal, prop, newResource, time)) {
                    newProps.add(prop);
                }
                
            } else if (prop != null) {
                newProps.add(prop);
            }
        }
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = 
                propertiesModification(principal, newResource, dto, time, allreadySetProperties, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }
    
    public ResourceImpl collectionContentModification(ResourceImpl resource, 
            Principal principal) {
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), 
                this.principalManager, this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, 
                resource, null, new Date(), rootResourceTypeDefinition);
        return newResource;
    }


    public ResourceImpl fileContentModification(ResourceImpl resource, 
            Principal principal, InputStream inputStream) {
        // XXX: What to do about swapping old resource with new?
        // XXX: Add resource type to resource
        ResourceImpl newResource = new ResourceImpl(resource.getURI(), 
                this.principalManager, this);
        newResource.setID(resource.getID());
        newResource.setACL(resource.getAcl());
        newResource.setLock(resource.getLock());
        ResourceTypeDefinition rt = contentModification(principal, newResource, resource,
                new ContentImpl(inputStream), new Date(), rootResourceTypeDefinition);
        return newResource;
    }
    
    
    private ResourceTypeDefinition contentModification(Principal principal, 
            ResourceImpl newResource, Resource original, Content content, Date time, ResourceTypeDefinition rt) {

        // Checking if resource type matches
        if (!checkAssertions(rt, newResource, principal)) return null;

        // Evaluating resource type properties
        List newProps = new ArrayList();
        PropertyTypeDefinition[] def = rt.getPropertyTypeDefinitions();
        for (int i = 0; i < def.length; i++) {
            PropertyTypeDefinition propertyDef = def[i];
            
            Property prop = original.getProperty(rt.getNamespace(), propertyDef.getName());
            ContentModificationPropertyEvaluator evaluator = propertyDef.getContentModificationEvaluator();
            if (evaluator != null) {
                if (prop == null) 
                    prop = createProperty(rt.getNamespace(), propertyDef.getName());
                if (evaluator.contentModification(principal, prop, newResource, content, time)) {
                    newProps.add(prop);
                }
            } else if (prop != null) {
                newProps.add(prop);
            }
        }
        for (Iterator iter = newProps.iterator(); iter.hasNext();) {
            Property prop = (Property) iter.next();
            newResource.addProperty(prop);
        }

        // Checking child resource types by delegating
        ResourceTypeDefinition[] children = getResourceTypeDefinitionChildren(rt);
        for (int i = 0; i < children.length; i++) {
            ResourceTypeDefinition resourceType = 
                contentModification(principal, newResource, original, content, time, children[i]);
            if (resourceType != null) {
                return resourceType;
            }
        }

        return rt;
    }

    
    
    
    
    
    public Property createProperty(Namespace namespace, String name) {

        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        // XXX: probably not desired behavior
        Map map = (Map)propertyTypeDefinitions.get(namespace);
        if (map != null) {
            PropertyTypeDefinition propDef = (PropertyTypeDefinition) map.get(name);
            if (propDef != null)
                prop.setDefinition(propDef);
        }
        
        return prop;
    }

    public Property createProperty(Namespace namespace, String name, Object value) 
        throws ValueFormatException {
        PropertyImpl prop = new PropertyImpl();
        prop.setNamespace(namespace);
        prop.setName(name);
        
        PropertyTypeDefinition propDef = null;
        Map map = (Map)propertyTypeDefinitions.get(namespace);
        if (map != null) {
            propDef = (PropertyTypeDefinition) map.get(name);
            prop.setDefinition(propDef);
        }
        
        // XXX: complete this
        if (value instanceof Date) {
            Date date = (Date) value;
            prop.setDateValue(date);
        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            prop.setBooleanValue(bool.booleanValue());
        } else if (value instanceof Long) {
            Long l = (Long) value;
            prop.setLongValue(l.longValue());
        } else {
            prop.setStringValue((String) value);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Created property: " + prop);
        }

        return prop;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setRootResourceTypeDefinition(
            ResourceTypeDefinition rootResourceTypeDefinition) {
        this.rootResourceTypeDefinition = rootResourceTypeDefinition;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
