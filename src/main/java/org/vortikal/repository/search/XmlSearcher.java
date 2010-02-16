/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.search;

import static org.vortikal.repository.resourcetype.PropertyType.Type.IMAGE_REF;
import static org.vortikal.repository.resourcetype.PropertyType.Type.STRING;

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
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.html.HtmlText;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
public class XmlSearcher {

    private static final String URL_IDENTIFIER = "url";
    
    private static Log logger = LogFactory.getLog(XmlSearcher.class);

    private HtmlPageParser htmlParser;
    private Map<String, String> htmlEntityMap;
    
    private Searcher searcher;
    private Parser parser;
    private ResourceTypeTree resourceTypeTree;
    private int maxResults = 2000;
    private Repository repository;
    private String defaultLocale = Locale.getDefault().getLanguage();

    private Service linkToService;
    private ResourceTypeDefinition collectionResourceTypeDef;

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
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

            Search search = new Search();
            search.setQuery(this.parser.parse(query));
            if (envir.getSorting() != null)
                search.setSorting(envir.getSorting());
            search.setLimit(limit);
            search.setPropertySelect(envir.getPropertySelect());
            ResultSet rs = this.searcher.execute(token, search);
            
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
        for (Iterator<PropertySet> i = rs.iterator(); i.hasNext();) {
            PropertySet propSet = i.next();
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
            propertySetElement.setAttribute(PropertySet.URI_IDENTIFIER, propSet.getURI().toString());
        if (envir.reportName())
            propertySetElement.setAttribute(PropertySet.NAME_IDENTIFIER, propSet.getName());
        if (envir.reportType())
            propertySetElement.setAttribute(PropertySet.TYPE_IDENTIFIER, propSet.getResourceType());
        if (envir.reportUrl())
            propertySetElement.setAttribute(URL_IDENTIFIER, getUrl(propSet).toString());

        for (Property prop: propSet.getProperties()) {
            addPropertyToPropertySetElement(propSet.getURI(), propertySetElement, prop, envir);
        }
        
    }
    
    private URL getUrl(PropertySet propSet) {
        Path uri = propSet.getURI();
        URL url = this.linkToService.constructURL(uri);
        if (collectionResourceTypeDef != null &&
                collectionResourceTypeDef.getQName().equals(propSet.getResourceType())) {
            url.setCollection(true);
        }
        return url;
    }
    
    private void addPropertyToPropertySetElement(Path uri, Element propSetElement,
                                                 Property prop, SearchEnvironment envir) {
        
        PropertyTypeDefinition propDef = prop.getDefinition();
        
        Document doc = propSetElement.getOwnerDocument();
        
        Element propertyElement = doc.createElement("property");
        
        String namespaceUri = propDef.getNamespace().getUri();
        if (namespaceUri != null) {
            propertyElement.setAttribute("namespace", namespaceUri);
        }
        
        String prefix = propDef.getNamespace().getPrefix();
        if (prefix != null) {
            propertyElement.setAttribute("name", prefix + ":" + propDef.getName());
        } else {
            propertyElement.setAttribute("name", propDef.getName());
        }
        
        Locale locale = envir.getLocale();

        Set<String> formatSet = envir.getFormats().getFormats(propDef);
        if (!formatSet.contains(null)) {
            // Add default (null) format:
            formatSet.add(null);
        }

        for (String format: formatSet) {
            if (propDef.isMultiple()) {
                Element valuesElement = doc.createElement("values");
                if (format != null) {
                    valuesElement.setAttribute("format", format);
                }
                for (Value v: prop.getValues()) {
                    Element valueElement = getPropertyElement(propDef,uri, v, format, locale, doc);
                    valuesElement.appendChild(valueElement);
                }
                propertyElement.appendChild(valuesElement);
            } else {
                Value value = prop.getValue();
                Element valueElement = getPropertyElement(propDef,uri, value, format, locale, doc);
                if (format != null) {
                    valueElement.setAttribute("format", format);
                }
                propertyElement.appendChild(valueElement);
            }
        }
        propSetElement.appendChild(propertyElement);
    }

    

