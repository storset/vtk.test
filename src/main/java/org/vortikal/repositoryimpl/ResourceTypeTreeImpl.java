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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;


public class ResourceTypeTreeImpl implements InitializingBean, ApplicationContextAware, ResourceTypeTree {

    private static Log logger = LogFactory.getLog(ResourceTypeTreeImpl.class);
    
    private ApplicationContext applicationContext;
    
    
    /**
     * The root of the resource type hierarchy
     */
    private PrimaryResourceTypeDefinition rootResourceTypeDefinition;
    
    /**
     * Maps a parent resource type def. to its children (arrays)
     */
    private Map<PrimaryResourceTypeDefinition, PrimaryResourceTypeDefinition[]> parentChildMap = 
        new HashMap<PrimaryResourceTypeDefinition, PrimaryResourceTypeDefinition[]>();
    

    /**
     * Maps resource type names to resource type objects
     */
    private Map<String, ResourceTypeDefinition> resourceTypeNameMap = 
        new HashMap<String, ResourceTypeDefinition>();


    /**
     * Currently maps namespaceUris to maps which map property names to defs.
     */
    private Map<Namespace, Map<String, PropertyTypeDefinition>> propertyTypeDefinitions = 
        new HashMap<Namespace, Map<String, PropertyTypeDefinition>>();
    

    /**
     * A collection containing all {@link MixinResourceTypeDefinition
     * mixin} resource type definitions
     */
    private Collection<MixinResourceTypeDefinition> mixins;


    /**
     * A collection containing all {@link
     * PrimaryResourceTypeDefinition primary} resource type
     * definitions
     */
    private Collection<PrimaryResourceTypeDefinition> primaryTypes;    


    /**
     * Maps from primary resource types to a list of mixin types:
     *
     */
    private Map<PrimaryResourceTypeDefinition, List<MixinResourceTypeDefinition>> mixinTypeDefinitionMap = 
        new HashMap<PrimaryResourceTypeDefinition, List<MixinResourceTypeDefinition>>();

    
    /**
     * Maps from mixin types to a {@link Set} of primary resource types:
     * XXX: is this really only primary resource types? I'm not really sure...
     */
    private Map<MixinResourceTypeDefinition, Set<PrimaryResourceTypeDefinition>> mixinTypePrimaryTypesMap = 
        new HashMap<MixinResourceTypeDefinition, Set<PrimaryResourceTypeDefinition>>();


    /**
     * Maps from name space URIs to {@link Namespace} objects
     */
    private Map<String, Namespace> namespaceUriMap = new HashMap<String, Namespace>();
    

    /**
     * Maps from name space prefixes to {@link Namespace} objects
     */
    private Map<String, Namespace> namespacePrefixMap = new HashMap<String, Namespace>();

    
    /**
     * Maps from namespaces to maps which map property names to a set
     * of primary resource types
     */
    private Map<Namespace, Map<String, Set<PrimaryResourceTypeDefinition>>> propDefPrimaryTypesMap = 
        new HashMap<Namespace, Map<String, Set<PrimaryResourceTypeDefinition>>>();


    /**
     * Map resource type name to flat list of _all_ descendant resource type names.
     * (Supports fast lookup for 'IN'-resource-type queries)
     */
    private Map<String, List<String>> resourceTypeDescendantNames;


    public PropertyTypeDefinition findPropertyTypeDefinition(
            Namespace namespace, String name) {
        PropertyTypeDefinition propDef = null;
        Map<String, PropertyTypeDefinition> map = this.propertyTypeDefinitions.get(namespace);

        if (map != null) {
            propDef = map.get(name);
        }

        if (logger.isDebugEnabled() && propDef == null) {
            logger.debug("No definition found for property "
                    + namespace.getPrefix() + ":" + name);
        }

        return propDef;
    }

