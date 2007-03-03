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
package org.vortikal.repositoryimpl.query.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.query.WildcardPropertySelect;
import org.vortikal.repositoryimpl.query.query.PropertySelect;
import org.vortikal.repositoryimpl.query.query.PropertySortField;
import org.vortikal.repositoryimpl.query.query.SimpleSortField;
import org.vortikal.repositoryimpl.query.query.SortField;
import org.vortikal.repositoryimpl.query.query.SortFieldDirection;
import org.vortikal.repositoryimpl.query.query.Sorting;
import org.vortikal.repositoryimpl.query.query.SortingImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/**
 * Utility class for performing searches returning result sets wrapped
 * in an XML structure.
 *
 * XXX: move this class to another package (has org.vortikal.web.*
 * dependencies among other things).
 *
 */
public class XmlSearcher implements InitializingBean {

    private static final String URL_IDENTIFIER = "url";
    private static final String NAME_IDENTIFIER = "name";
    private static final String TYPE_IDENTIFIER = "type";
    private static final String URI_IDENTIFIER = "uri";
    
    private static Log logger = LogFactory.getLog(XmlSearcher.class);

    private Searcher searcher;
    private QueryManager queryManager;
    private PropertyManager propertyManager;
    private ResourceTypeTree resourceTypeTree;
    private int maxResults = 1000;
    private Repository repository;
    private String defaultLocale = Locale.getDefault().getLanguage();

    private Service linkToService;
    private ResourceTypeDefinition collectionResourceTypeDef;

    private ValueFormatter valueFormatter;
    
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setQueryManager(QueryManager queryManager) {
        this.queryManager = queryManager;
    }
    
    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
    

