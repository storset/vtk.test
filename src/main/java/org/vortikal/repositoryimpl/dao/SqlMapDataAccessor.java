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
package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repositoryimpl.AclImpl;
import org.vortikal.repositoryimpl.LockImpl;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PseudoPrincipal;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.util.web.URLUtil;

import com.ibatis.sqlmap.client.SqlMapClient;



/**
 * An iBATIS SQL maps implementation of the DataAccessor interface.
 */
public class SqlMapDataAccessor implements InitializingBean, DataAccessor {

    public static final char SQL_ESCAPE_CHAR = '@';

    private Map sqlMaps;

    private Log logger = LogFactory.getLog(this.getClass());

    private ContentStore contentStore;
    private PropertyManager propertyManager;
    private PrincipalFactory principalFactory;
    private AuthorizationManager authorizationManager;
    private SqlMapClient sqlMapClient;
    
    private boolean optimizedAclCopySupported = false;

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setPrincipalFactory(PrincipalFactory principalfactory) {
        this.principalFactory = principalfactory;
    }
    
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }
    
    public void setOptimizedAclCopySupported(boolean optimizedAclCopySupported) {
        this.optimizedAclCopySupported = optimizedAclCopySupported;
    }
    
    public void setSqlMaps(Map sqlMaps) {
        this.sqlMaps = sqlMaps;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.contentStore == null) {
            throw new BeanInitializationException(
                "JavaBean property 'contentStore' not specified");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not specified");
        }
        if (this.authorizationManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'authorizationManager' not specified");
        }
        if (this.principalFactory == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not specified");
        }
        if (this.sqlMapClient == null) {
            throw new BeanInitializationException(
                "JavaBean property 'sqlMapClient' not specified");
        }
        if (this.sqlMaps == null) {
            throw new BeanInitializationException(
                "JavaBean property 'sqlMaps' not specified");
        }
    }



    public boolean validate() throws IOException {
        throw new IOException("Not implemented");
    }


    

    public ResourceImpl load(String uri) throws IOException {

        try {
            this.sqlMapClient.startTransaction();

            ResourceImpl resource = loadResourceInternal(uri);
            if (resource == null) {
                return null;
            }

            loadACLs(new ResourceImpl[] {resource});
            loadChildUris(resource);

            this.sqlMapClient.commitTransaction();
            return resource;

        } catch (SQLException e) {
            this.logger.warn("Error occurred while loading resource: " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    private ResourceImpl loadResourceInternal(String uri) throws SQLException {
        String sqlMap = getSqlMap("loadResourceByUri");
        Map resourceMap = (Map)
            this.sqlMapClient.queryForObject(sqlMap, uri);
        if (resourceMap == null) {
            return null;
        }
        ResourceImpl resource = new ResourceImpl(uri, this.propertyManager,
                                                 this.authorizationManager);
        Map locks = loadLocks(new String[] {resource.getURI()});
        if (locks.containsKey(resource.getURI())) {
            resource.setLock((Lock) locks.get(resource.getURI()));
        }

        populateStandardProperties(this.propertyManager, this.principalFactory,
                                   resource, resourceMap);
        Integer resourceId = new Integer(resource.getID());
        sqlMap = getSqlMap("loadPropertiesForResource");
        List propertyList = this.sqlMapClient.queryForList(sqlMap, resourceId);
        populateCustomProperties(new ResourceImpl[] {resource}, propertyList);

        Integer aclInheritedFrom = (Integer) resourceMap.get("aclInheritedFrom");
        boolean aclInherited = aclInheritedFrom != null;
        resource.setInheritedAcl(aclInherited);
        resource.setAclInheritedFrom(aclInherited ?
                                     aclInheritedFrom.intValue() :
                                     PropertySetImpl.NULL_RESOURCE_ID);
        return resource;
    }


    public InputStream getInputStream(String uri) throws IOException {
        return this.contentStore.getInputStream(uri);
    }


    public void storeContent(String uri, InputStream inputStream)
            throws IOException {
        this.contentStore.storeContent(uri, inputStream);
    }


    public void deleteExpiredLocks() throws IOException {

        try {
            this.sqlMapClient.startTransaction();
            String sqlMap = getSqlMap("deleteExpiredLocks");
            this.sqlMapClient.update(sqlMap, new Date());
            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            this.logger.warn("Error occurred while deleting expired locks", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    


    public void addChangeLogEntry(int loggerId, int loggerType, String uri,
                                  String operation, int resourceId, boolean collection,
                                  Date timestamp, boolean recurse) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("loggerId", new Integer(loggerId));
            parameters.put("loggerType", new Integer(loggerType));
            parameters.put("uri", uri);
            parameters.put("operation", operation);
            parameters.put("resourceId", resourceId == -1 ? null : new Integer(resourceId));
            parameters.put("collection", collection ? "Y" : "N");
            parameters.put("timestamp", timestamp);
            parameters.put("uri", uri);
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               uri, SQL_ESCAPE_CHAR));

            String sqlMap = null;
            if (collection && recurse) {
                sqlMap = getSqlMap("insertChangelogEntriesRecursively");
            } else {
                sqlMap = getSqlMap("insertChangelogEntry");
            }
            this.sqlMapClient.update(sqlMap, parameters);
            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            this.logger.warn("Error occurred while adding changelog entry: " + operation
                        + " for resource: " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] discoverLocks(String uri) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               uri, SQL_ESCAPE_CHAR));
            parameters.put("timestamp", new Date());

            String sqlMap = getSqlMap("discoverLocks");
            List list = this.sqlMapClient.queryForList(sqlMap, parameters);

            String[] locks = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                locks[i] = (String) ((Map) list.get(i)).get("uri");
            }
            this.sqlMapClient.commitTransaction();
            return locks;

        } catch (SQLException e) {
            this.logger.warn("Error occurred while discovering locks below resource: "
                        + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] listSubTree(ResourceImpl parent) throws IOException {

        try {
            this.sqlMapClient.startTransaction();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(parent.getURI(),
                                                                        SQL_ESCAPE_CHAR));
            String sqlMap = getSqlMap("listSubTree");
            List list = this.sqlMapClient.queryForList(sqlMap, parameters);
            String[] uris = new String[list.size()];
            int n = 0;
            for (Iterator i = list.iterator(); i.hasNext();) {
                Map map = (Map) i.next();
                uris[n++] = (String) map.get("uri");
            }
            this.sqlMapClient.commitTransaction();
            return uris;

        } catch (SQLException e) {
            this.logger.warn("Error occurred while listing sub tree of resource: "
                        + parent.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

    }

    public void storeACL(ResourceImpl r) throws IOException {
        try {
            this.sqlMapClient.startTransaction();
            updateACL(r);
            this.sqlMapClient.commitTransaction();
        } catch (SQLException e) {
            this.logger.warn("Error occurred while storing ACL of resource: " + r.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
    


    public void updateACL(ResourceImpl r) throws SQLException {

        // XXX: ACL inheritance checking does not belong here!?
        boolean wasInherited = isInheritedAcl(r);
        if (wasInherited && r.isInheritedAcl()) {
            return;
        } 

        if (wasInherited) {

            // ACL was inherited, new ACL is not inherited:
            int oldInheritedFrom = findNearestACL(r.getURI());
            insertAcl(r);
                
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("resourceId", new Integer(r.getID()));
            parameters.put("inheritedFrom", null);

            String sqlMap = getSqlMap("updateAclInheritedFromByResourceId");
            this.sqlMapClient.update(sqlMap, parameters);

            parameters = new HashMap<String, Object>();
            parameters.put("previouslyInheritedFrom", new Integer(oldInheritedFrom));
            parameters.put("inheritedFrom", new Integer(r.getID()));
            parameters.put("uri", r.getURI());
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               r.getURI(), SQL_ESCAPE_CHAR));
            
            sqlMap = getSqlMap("updateAclInheritedFromByPreviousInheritedFromAndUri");
            this.sqlMapClient.update(sqlMap, parameters);
            return;
        }

        // ACL was not inherited
        // Delete previous ACL entries for resource:
        String sqlMap = getSqlMap("deleteAclEntriesByResourceId");
        this.sqlMapClient.delete(sqlMap, new Integer(r.getID()));

        if (!r.isInheritedAcl()) {
            insertAcl(r);

        } else {

            // The new ACL is inherited, update pointers to the
            // previously "nearest" ACL node:
            int nearest = findNearestACL(r.getURI());
            
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("inheritedFrom", new Integer(nearest));
            parameters.put("resourceId", new Integer(r.getID()));
            parameters.put("previouslyInheritedFrom", new Integer(r.getID()));
            
            sqlMap = getSqlMap("updateAclInheritedFromByResourceIdOrPreviousInheritedFrom");
            this.sqlMapClient.update(sqlMap, parameters);
        }
    }
    
    

    public void store(ResourceImpl r) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            String sqlMap = getSqlMap("loadResourceByUri");
            boolean existed = this.sqlMapClient.queryForObject(sqlMap, r.getURI()) != null;

            Map<String, Object> parameters = getResourceAsMap(r);
            if (!existed) {
                parameters.put("aclInheritedFrom", new Integer(findNearestACL(r.getURI())));
            }
            parameters.put("depth", new Integer(
                               SqlDaoUtils.getUriDepth(r.getURI())));

            sqlMap = existed ? getSqlMap("updateResource") : getSqlMap("insertResource");
            if (this.logger.isDebugEnabled()) {
                this.logger.debug((existed? "Updating" : "Storing") + " resource " + r
                             + ", parameter map: " + parameters);
            }

            this.sqlMapClient.update(sqlMap, parameters);

            if (!existed) {
                sqlMap = getSqlMap("loadResourceIdByUri");
                Map map = (Map) this.sqlMapClient.queryForObject(sqlMap, r.getURI());
                Integer id = (Integer) map.get("resourceId");
                r.setID(id.intValue());

                this.contentStore.createResource(r.getURI(), r.isCollection());
            } 

            storeLock(r);
            storeProperties(r);

            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            this.logger.warn("Error occurred while storing resource: " + r.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }



    public void delete(ResourceImpl resource) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("uri", resource.getURI());
            parameters.put("uriWildcard",
                           SqlDaoUtils.getUriSqlWildcard(resource.getURI(),
                                                         SQL_ESCAPE_CHAR));
            
            String sqlMap = getSqlMap("deleteAclEntriesByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("deleteLocksByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("deletePropertiesByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("deleteResourceByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            this.contentStore.deleteResource(resource.getURI());
            
            this.sqlMapClient.commitTransaction();

        } catch (SQLException e) {
            this.logger.warn("Error occurred while deleting resource: "
                        + resource.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public ResourceImpl[] loadChildren(ResourceImpl parent) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               parent.getURI(), SQL_ESCAPE_CHAR));
            parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                    parent.getURI()) + 1));

            List<ResourceImpl> children = new ArrayList<ResourceImpl>();
            String sqlMap = getSqlMap("loadChildren");
            List resources = this.sqlMapClient.queryForList(sqlMap, parameters);
            Map locks = loadLocksForChildren(parent);

            for (Iterator i = resources.iterator(); i.hasNext();) {
                Map resourceMap = (Map) i.next();
                String uri = (String) resourceMap.get("uri");

                ResourceImpl resource = new ResourceImpl(uri, this.propertyManager,
                                                         this.authorizationManager);

                populateStandardProperties(this.propertyManager, this.principalFactory,
                                           resource, resourceMap);
            
                if (locks.containsKey(uri)) {
                    resource.setLock((LockImpl) locks.get(uri));
                }

                children.add(resource);
            }

            ResourceImpl[] result = children.toArray(new ResourceImpl[children.size()]);
            loadChildUrisForChildren(parent, result);
            loadACLs(result);
            loadPropertiesForChildren(parent, result);

            this.sqlMapClient.commitTransaction();
            return result;
        
        } catch (SQLException e) {
            this.logger.warn("Error occurred while loading children of resource: "
                        + parent.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] discoverACLs(String uri) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(uri, SQL_ESCAPE_CHAR));

            String sqlMap = getSqlMap("discoverAcls");
            List uris = this.sqlMapClient.queryForList(sqlMap, parameters);
            
            String[] result = new String[uris.size()];
            int n = 0;
            for (Iterator i = uris.iterator(); i.hasNext();) {
                Map map = (Map) i.next();
                result[n++] = (String) map.get("uri");
            }
            this.sqlMapClient.commitTransaction();
            return result;

        } catch (SQLException e) {
            this.logger.warn("Error occurred while discovering ACLs below resource: " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

    }
    
    private void supplyFixedProperties(Map<String, Object> parameters, PropertySet fixedProperties) {
        List<Property> propertyList = fixedProperties.getProperties(Namespace.DEFAULT_NAMESPACE);
        for (Property property: propertyList) {
            if (PropertyType.SPECIAL_PROPERTIES_SET.contains(property.getName())) {
                Object value = property.getValue().getObjectValue();
                if (property.getValue().getType() == PropertyType.TYPE_PRINCIPAL) {
                    value = ((Principal) value).getQualifiedName();
                }
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Copy: fixed property: " + property.getName() + ": " + value);
                }
                parameters.put(property.getName(), value);
            }
        }
    }
    
    
    public void copy(ResourceImpl resource, ResourceImpl dest,
                     String destURI, boolean copyACLs,
                     PropertySet fixedProperties, PropertySet newResource) throws IOException {
        try {
            this.sqlMapClient.startTransaction();

            int depthDiff = SqlDaoUtils.getUriDepth(destURI)
                - SqlDaoUtils.getUriDepth(resource.getURI());
    
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("uri", resource.getURI());
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               resource.getURI(), SQL_ESCAPE_CHAR));
            parameters.put("destUri", destURI);
            parameters.put("destUriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               destURI, SQL_ESCAPE_CHAR));
            parameters.put("depthDiff", new Integer(depthDiff));

            if (fixedProperties != null) {
                supplyFixedProperties(parameters, fixedProperties);
            }

            String sqlMap = getSqlMap("copyResource");
            this.sqlMapClient.update(sqlMap, parameters);

            sqlMap = getSqlMap("copyProperties");
            this.sqlMapClient.update(sqlMap, parameters);

            if (copyACLs) {

                sqlMap = getSqlMap("copyAclEntries");
                this.sqlMapClient.update(sqlMap, parameters);
            
                // Update inheritance to nearest node:
                int srcNearestACL = findNearestACL(resource.getURI());
                int destNearestACL = findNearestACL(destURI);

                parameters = new HashMap<String, Object>();
                parameters.put("uri", destURI);
                parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                                   destURI, SQL_ESCAPE_CHAR));
                parameters.put("inheritedFrom", new Integer(destNearestACL));
                parameters.put("previouslyInheritedFrom", new Integer(srcNearestACL));

                sqlMap = getSqlMap("updateAclInheritedFromByPreviousInheritedFromAndUri");
                this.sqlMapClient.update(sqlMap, parameters);

                if (this.optimizedAclCopySupported) {
                    sqlMap = getSqlMap("updateAclInheritedFromByPreviousResourceId");
                    this.sqlMapClient.update(sqlMap, parameters);
                } else {
                    sqlMap = getSqlMap("loadPreviousInheritedFromMap");
                    List list = this.sqlMapClient.queryForList(sqlMap, parameters);
                    this.sqlMapClient.startBatch();
                    for (Iterator i = list.iterator(); i.hasNext();) {
                        Map map = (Map) i.next();
                        sqlMap = getSqlMap("updateAclInheritedFromByResourceId");
                        this.sqlMapClient.update(sqlMap, map);
                    }
                    this.sqlMapClient.executeBatch();
                }

            } else {
                Integer nearestAclNode = new Integer(findNearestACL(destURI));
                parameters = new HashMap<String, Object>();
                parameters.put("uri", destURI);
                parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                                   destURI, SQL_ESCAPE_CHAR));
                parameters.put("inheritedFrom", nearestAclNode);

                sqlMap = getSqlMap("updateAclInheritedFromByUri");
                this.sqlMapClient.update(sqlMap, parameters);
            }

            parameters = new HashMap<String, Object>();
            parameters.put("uri", destURI);
            parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                               destURI, SQL_ESCAPE_CHAR));
            sqlMap = getSqlMap("clearPrevResourceIdByUri");
            this.sqlMapClient.update(sqlMap, parameters);

            this.contentStore.copy(resource.getURI(), destURI);

            parameters = getResourceAsMap(dest);
            sqlMap = getSqlMap("updateResource");
            this.sqlMapClient.update(sqlMap, parameters);

            ResourceImpl created = loadResourceInternal(newResource.getURI());
            for (Property prop: newResource.getProperties()) {
                created.addProperty(prop);
                Property fixedProp = fixedProperties != null ?
                    fixedProperties.getProperty(prop.getNamespace(), prop.getName()) : null;
                if (fixedProp != null) {
                    created.addProperty(fixedProp);
                }
            }
            storeProperties(created);

            this.sqlMapClient.commitTransaction();
        } catch (SQLException e) {
            this.logger.warn("Error occurred while copying resource: " + resource.getURI()
                        + " to: " + destURI, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }



    private void loadChildUris(ResourceImpl parent) throws SQLException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("uriWildcard",
                       SqlDaoUtils.getUriSqlWildcard(parent.getURI(), SQL_ESCAPE_CHAR));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 1));

        String sqlMap = getSqlMap("loadChildUrisForChildren");
        List resourceList = this.sqlMapClient.queryForList(sqlMap, parameters);
        
        String[] childUris = new String[resourceList.size()];
        int n = 0;
        for (Iterator i = resourceList.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            childUris[n++] = (String) map.get("uri");
        }

        parent.setChildURIs(childUris);
    }
    

    private void loadChildUrisForChildren(ResourceImpl parent, ResourceImpl[] children)
        throws SQLException {
        
        // Initialize a map from child.URI to the set of grandchildren's URIs:
        Map<String, Set<String>> childMap = new HashMap<String, Set<String>>();
        for (int i = 0; i < children.length; i++) {
            childMap.put(children[i].getURI(), new HashSet<String>());
        }

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("uriWildcard",
                       SqlDaoUtils.getUriSqlWildcard(parent.getURI(), SQL_ESCAPE_CHAR));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 2));

        String sqlMap = getSqlMap("loadChildUrisForChildren");
        List resourceUris = this.sqlMapClient.queryForList(sqlMap, parameters);

        for (Iterator i = resourceUris.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            String uri = (String) map.get("uri");
            String parentUri = URIUtil.getParentURI(uri);
            childMap.get(parentUri).add(uri);
        }

        for (int i = 0; i < children.length; i++) {
            if (!children[i].isCollection()) continue;
            Set<String> childURIs = childMap.get(children[i].getURI());
            children[i].setChildURIs(childURIs.toArray(new String[childURIs.size()]));
        }
    }
    

    private void loadPropertiesForChildren(ResourceImpl parent, ResourceImpl[] resources)
            throws SQLException {
        if ((resources == null) || (resources.length == 0)) {
            return;
        }

        Map<Integer, ResourceImpl> resourceMap = new HashMap<Integer, ResourceImpl>();

        for (int i = 0; i < resources.length; i++) {
            resourceMap.put(resources[i].getID(), resources[i]);
        }
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("uriWildcard",
                       SqlDaoUtils.getUriSqlWildcard(parent.getURI(), SQL_ESCAPE_CHAR));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 1));

        String sqlMap = getSqlMap("loadPropertiesForChildren");
        List propertyList = this.sqlMapClient.queryForList(sqlMap, parameters);

        populateCustomProperties(resources, propertyList);
    }



    private Map<String, Lock> loadLocks(String[] uris) throws SQLException {
        if (uris.length == 0) return new HashMap<String, Lock>();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("uris", java.util.Arrays.asList(uris));
        parameters.put("timestamp", new Date());
        String sqlMap = getSqlMap("loadLocksByUris");
        List locks = this.sqlMapClient.queryForList(sqlMap, parameters);
        Map<String, Lock> result = new HashMap<String, Lock>();

        for (Iterator i = locks.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            LockImpl lock = new LockImpl(
                (String) map.get("token"),
                this.principalFactory.getUserPrincipal((String) map.get("owner")),
                (String) map.get("ownerInfo"),
                (String) map.get("depth"),
                (Date) map.get("timeout"));
            
            result.put((String) map.get("uri"), lock);
        }
        return result;
    }
    

    private Map<String, Lock> loadLocksForChildren(ResourceImpl parent) throws SQLException {

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("timestamp", new Date());
        parameters.put("uriWildcard", SqlDaoUtils.getUriSqlWildcard(
                           parent.getURI(), SQL_ESCAPE_CHAR));
        parameters.put("depth", new Integer(SqlDaoUtils.getUriDepth(
                                                parent.getURI()) + 1));
        
        String sqlMap = getSqlMap("loadLocksForChildren");
        List locks = this.sqlMapClient.queryForList(sqlMap, parameters);
        Map<String, Lock> result = new HashMap<String, Lock>();

        for (Iterator i = locks.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            LockImpl lock = new LockImpl(
                (String) map.get("token"),
                this.principalFactory.getUserPrincipal((String) map.get("owner")),
                (String) map.get("ownerInfo"),
                (String) map.get("depth"),
                (Date) map.get("timeout"));
            
            result.put((String) map.get("uri"), lock);
        }
        return result;
    }
    


    private void insertAcl(ResourceImpl r) throws SQLException {
        Map actionTypes = loadActionTypes();

        Acl newAcl = r.getAcl();
        if (newAcl == null) {
            throw new SQLException("Resource " + r + " has no ACL");
        }

        Set actions = newAcl.getActions();
        
        String sqlMap = getSqlMap("insertAclEntry");

        this.sqlMapClient.startBatch();
        for (Iterator i = actions.iterator(); i.hasNext();) {
            RepositoryAction action = (RepositoryAction) i.next();
            String actionName = Privilege.getActionName(action);
            
            for (Iterator j = newAcl.getPrincipalSet(action).iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();

                Integer actionID = (Integer) actionTypes.get(actionName);
                if (actionID == null) {
                    throw new SQLException("insertAcl(): Unable to "
                                           + "find id for action '" + action + "'");
                }

                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("actionId", actionID);
                parameters.put("resourceId", new Integer(r.getID()));
                parameters.put("principal", p.getQualifiedName());
                parameters.put("isUser", p.getType() == Principal.TYPE_GROUP ? "N" : "Y");
                parameters.put("grantedBy", r.getOwner().getQualifiedName());
                parameters.put("grantedDate", new Date());

                this.sqlMapClient.update(sqlMap, parameters);
            }

        }

        this.sqlMapClient.executeBatch();
    }
    



    private Map<String, Integer> loadActionTypes() throws SQLException {
        Map<String, Integer> actionTypes = new HashMap<String, Integer>();

        String sqlMap = getSqlMap("loadActionTypes");
        List list = this.sqlMapClient.queryForList(sqlMap, null);
        for (Iterator i = list.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            actionTypes.put((String) map.get("name"), (Integer) map.get("id"));
        }
        return actionTypes;
    }
    
    private boolean isInheritedAcl(ResourceImpl r) throws SQLException {

        String sqlMap = getSqlMap("isInheritedAcl");
        Map map = (Map) this.sqlMapClient.queryForObject(
            sqlMap, new Integer(r.getID()));
        
        Integer inheritedFrom = (Integer) map.get("inheritedFrom");
        return inheritedFrom != null;
    }       
    


    private int findNearestACL(String uri) throws SQLException {
        
        List path = java.util.Arrays.asList(URLUtil.splitUriIncrementally(uri));
        
        // Reverse list to get deepest URI first
        Collections.reverse(path);
        
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("path", path);
        String sqlMap = getSqlMap("findNearestAclResourceId");
        List list = this.sqlMapClient.queryForList(sqlMap, parameters);
        Map<String, Integer> uris = new HashMap<String, Integer>();
        for (Iterator i = list.iterator(); i.hasNext();) {
             Map map = (Map) i.next();
             uris.put((String) map.get("uri"), (Integer) map.get("resourceId"));
        }

        int nearestResourceId = -1;
        for (Iterator i = path.iterator(); i.hasNext();) {
            String candidateUri = (String) i.next();
            if (uris.containsKey(candidateUri)) {
                nearestResourceId = ((Integer) uris.get(candidateUri)).intValue();
                break;
            }
        }
        if (nearestResourceId == -1) {
            throw new SQLException("Database inconsistency: no acl to inherit "
                                   + "from for resource " + uri);
        }
        return nearestResourceId;
    }
    

    private void loadACLs(ResourceImpl[] resources) throws SQLException {

        if (resources.length == 0) return; 

        Set<Integer> resourceIds = new HashSet<Integer>();
        for (int i = 0; i < resources.length; i++) {

            Integer id = new Integer(
                resources[i].isInheritedAcl()
                ? resources[i].getAclInheritedFrom()
                : resources[i].getID());

            resourceIds.add(id);
        }
        Map<Integer, AclImpl> map = loadAclMap(new ArrayList<Integer>(resourceIds));

        if (map.isEmpty()) {
            throw new SQLException(
                "Database inconsistency: no ACL entries exist for "
                + "resources " + java.util.Arrays.asList(resources));
        }

        for (int i = 0; i < resources.length; i++) {
            AclImpl acl = null;

            if (resources[i].getAclInheritedFrom() != -1) {
                acl = (AclImpl) map.get(new Integer(resources[i].getAclInheritedFrom()));
            } else {
                acl = (AclImpl) map.get(new Integer(resources[i].getID()));
            }

            if (acl == null) {
                throw new SQLException(
                    "Resource " + resources[i] + " has no ACL entry (ac_inherited_from = "
                    + resources[i].getAclInheritedFrom() + ")");
            }

            acl = (AclImpl) acl.clone();
            resources[i].setAcl(acl);
        }
    }
    


    private Map<Integer, AclImpl> loadAclMap(List<Integer> resourceIds) throws SQLException {

        Map<Integer, AclImpl> resultMap = new HashMap<Integer, AclImpl>();
        if (resourceIds.isEmpty()) {
            return resultMap;
        }

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("resourceIds", resourceIds);

        String sqlMap = getSqlMap("loadAclEntriesByResourceIds");
        List aclEntries = this.sqlMapClient.queryForList(sqlMap, parameterMap);
            

        for (Iterator i = aclEntries.iterator(); i.hasNext();) {
            Map map = (Map) i.next();

            Integer resourceId = (Integer) map.get("resourceId");
            String privilege = (String) map.get("action");

            AclImpl acl = (AclImpl) resultMap.get(resourceId);
            
            if (acl == null) {
                acl = new AclImpl();
                resultMap.put(resourceId, acl);
            }
            
            boolean isGroup = "N".equals(map.get("isUser"));
            String name = (String) map.get("principal");
            Principal p = null;

            if (isGroup)
                p = this.principalFactory.getGroupPrincipal(name);
            else if (name.startsWith("pseudo:"))
                p = PseudoPrincipal.getPrincipal(name);
            else
                p = this.principalFactory.getUserPrincipal(name);
            RepositoryAction action = Privilege.getActionByName(privilege);
            acl.addEntry(action, p);
        }
        return resultMap;
    }
    

    

    private void storeLock(ResourceImpl r) throws SQLException {
        Lock lock = r.getLock();
        if (lock == null) {
            // Delete any old persistent locks
            String sqlMap = getSqlMap("deleteLockByResourceId");
            this.sqlMapClient.delete(sqlMap, new Integer(r.getID()));
        }
        if (lock != null) {
            String sqlMap = getSqlMap("loadLockByLockToken");
            boolean exists = this.sqlMapClient.queryForObject(
                sqlMap, lock.getLockToken()) != null;

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("lockToken", lock.getLockToken());
            parameters.put("timeout", lock.getTimeout());
            parameters.put("owner", lock.getPrincipal().getQualifiedName());
            parameters.put("ownerInfo", lock.getOwnerInfo());
            parameters.put("depth", lock.getDepth());
            parameters.put("resourceId", new Integer(r.getID()));

            sqlMap = exists ? getSqlMap("updateLock") : getSqlMap("insertLock");
            this.sqlMapClient.update(sqlMap, parameters);
        }
    }
    


    private void storeProperties(ResourceImpl r) throws SQLException {
        
        String sqlMap = getSqlMap("deletePropertiesByResourceId");
        this.sqlMapClient.update(sqlMap, new Integer(r.getID()));

        List properties = r.getProperties();
        
        if (properties != null) {

            sqlMap = getSqlMap("insertPropertyEntry");

            this.sqlMapClient.startBatch();

            for (Iterator iter = properties.iterator(); iter.hasNext();) {
                Property property = (Property) iter.next();

                if (!PropertyType.SPECIAL_PROPERTIES_SET.contains(property.getName())) {
                    Map<String, Object> parameters = new HashMap<String, Object>();
                    parameters.put("namespaceUri", property.getNamespace().getUri());
                    parameters.put("name", property.getName());
                    parameters.put("resourceId", new Integer(r.getID()));
                    parameters.put("type", new Integer(
                                       property.getDefinition() != null
                                       ? property.getDefinition().getType()
                                       : PropertyType.TYPE_STRING));
                    
                    if (property.getDefinition() != null
                            && property.getDefinition().isMultiple()) {

                        Value[] values = property.getValues();
                        for (int i = 0; i < values.length; i++) {
                            parameters.put("value",
                                           values[i].getNativeStringRepresentation());
                            
                            this.sqlMapClient.update(sqlMap, parameters);
                        }
                    } else {
                        Value value = property.getValue();
                        parameters.put("value", value.getNativeStringRepresentation());
                        this.sqlMapClient.update(sqlMap, parameters);
                    }
                }
            }
            this.sqlMapClient.executeBatch();
        }
    }
    


    private void populateCustomProperties(ResourceImpl[] resources, List propertyList) {

        Map<Integer, ResourceImpl> resourceMap = new HashMap<Integer, ResourceImpl>();
        for (int i = 0; i < resources.length; i++) {
            resourceMap.put(new Integer(resources[i].getID()), resources[i]);
        }

        Map<SqlDaoUtils.PropHolder, List<String>> propMap =
            new HashMap<SqlDaoUtils.PropHolder, List<String>>();

        for (Iterator i = propertyList.iterator(); i.hasNext();) {
            Map propEntry = (Map) i.next();

            SqlDaoUtils.PropHolder prop = new SqlDaoUtils.PropHolder();
            prop.namespaceUri = (String) propEntry.get("namespaceUri");
            prop.name = (String) propEntry.get("name");
            prop.resourceId = ((Integer) propEntry.get("resourceId")).intValue();
            
            List<String> values = propMap.get(prop);
            if (values == null) {
                values = new ArrayList<String>();
                prop.type = ((Integer) propEntry.get("typeId")).intValue();
                prop.values = values;
                propMap.put(prop, values);
            }
            values.add((String) propEntry.get("value"));
        }

        for (Iterator i = propMap.keySet().iterator(); i.hasNext();) {
            SqlDaoUtils.PropHolder prop = (SqlDaoUtils.PropHolder) i.next();
            
            Property property = this.propertyManager.createProperty(
                prop.namespaceUri,
                prop.name, (String[]) prop.values.toArray(new String[]{}));

            ResourceImpl r = (ResourceImpl) resourceMap.get(
                    new Integer(prop.resourceId));
            r.addProperty(property);
        }
    }
    

    public static void populateStandardProperties(
        PropertyManager propertyManager, PrincipalFactory principalFactory,
        PropertySetImpl propertySet, Map resourceMap) {

        propertySet.setID(((Number)resourceMap.get("id")).intValue());
        
        boolean collection = "Y".equals(resourceMap.get("isCollection"));
        Property prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME,
            Boolean.valueOf(collection));
        propertySet.addProperty(prop);
        
        Principal createdBy = principalFactory.getUserPrincipal(
            (String) resourceMap.get("createdBy"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME,
                createdBy);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME,
            resourceMap.get("creationTime"));
        propertySet.addProperty(prop);

        Principal principal = principalFactory.getUserPrincipal(
            (String) resourceMap.get("owner"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        String string = (String) resourceMap.get("contentType");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("characterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("guessedCharacterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("userSpecifiedCharacterEncoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = (String) resourceMap.get("contentLanguage");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }

        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME,
                resourceMap.get("lastModified"));
        propertySet.addProperty(prop);

        principal = principalFactory.getUserPrincipal((String) resourceMap.get("modifiedBy"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME,
                principal);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME,
            resourceMap.get("contentLastModified"));
        propertySet.addProperty(prop);

        principal = principalFactory.getUserPrincipal(
            (String) resourceMap.get("contentModifiedBy"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME,
            resourceMap.get("propertiesLastModified"));
        propertySet.addProperty(prop);

        principal = principalFactory.getUserPrincipal(
            (String) resourceMap.get("propertiesModifiedBy"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        if (!collection) {
            long contentLength = ((Number) resourceMap.get("contentLength")).longValue();
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME,
                new Long(contentLength));
            propertySet.addProperty(prop);
        }
        
        propertySet.setResourceType((String) resourceMap.get("resourceType"));

        Integer aclInheritedFrom = (Integer) resourceMap.get("aclInheritedFrom");
        if (aclInheritedFrom == null) {
            aclInheritedFrom = new Integer(-1);
        }

        propertySet.setAclInheritedFrom(aclInheritedFrom.intValue());
    }


    
    private Map<String, Object> getResourceAsMap(ResourceImpl r) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("parent", r.getParent());
        // XXX: use Integer (not int) as aclInheritedFrom field:
        map.put("aclInheritedFrom", r.getAclInheritedFrom() == PropertySetImpl.NULL_RESOURCE_ID
                ? null : new Integer(r.getAclInheritedFrom()));
        map.put("uri", r.getURI());
        map.put("resourceType", r.getResourceType());
        

        // XXX: get list of names from PropertyType.SPECIAL_PROPERTIES:
        map.put("collection", r.isCollection() ? "Y" : "N");
        map.put("owner", r.getOwner().getQualifiedName());
        map.put("creationTime", r.getCreationTime());
        map.put("createdBy", r.getCreatedBy().getQualifiedName());
        map.put("contentType", r.getContentType());
        map.put("characterEncoding", r.getCharacterEncoding());
        map.put("userSpecifiedCharacterEncoding", r.getUserSpecifiedCharacterEncoding());
        map.put("guessedCharacterEncoding", r.getGuessedCharacterEncoding());
        // XXX: contentLanguage should be contentLocale:
        map.put("contentLanguage", r.getContentLanguage());
        map.put("lastModified", r.getLastModified());
        map.put("modifiedBy", r.getModifiedBy().getQualifiedName());
        map.put("contentLastModified", r.getContentLastModified());
        map.put("contentModifiedBy", r.getContentModifiedBy().getQualifiedName());
        map.put("propertiesLastModified", r.getPropertiesLastModified());
        map.put("propertiesModifiedBy", r.getPropertiesModifiedBy().getQualifiedName());
        map.put("contentLength", new Long(r.getContentLength()));

        return map;
    }
    

    private String getSqlMap(String statementId) {
        if (this.sqlMaps.containsKey(statementId)) {
            return (String) this.sqlMaps.get(statementId);
        }
        return statementId;
    }
    
    public Set<Principal> discoverGroups() throws IOException {
        
        try {
            this.sqlMapClient.startTransaction();

            String sqlMap = getSqlMap("discoverGroups");
            List groupNames = this.sqlMapClient.queryForList(sqlMap, null);
        
            Set<Principal> groups = new HashSet<Principal>();
            for (Iterator i = groupNames.iterator(); i.hasNext();) {
                String groupName = (String)i.next();
                Principal group = this.principalFactory.getGroupPrincipal(groupName);
                groups.add(group);
            }
            
            return groups;
        } catch (SQLException e) {
            this.logger.warn("Error occurred while queyring for distinct group names", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                this.sqlMapClient.endTransaction();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
    
}