    public void afterPropertiesSet() throws Exception {
        init();
        this.resourceTypeDescendantNames = buildResourceTypeDescendantsMap();

    }

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
                this.applicationContext = applicationContext;
        
        
    }

    public PrimaryResourceTypeDefinition getRoot() {
        return this.rootResourceTypeDefinition;
    }

    public List<MixinResourceTypeDefinition> getMixinTypes(ResourceTypeDefinition rt) {
        return this.mixinTypeDefinitionMap.get(rt);
    }
    
    public List<PrimaryResourceTypeDefinition> getResourceTypeDefinitionChildren(PrimaryResourceTypeDefinition def) {
        PrimaryResourceTypeDefinition[] children = 
            getResourceTypeDefinitionChildrenInternal(def);
        
        if (children != null) {
            return Arrays.asList(children);
        }
        
        return new ArrayList<PrimaryResourceTypeDefinition>();
    }
    
    public List<String> getResourceTypeDescendantNames(String resourceTypeName) {
        return this.resourceTypeDescendantNames.get(resourceTypeName);
    }

    public ResourceTypeDefinition getResourceTypeDefinitionByName(String name) {
        ResourceTypeDefinition type = this.resourceTypeNameMap.get(name);
        if (type == null) {
            throw new IllegalArgumentException(
                "No resource type of name '" + name + "' exists");
        }

        return type;
    }

    public PropertyTypeDefinition getPropertyDefinitionByPrefix(String prefix, String name) {
        Namespace namespace = this.namespacePrefixMap.get(prefix);
        if (namespace == null) {
            return null;
        }
        return findPropertyTypeDefinition(namespace, name);
    }

    /**
     * Search upwards in resource type tree, collect property type definitions
     * from all encountered resource type definitions including mixin resource types.
     * Assuming that mixin types can never have other mixin types attached.
     * 
     * If there are more than one occurence of the same property type definition
     * for the given resource type, only the first occurence in the resource type
     * tree is added to the returned list (upward direction).
     * 
     * @param def The <code>ResourceTypeDefinition</code> 
     * @return A <code>Set</code> of <code>PropertyTypeDefinition</code> instances.
     */
    public List<PropertyTypeDefinition> getPropertyTypeDefinitionsForResourceTypeIncludingAncestors(
                                                    ResourceTypeDefinition def) {
        Set<String> encounteredIds = new HashSet<String>();
        List<PropertyTypeDefinition> propertyTypes = new ArrayList<PropertyTypeDefinition>();
        
        if (def instanceof MixinResourceTypeDefinition) {
            MixinResourceTypeDefinition mixinDef = (MixinResourceTypeDefinition)def;
            
            PropertyTypeDefinition[] propDefs = mixinDef.getPropertyTypeDefinitions();
            addPropertyTypeDefinitions(encounteredIds, propertyTypes, propDefs);
        } else {
            // Assuming instanceof PrimaryResourceTypeDefinition
            PrimaryResourceTypeDefinition primaryDef = (PrimaryResourceTypeDefinition)def; 

            while (primaryDef != null) {
                PropertyTypeDefinition[] propDefs = primaryDef.getPropertyTypeDefinitions();
                addPropertyTypeDefinitions(encounteredIds, propertyTypes, propDefs);
                
                // Add any mixin resource types' property type defs
                MixinResourceTypeDefinition[] mixinDefs = primaryDef.getMixinTypeDefinitions();
                for (int i=0; i<mixinDefs.length; i++) {
                    addPropertyTypeDefinitions(encounteredIds, propertyTypes, 
                                                mixinDefs[i].getPropertyTypeDefinitions());
                }

                primaryDef = primaryDef.getParentTypeDefinition();
            }
        }
        
        return propertyTypes;
    }
    
    public boolean isContainedType(ResourceTypeDefinition def, String resourceTypeName) {

        ResourceTypeDefinition type = this.resourceTypeNameMap.get(resourceTypeName);
        if (type == null || !(type instanceof PrimaryResourceTypeDefinition)) {
            return false;
        }

        PrimaryResourceTypeDefinition primaryDef = (PrimaryResourceTypeDefinition) type;

        // recursive ascent on the parent axis
        while (primaryDef != null) {
            if (def instanceof MixinResourceTypeDefinition) {
                MixinResourceTypeDefinition[] mixins = primaryDef.getMixinTypeDefinitions();
                for (MixinResourceTypeDefinition mixin: mixins) {
                    if (mixin.equals(def)) {
                        return true;
                    }
                }
            } else if (primaryDef.equals(def)) {
                return true;
            }
            primaryDef = primaryDef.getParentTypeDefinition();
        }
        return false;
    }

    
    public List<PropertyTypeDefinition> getPropertyTypeDefinitions() {
        ArrayList<PropertyTypeDefinition> definitions = 
            new ArrayList<PropertyTypeDefinition>();
        
        for (Map<String, PropertyTypeDefinition> propMap: this.propertyTypeDefinitions.values()) {
            definitions.addAll(propMap.values());
        }
        
        return definitions;
    }

    public Namespace getNamespace(String namespaceUrl) {
        Namespace namespace = this.namespaceUriMap.get(namespaceUrl);
        
        if (namespace == null) 
            namespace = new Namespace(namespaceUrl);
        return namespace;
    }


    public PrimaryResourceTypeDefinition[] getPrimaryResourceTypesForPropDef(
            PropertyTypeDefinition definition) {
        
        Set<PrimaryResourceTypeDefinition> rts = 
            this.propDefPrimaryTypesMap.get(definition.getNamespace()).get(definition.getName());
        return rts.toArray(new PrimaryResourceTypeDefinition[rts.size()]);
    }

    public String getResourceTypeTreeAsString() {
        StringBuffer sb = new StringBuffer();
        printResourceTypes(sb, 0, this.rootResourceTypeDefinition);
        printMixinTypes(sb);
        return sb.toString();
    }



    private  void init() {

        this.primaryTypes = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values();

        this.mixins = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    MixinResourceTypeDefinition.class, false, false).values();

        PrimaryResourceTypeDefinition rootDefinition = null;
        for (Iterator i = this.primaryTypes.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            if (def.getParentTypeDefinition() == null) {
                if (rootDefinition != null) {
                    throw new IllegalStateException(
                        "Only one PrimaryResourceTypeDefinition having "
                        + "parentTypeDefinition = null may be defined");
                }
                rootDefinition = def;
            }
        }
        if (rootDefinition == null) {
                    throw new IllegalStateException(
                        "A PrimaryResourceTypeDefinition having "
                        + "parentTypeDefinition = null must be defined");
        }
        this.rootResourceTypeDefinition = rootDefinition;
        
        for (PrimaryResourceTypeDefinition def: this.primaryTypes) {
            
            this.resourceTypeNameMap.put(def.getName(), def);
            if (def.getNamespace() == null) {
                throw new BeanInitializationException(
                    "Definition's namespace is null: " + def
                    + " (already initialized resourceTypes = " + this.resourceTypeNameMap + ")");
            }

            addNamespacesAndProperties(def);
            
            // Populate map of resourceTypeDefiniton parent -> children
            PrimaryResourceTypeDefinition parent = def.getParentTypeDefinition();
            PrimaryResourceTypeDefinition[] children = 
                    this.parentChildMap.get(parent);

            // Array append (or create if not exists for given parent)
            PrimaryResourceTypeDefinition[] newChildren = null;

            if (children == null) {
                newChildren = new PrimaryResourceTypeDefinition[1];
                newChildren[0] = def;
            } else {
                newChildren = new PrimaryResourceTypeDefinition[children.length + 1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                newChildren[newChildren.length - 1] = def;
            }

            this.parentChildMap.put(parent, newChildren);
            this.mixinTypeDefinitionMap.put(def, buildMixinTypes(def));

        
            // Populate map from mixin types to all applicable primary types:
            List<MixinResourceTypeDefinition> mixins = 
                this.mixinTypeDefinitionMap.get(def);
            for (MixinResourceTypeDefinition mixin: mixins) {
                Set<PrimaryResourceTypeDefinition> set = this.mixinTypePrimaryTypesMap.get(mixin);
                if (set == null) {
                    set = new HashSet<PrimaryResourceTypeDefinition>();
                    this.mixinTypePrimaryTypesMap.put(mixin, set);     
                }
                set.addAll(getDescendantsAndSelf(def));
            }
        }

        // Remove null-key (which is the root resource type's "parent")
        this.parentChildMap.remove(null);
        

        for (MixinResourceTypeDefinition def: this.mixins) {
            this.resourceTypeNameMap.put(def.getName(), def);
            addNamespacesAndProperties(def);
        }

        mapPropertyDefinitionsToPrimaryTypes();
        logger.info("Resource type tree: \n" + getResourceTypeTreeAsString());
    }

    private void addPropertyTypeDefinitions(Set<String> encounteredIds, List<PropertyTypeDefinition> propertyTypes, 
                                            PropertyTypeDefinition[] propDefs) {
        for (int i = 0; i < propDefs.length; i++) {
            String id = propDefs[i].getNamespace().getUri() + ":" + propDefs[i].getName();
            // Add only _first_ occurence of property type definition
            if (encounteredIds.add(id)) {
                propertyTypes.add(propDefs[i]);
            }
        }
    }

    private PrimaryResourceTypeDefinition[] getResourceTypeDefinitionChildrenInternal(
            PrimaryResourceTypeDefinition rt) {

            PrimaryResourceTypeDefinition[] children = this.parentChildMap.get(rt);
            if (children == null)
                return new PrimaryResourceTypeDefinition[0];
            return children;
    }


    private void addNamespacesAndProperties(ResourceTypeDefinition def) {
        // Populate map of property type definitions
        PropertyTypeDefinition[] definitions = def.getPropertyTypeDefinitions();
        Namespace namespace = def.getNamespace();

        if (!this.namespaceUriMap.containsKey(def.getNamespace().getUri())) {
            logger.info("Adding namespace URI mapping: " + def.getNamespace().getUri()
                        + " -> " + def.getNamespace());
            this.namespaceUriMap.put(def.getNamespace().getUri(), def.getNamespace());
        }        

        if (!this.namespacePrefixMap.containsKey(def.getNamespace().getPrefix())) {            
            logger.info("Adding namespace prefix mapping: " + def.getNamespace().getPrefix()
                        + " -> " + def.getNamespace());
            this.namespacePrefixMap.put(def.getNamespace().getPrefix(), def.getNamespace());
        }
        
        Map<String, PropertyTypeDefinition> propDefMap = this.propertyTypeDefinitions.get(namespace);
        
        if (propDefMap == null) {
            propDefMap = new HashMap<String, PropertyTypeDefinition>();
            this.propertyTypeDefinitions.put(namespace, propDefMap);
        }
        for (int u = 0; u < definitions.length; u++) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering property type definition "
                        + definitions[u].getName());
            }
            propDefMap.put(definitions[u].getName(), definitions[u]);
        }
    }


    private Set<PrimaryResourceTypeDefinition> getDescendantsAndSelf(PrimaryResourceTypeDefinition def) {
        Set<PrimaryResourceTypeDefinition> s = new HashSet<PrimaryResourceTypeDefinition>();
        s.add(def);
        PrimaryResourceTypeDefinition[] children = this.parentChildMap.get(def);
        if (children != null) {
            for (PrimaryResourceTypeDefinition child: children) {
                s.addAll(getDescendantsAndSelf(child));
            }
        }
        return s;
    }
    
    

    private List<MixinResourceTypeDefinition> buildMixinTypes(PrimaryResourceTypeDefinition rt) {
        List<MixinResourceTypeDefinition> mixinTypes = new ArrayList<MixinResourceTypeDefinition>();
        MixinResourceTypeDefinition[] mixins = rt.getMixinTypeDefinitions();
        if (mixins != null) {
            for (MixinResourceTypeDefinition mix: mixins) {
                mixinTypes.add(mix);
                if (!this.namespaceUriMap.containsKey(mix.getNamespace().getUri()))
                    this.namespaceUriMap.put(mix.getNamespace().getUri(), mix.getNamespace());                    
            }
        }        
        return mixinTypes;
    }
    
    /**
     * Build map of resource type names to names of all descendants
     */
    private Map<String, List<String>> buildResourceTypeDescendantsMap() {
        Map<String, List<String>> resourceTypeDescendantNames = new HashMap<String, List<String>>();
        
        for (PrimaryResourceTypeDefinition def: getPrimaryResourceTypeDefinitions()) {
            List<String> descendantNames = new LinkedList<String>();
            resourceTypeDescendantNames.put(def.getName(), descendantNames);
            populateDescendantNamesRecursively(descendantNames, def);
        }
        
        if (logger.isDebugEnabled()) {
            for (Iterator i=resourceTypeDescendantNames.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String name = (String)entry.getKey();
                List descendantNames = (List)entry.getValue();
                
                StringBuffer buf = new StringBuffer("Descendant resource types of [" + name + "]: [");
                for (Iterator u = descendantNames.iterator();u.hasNext();) {
                    buf.append(u.next());
                    if (u.hasNext()) {
                        buf.append(", ");
                    }
                }
                buf.append("]");
                logger.debug(buf.toString());
            }
        }        
        return resourceTypeDescendantNames;
    }

    /**
     * Recursively get all descendant names for a given resource type 
     */
    private void populateDescendantNamesRecursively(List<String> names, PrimaryResourceTypeDefinition def) {
        List<PrimaryResourceTypeDefinition> children = getResourceTypeDefinitionChildren(def);
        
        for (PrimaryResourceTypeDefinition child: children) {
            names.add(child.getName());
            populateDescendantNamesRecursively(names, child);
        }
    }


    private void mapPropertyDefinitionsToPrimaryTypes() {

        for (Iterator i = this.primaryTypes.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition primaryTypeDef = (PrimaryResourceTypeDefinition) i.next();
            PropertyTypeDefinition[] propDefs = primaryTypeDef.getPropertyTypeDefinitions();
            mapPropertyDefinitionsToPrimaryType(propDefs, primaryTypeDef.getNamespace(), primaryTypeDef);
        }

        for (Iterator i = this.mixins.iterator(); i.hasNext();) {
            MixinResourceTypeDefinition mixin = (MixinResourceTypeDefinition) i.next();
            PropertyTypeDefinition[] mixinPropDefs = mixin.getPropertyTypeDefinitions();

            Set<PrimaryResourceTypeDefinition> primaryTypes = mixinTypePrimaryTypesMap.get(mixin);
            for (PrimaryResourceTypeDefinition primaryTypeDef: primaryTypes) {
                mapPropertyDefinitionsToPrimaryType(mixinPropDefs, mixin.getNamespace(), primaryTypeDef);
            }
        }
    }

    private List<PrimaryResourceTypeDefinition> getPrimaryResourceTypeDefinitions() {
        return new ArrayList<PrimaryResourceTypeDefinition>(
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values());
    }

    private void mapPropertyDefinitionsToPrimaryType(PropertyTypeDefinition[] propDefs,
                                                     Namespace namespace,
                                                     PrimaryResourceTypeDefinition primaryTypeDef) {
        Map<String, Set<PrimaryResourceTypeDefinition>> propDefMap = 
            this.propDefPrimaryTypesMap.get(namespace);
        if (propDefMap == null) {
            propDefMap = new HashMap<String, Set<PrimaryResourceTypeDefinition>>();
            this.propDefPrimaryTypesMap.put(namespace, propDefMap);
        }
            
        for (PropertyTypeDefinition propDef: propDefs) {
            Set<PrimaryResourceTypeDefinition> definitions = propDefMap.get(propDef.getName());
            if (definitions == null) {
                definitions = new HashSet<PrimaryResourceTypeDefinition>();
                propDefMap.put(propDef.getName(), definitions);
            }
            definitions.add(primaryTypeDef);
        }
    }
        

    private void printMixinTypes(StringBuffer sb) {

        sb.append("\n");
        for (Iterator i = this.mixins.iterator(); i.hasNext();) {
            MixinResourceTypeDefinition mixin = (MixinResourceTypeDefinition) i
                    .next();
            printResourceTypes(sb, 0, mixin);
            sb.append("\n");
        }
    }

    private void printResourceTypes(StringBuffer sb, int level,
            ResourceTypeDefinition def) {

        if (level > 0) {
            for (int i = 1; i < level; i++)
                sb.append("  ");
            sb.append("|\n");
            for (int i = 1; i < level; i++)
                sb.append("  ");
            sb.append("+--");
        }

        sb.append("[").append(def.getNamespace()).append("] ").append(
                def.getName());
        if (def instanceof MixinResourceTypeDefinition) {
            sb.append(" (mixin)");
        }
        sb.append("\n");

        List<MixinResourceTypeDefinition> mixins = this.mixinTypeDefinitionMap.get(def);
        if (mixins != null) {
            for (MixinResourceTypeDefinition mixin: mixins) {
                for (int j = 0; j < level; j++)
                    sb.append("  ");
                sb.append("  mixin: [");
                sb.append(mixin.getNamespace()).append("] ");
                sb.append(mixin.getName()).append("\n");
            }
        }

        PropertyTypeDefinition[] definitions = def.getPropertyTypeDefinitions();
        if (definitions.length > 0) {
            for (int i = 0; i < definitions.length; i++) {
                sb.append("  ");
                for (int j = 0; j < level; j++)
                    sb.append("  ");
                String type = PropertyType.PROPERTY_TYPE_NAMES[definitions[i]
                        .getType()];
                sb.append(type);
                if (definitions[i].isMultiple())
                    sb.append("[]");
                sb.append(" ").append(definitions[i].getName());
                sb.append("\n");
            }
        }
        PrimaryResourceTypeDefinition[] children = this.parentChildMap.get(def);

        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                printResourceTypes(sb, level + 1, children[i]);
            }
        }
    }

}