    public void afterPropertiesSet() {
        if (this.searcher == null) {
            throw new BeanInitializationException(
                "JavaBean property 'searcher' not set");
        }
        if (this.queryManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'queryManager' not set");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not set");
        }
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set");
        }
        if (this.valueFormatter == null) {
            throw new BeanInitializationException(
                "JavaBean property 'valueFormatter' not set");
        }
        if (this.linkToService == null) {
            throw new BeanInitializationException(
                "JavaBean property 'linkToService' not set");
        }
        if (this.defaultLocale == null) {
            throw new BeanInitializationException(
                "JavaBean property 'defaultLocale' not set");
        }

        this.resourceTypeTree = this.propertyManager.getResourceTypeTree();
    }
    

    /**
     * Should probably be deprecated?
     */
    public NodeList executeQuery(String query, String sort, String maxResultsStr,
                                 String fields) throws QueryException {
        return executeQuery(query, sort, maxResultsStr, fields, false);
    }
  
    public NodeList executeQuery(String query, String sort, String maxResultsStr,
            String fields, boolean authorizeCurrentPrincipal) throws QueryException {

        int limit = this.maxResults;
        try {
            limit = Integer.parseInt(maxResultsStr);
        } catch (NumberFormatException e) {}

        Document doc = executeDocumentQuery(query, sort, limit, fields, authorizeCurrentPrincipal);
        return doc.getDocumentElement().getChildNodes();
    }

    public Document executeDocumentQuery(String query, String sort,
            int maxResults, String fields, boolean authorizeCurrentPrincipal) throws QueryException {
        String token = null;
        
        if (authorizeCurrentPrincipal) {
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            token = securityContext.getToken();
        }
        
        return executeDocumentQuery(token, query, sort, maxResults, fields);
    }

    private Document executeDocumentQuery(String token, String query,
                                         String sort, int maxResults,
                                         String fields) throws QueryException {
        int limit = maxResults;

        if (maxResults > this.maxResults) {
            limit = this.maxResults;
        }


        Document doc = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new QueryException(e.getMessage());
        }
        
        try {
            SearchEnvironment envir = new SearchEnvironment(sort, fields);
            PropertySelect select = envir.getPropertySelect();
            Sorting sorting = envir.getSorting();

            if (logger.isDebugEnabled()) {
                logger.debug("About to execute query: " + query + ": sort = " + sorting
                             + ", limit = " + limit + ", envir = " + envir);
            }

            ResultSet rs = this.queryManager.execute(token, query, sorting, limit, select);
            
            addResultSetToDocument(rs, doc, envir);
        } catch (Exception e) {
            logger.warn("Error occurred while performing query: '" + query + "'", e);
            
            Element errorElement = doc.createElement("error");
            doc.appendChild(errorElement);
            errorElement.setAttribute("exception", e.getClass().getName());
            errorElement.setAttribute("query", query);
            errorElement.setAttribute("sort", sort);
            String msg = e.getMessage() != null ? e.getMessage() : "No message";
            Text text = doc.createTextNode(msg);
            errorElement.appendChild(text);

        }
        return doc;
    }
    

    private void addResultSetToDocument(ResultSet rs, Document doc, SearchEnvironment envir) {
        long start = System.currentTimeMillis();
        
        Element resultElement = doc.createElement("results");
        doc.appendChild(resultElement);
        resultElement.setAttribute("size", String.valueOf(rs.getSize()));
        resultElement.setAttribute("totalHits", String.valueOf(rs.getTotalHits()));
        for (Iterator i = rs.iterator(); i.hasNext();) {
            PropertySet propSet = (PropertySet)i.next();
            addPropertySetToResults(doc, resultElement, propSet, envir);
        }
        
        if (logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            logger.debug("Building XML result set took " + (now - start) + " ms");
        }
    }
    
    private void addPropertySetToResults(Document doc, Element resultsElement, 
                                         PropertySet propSet, SearchEnvironment envir) {
        
        Element propertySetElement = doc.createElement("resource");
        resultsElement.appendChild(propertySetElement);
        if (envir.reportUri())
            propertySetElement.setAttribute(URI_IDENTIFIER, propSet.getURI());
        if (envir.reportName())
            propertySetElement.setAttribute(NAME_IDENTIFIER, propSet.getName());
        if (envir.reportType())
            propertySetElement.setAttribute(TYPE_IDENTIFIER, propSet.getResourceType());
        if (envir.reportUrl())
            propertySetElement.setAttribute(URL_IDENTIFIER, getUrl(propSet));

        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Property prop = (Property) i.next();
            addPropertyToPropertySetElement(doc, propertySetElement, prop, envir);
        }
        
    }
    
    private String getUrl(PropertySet propSet) {
        String uri = propSet.getURI();
        if (collectionResourceTypeDef != null && !uri.equals("/") 
                && resourceTypeTree.isContainedType(collectionResourceTypeDef,
                        propSet.getResourceType()))
            uri += "/";
                        
        return this.linkToService.constructLink(uri);
    }
    
    private void addPropertyToPropertySetElement(Document doc, Element propSetElement,
                                                 Property prop, SearchEnvironment envir) {
        
        if (prop.getDefinition() == null) {
            return;
        }
        
        Element propertyElement = doc.createElement("property");
        
        String namespaceUri = prop.getNamespace().getUri();
        if (namespaceUri != null) {
            propertyElement.setAttribute("namespace", namespaceUri);
        }
        
        String prefix = prop.getNamespace().getPrefix();
        if (prefix != null) {
            propertyElement.setAttribute("name", prefix + ":" + prop.getName());
        } else {
            propertyElement.setAttribute("name", prop.getName());
        }
        
        Locale locale = envir.getLocale();

        Set formatSet = envir.getFormats().getFormats(prop.getDefinition());
        if (!formatSet.contains(null)) {
            // Add default (null) format:
            formatSet.add(null);
        }

        for (Iterator iter = formatSet.iterator(); iter.hasNext();) {
            String format = (String) iter.next();
            if (prop.getDefinition().isMultiple()) {
                Element valuesElement = doc.createElement("values");
                if (format != null) {
                    valuesElement.setAttribute("format", format);
                }
                Value[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    Element valueElement = valueElement(doc, values[i], format, locale);
                    valuesElement.appendChild(valueElement);
                }
                propertyElement.appendChild(valuesElement);
            } else {
                Value value = prop.getValue();
                Element valueElement = valueElement(doc, value, format, locale);
                if (format != null) {
                    valueElement.setAttribute("format", format);
                }

                propertyElement.appendChild(valueElement);
            }
        }
        propSetElement.appendChild(propertyElement);        
    }

    
    private Element valueElement(Document doc, Value value, String format, Locale locale) {
            Element valueElement = doc.createElement("value");
            Text text = doc.createTextNode(this.valueFormatter.valueToString(value, format, locale));
            valueElement.appendChild(text);
            return valueElement;
    }
    

    private class Formats {

        private Map formats = new HashMap();

        public void addFormat(PropertyTypeDefinition def, String format) {
            Set s = (Set) this.formats.get(def);
            if (s == null) {
                s = new HashSet();
                this.formats.put(def, s);
            }
            s.add(format);
        }

        public Set getFormats(PropertyTypeDefinition def) {
            Set set = (Set) this.formats.get(def);
            if (set == null) {
                set = new HashSet();
            }
            return set;
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer(this.getClass().getName());
            sb.append(": formats = ").append(this.formats);
            return sb.toString();
        }
    }


    private class SearchEnvironment {
        
        private PropertySelect select = null;
        private Sorting sort;
        private Formats formats = new Formats();
        private Locale locale = new Locale(defaultLocale);
        private boolean reportUri = false;
        private boolean reportName = false;
        private boolean reportType = false;
        private boolean reportUrl = false;
        
        public SearchEnvironment(String sort, String fields) {
            parseSortString(sort);
            parseFields(fields);
            resolveLocale();
        }

        public PropertySelect getPropertySelect() {
            return this.select;
        }

        public Sorting getSorting() {
            return this.sort;
        }

        public Formats getFormats() {
            return this.formats;
        }

        public Locale getLocale() {
            return this.locale;
        }
        

        public String toString() {
            StringBuffer sb = new StringBuffer(this.getClass().getName());
            sb.append(": select = ").append(this.select);
            sb.append(", sort = ").append(this.sort);
            sb.append(", formats = ").append(this.formats);
            sb.append(", locale = ").append(this.locale);
            return sb.toString();
        }
        

        /**
         * Parses a sort specification of the syntax
         * <code>field(:asc|:desc)?(,field(:asc|:desc)?)*</code> and
         * produces a {@link Sorting} object.
         *
         * @param sortString the sort specification
         * @return a sort object, or <code>null</code> if the string does
         * not contain any valid sort fields.
         */
        public void parseSortString(String sortString) {
            if (sortString == null || "".equals(sortString.trim())) {
                return;
            }
        
            String[] fields = sortString.split(",");
            List result = new ArrayList();
            Set referencedFields = new HashSet();
        
            for (int i = 0; i < fields.length; i++) {
                String specifier = fields[i].trim();
                String field = null;
                SortFieldDirection direction = SortFieldDirection.ASC;
                String[] pair = specifier.split("\\s+");
                if (pair.length == 2) {
                    field = pair[0];
                    if ("descending".startsWith(pair[1])) {
                        direction = SortFieldDirection.DESC;
                    }
                } else if (pair.length == 1) {
                    field = pair[0];
                } else {
                    throw new QueryException("Invalid sort field: '" + specifier + "'");
                }
                SortField sortField = null;
                sortField = new SimpleSortField(field, direction);
                if (URI_IDENTIFIER.equals(field) || 
                        TYPE_IDENTIFIER.equals(field) || 
                        NAME_IDENTIFIER.equals(field) || 
                        URL_IDENTIFIER.equals(field)) {

                } else {
                    String prefix = null;
                    String name = null;

                    String[] components = field.split(":");
                    if (components.length == 2) {
                        prefix = components[0];
                        name = components[1];
                    } else if (components.length == 1) {
                        name = components[0];
                    } else {
                        throw new QueryException("Unknown sort field: '" + field + "'");
                    }
                    PropertyTypeDefinition def =
                        XmlSearcher.this.resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);
                    sortField = new PropertySortField(def, direction);
                }
                if (referencedFields.contains(field)) {
                    throw new QueryException(
                        "Sort field '" + field + "' occurs more than once");
                }
                referencedFields.add(field);
                result.add(sortField);
            }

            if (!result.isEmpty()) {
                this.sort = new SortingImpl(result);
            }
        }


        private void parseFields(String fields) {
            if (fields == null || "".equals(fields.trim())) {
                this.select = WildcardPropertySelect.WILDCARD_PROPERTY_SELECT;
                this.reportUri = true;
                this.reportName = true;
                this.reportType = true;
                this.reportUrl = true;
                return;
            }
            String[] fieldsArray = splitFields(fields);
            HashSetPropertySelect selectedFields = new HashSetPropertySelect();
            this.select = selectedFields;

            for (int i = 0; i < fieldsArray.length; i++) {
                String fullyQualifiedName = fieldsArray[i];
                if ("".equals(fullyQualifiedName.trim())) {
                    continue;
                }
                fullyQualifiedName = fullyQualifiedName.replaceAll("\\,", ",");
                String prefix = null;
                String name = fullyQualifiedName.trim();

                if (URI_IDENTIFIER.equals(name)) {
                    this.reportUri = true;
                    continue;
                } else if (NAME_IDENTIFIER.equals(name)) {
                    this.reportName = true;
                    continue;
                } else if (TYPE_IDENTIFIER.equals(name)) {
                    this.reportType = true;
                    continue;
                } else if (URL_IDENTIFIER.equals(name)) {
                    this.reportUrl = true;
                    continue;
                } 
                
                String format = null;
                int bracketStartPos = name.indexOf("[");
                if (bracketStartPos != -1 && bracketStartPos > 1) {
                    int bracketEndPos = name.indexOf("]", bracketStartPos);
                    if (bracketEndPos != -1) {
                        format = name.substring(bracketStartPos + 1, bracketEndPos);
                        name = name.substring(0, bracketStartPos);
                    }
                }

                int separatorPos = name.indexOf(":");
                if (separatorPos != -1) {
                    prefix = name.substring(0, separatorPos).trim();
                    name = name.substring(separatorPos + 1).trim();
                }
                
                PropertyTypeDefinition def =
                    resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);

                if (def != null && format != null) {
                    this.formats.addFormat(def, format);
                }
                if (def != null) {
                    selectedFields.addPropertyDefinition(def);
                }
            }
            
        }
        

        private void resolveLocale() {
            try {
                RequestContext requestContext = RequestContext.getRequestContext();
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                String token = securityContext.getToken();
                String uri = requestContext.getResourceURI();
                Resource resource = repository.retrieve(token, uri, true);
                String contentLanguage = resource.getContentLanguage();
                if (contentLanguage != null) {
                    String lang = contentLanguage;
                    if (contentLanguage.indexOf("_") != -1) {
                        lang = contentLanguage.substring(0, contentLanguage.indexOf("_"));
                    }
                    this.locale = new Locale(lang);
                }
                
            } catch (Throwable t) { 
                if (logger.isDebugEnabled()) {
                    logger.debug("Unable to resolve locale of resource", t);
                }
            }
        }
        

        // Splits fields on ',' characters, allowing commas to appear
        // within (non-nested) brackets.
        private String[] splitFields(String fields) {
            List l = new ArrayList();
            String s = new String();
            boolean insideBrackets = false;
            for (int i = 0; i < fields.length(); i++) {
                if (',' == fields.charAt(i) && !insideBrackets) {
                    l.add(s);
                    s = new String();
                } else {

                    if ('[' == fields.charAt(i)) {
                        if (!insideBrackets) {
                            insideBrackets = true;
                        }

                    } else if (']' == fields.charAt(i)) {
                        if (insideBrackets) {
                            insideBrackets = false;
                        }
                    }
                    s += fields.charAt(i);
                }

            }
            if (!"".equals(s)) {
                l.add(s);
            }
            return (String[]) l.toArray(new String[l.size()]);
        }
        
        public boolean reportUri() {
            return this.reportUri;
        }

        public boolean reportName() {
            return this.reportName;
        }

        public boolean reportType() {
            return this.reportType;
        }

        public boolean reportUrl() {
            return this.reportUrl;
        }

    }


    private class HashSetPropertySelect implements PropertySelect {
        private Set properties = new HashSet();
        
        public void addPropertyDefinition(PropertyTypeDefinition def) {
            this.properties.add(def);
        }

        public boolean isEmpty() {
            return this.properties.isEmpty();
        }

        public boolean isIncludedProperty(PropertyTypeDefinition def) {
            return this.properties.contains(def);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getClass().getName()).append(":");
            sb.append("propertiess = ").append(this.properties);
            return sb.toString();
        }
        
    }


    public void setLinkToService(Service linkToService) {
        this.linkToService = linkToService;
    }

    public void setCollectionResourceTypeDef(
            ResourceTypeDefinition collectionResourceTypeDef) {
        this.collectionResourceTypeDef = collectionResourceTypeDef;
    }

    public void setValueFormatter(ValueFormatter valueFormatter) {
        this.valueFormatter = valueFormatter;
    }

}