    private Element getPropertyElement(PropertyTypeDefinition propDef, 
                                             Path uri, Value value, String format, 
                                             Locale locale, Document doc) {

        String valueString = propDef.getValueFormatter().valueToString(value, format, locale);

        Node node = null;
        // If string value and format is url, try to create url (if it doesn't start with http?)
        if (format != null && (propDef.getType() == STRING || propDef.getType() == IMAGE_REF)) {

            if (format.equals("url") && !valueString.startsWith("http")) {
                if (!valueString.startsWith("/")) {
                    valueString = uri.getParent().toString() + "/" + valueString;
                }
                try {
                    valueString = this.linkToService.constructLink(Path.fromString(valueString));
                } catch (Exception e) {
                    logger.warn(valueString + " led to exception ", e);
                }
            } 

        } else if (format != null && propDef.getType() == PropertyType.Type.HTML) {
            if (format.equals("flattened")) {
                try {
                    StringBuilder sb = new StringBuilder();
                    HtmlFragment fragment = this.htmlParser.parseFragment(valueString);
                    for (HtmlContent c : fragment.getContent()) {
                        sb.append(flatten(c));
                    }
                    node = doc.createTextNode(sb.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } 
          
        if (node == null) {
            node = doc.createTextNode(valueString);
        }
        Element valueElement = doc.createElement("value");
        valueElement.appendChild(node);
        return valueElement;
    }
    
    private String flatten(HtmlContent c) {
        StringBuilder sb = new StringBuilder();
        if (c instanceof HtmlElement) {
            HtmlElement htmlElement = (HtmlElement) c;
            for (HtmlContent child : htmlElement.getChildNodes()) {
                sb.append(flatten(child));
            }
        } else if (c instanceof HtmlText) {
            String content = c.getContent();
            sb.append(processHtmlEntities(content));
        }
        return sb.toString();
    }
    
    private String processHtmlEntities(String content) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '&') {
                int j = i + 1;

                String entity = null;
                while (j < content.length()) {
                    boolean validChar = validEntityCharAtIndex(content, j);
                    if (!validChar && content.charAt(j) == ';' && j > i + 1) {
                        entity = content.substring(i + 1, j);
                        i = j;
                        break;
                    } else if (!validChar) {
                        break;
                    }
                    j++;
                }
                if (entity != null) {
                    if (this.htmlEntityMap.containsKey(entity)) {
                        result.append(this.htmlEntityMap.get(entity));
                    } else {
                        result.append("&").append(entity).append(";");
                    }
                } 
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
    
    private boolean validEntityCharAtIndex(String content, int i) {
        if (i >= content.length()) return false;
        char c = content.charAt(i);
        return i < content.length() && c != ';' 
            && (('a' <= c && 'z' >= c) || ('A' <= c && 'Z' >= c));
    }
    

    private class Formats {

        private Map<PropertyTypeDefinition, Set<String>> formats = 
            new HashMap<PropertyTypeDefinition, Set<String>>();

        public void addFormat(PropertyTypeDefinition def, String format) {
            Set<String> s = this.formats.get(def);
            if (s == null) {
                s = new HashSet<String>();
                this.formats.put(def, s);
            }
            s.add(format);
        }

        public Set<String> getFormats(PropertyTypeDefinition def) {
            Set<String> set = this.formats.get(def);
            if (set == null) {
                set = new HashSet<String>();
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
            this.sort = parser.parseSortString(sort);
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
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(": select = ").append(this.select);
            sb.append(", sort = ").append(this.sort);
            sb.append(", formats = ").append(this.formats);
            sb.append(", locale = ").append(this.locale);
            return sb.toString();
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
            List<String> fieldsArray = splitFields(fields);
            ConfigurablePropertySelect selectedFields = new ConfigurablePropertySelect();
            this.select = selectedFields;

            for (String fullyQualifiedName: fieldsArray) {
                if ("".equals(fullyQualifiedName.trim())) {
                    continue;
                }
                fullyQualifiedName = fullyQualifiedName.replaceAll("\\,", ",");
                String prefix = null;
                String name = fullyQualifiedName.trim();

                if (PropertySet.URI_IDENTIFIER.equals(name)) {
                    this.reportUri = true;
                    continue;
                } else if (PropertySet.NAME_IDENTIFIER.equals(name)) {
                    this.reportName = true;
                    continue;
                } else if (PropertySet.TYPE_IDENTIFIER.equals(name)) {
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
                Path uri = requestContext.getResourceURI();
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
        private List<String> splitFields(String fields) {
            List<String> results = new ArrayList<String>();
            
            StringBuilder field = new StringBuilder();
            boolean insideBrackets = false;
            for (int i = 0; i < fields.length(); i++) {
                if (',' == fields.charAt(i) && !insideBrackets) {
                    results.add(field.toString());
                    field = new StringBuilder();
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
                    field.append(fields.charAt(i));
                }
            }

            if (field.length() > 0) {
                results.add(field.toString());
            }
            
            return results;
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

    @Required
    public void setLinkToService(Service linkToService) {
        this.linkToService = linkToService;
    }

    public void setCollectionResourceTypeDef(
            ResourceTypeDefinition collectionResourceTypeDef) {
        this.collectionResourceTypeDef = collectionResourceTypeDef;
    }

    @Required
    public void setParser(Parser parser) {
        this.parser = parser;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    public void setHtmlEntityMap(Map<String, String> htmlEntityMap) {
        this.htmlEntityMap = htmlEntityMap;
    }
}
