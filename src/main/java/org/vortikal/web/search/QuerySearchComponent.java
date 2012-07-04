/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.ResourceWrapper;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class QuerySearchComponent implements SearchComponent {

    private String name;
    private String titleLocalizationKey;
    private ResourceWrapperManager resourceManager;
    private List<PropertyDisplayConfig> listableProperties;
    private SearchSorting searchSorting;
    private List<String> configurablePropertySelectPointers;
    private ResourceTypeTree resourceTypeTree;

    protected Service viewService;

    protected abstract ResultSet getResultSet(HttpServletRequest request, Resource collection, String token,
            Sorting sorting, int searchLimit, int baseOffset, ConfigurablePropertySelect propertySelect);

    public Listing execute(HttpServletRequest request, Resource collection, int page, int pageLimit, int baseOffset)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.isViewUnauthenticated() ? null : requestContext.getSecurityToken();

        int offset = baseOffset + (pageLimit * (page - 1));
        int searchLimit = pageLimit + 1;

        Sorting sorting = null;
        String[] sortingParams = request.getParameterValues(Listing.SORTING_PARAM);
        if (sortingParams != null && sortingParams.length > 0) {
            sorting = new SortingImpl(this.searchSorting.getSortFieldsFromRequestParams(sortingParams));
        } else {
            sorting = new SortingImpl(this.searchSorting.getSortFields(collection));
        }

        ConfigurablePropertySelect propertySelect = null;
        if (this.configurablePropertySelectPointers != null && this.resourceTypeTree != null) {
            for (String propPointer : this.configurablePropertySelectPointers) {
                PropertyTypeDefinition ptd = this.resourceTypeTree.getPropertyDefinitionByPointer(propPointer);
                if (ptd != null) {
                    if (propertySelect == null) {
                        propertySelect = new ConfigurablePropertySelect();
                    }
                    propertySelect.addPropertyDefinition(ptd);
                }
            }
        }

        ResultSet result = this.getResultSet(request, collection, token, sorting, searchLimit, offset, propertySelect);

        boolean more = result.getSize() == pageLimit + 1;
        int num = result.getSize();
        if (more) {
            num--;
        }

        Map<String, URL> urls = new HashMap<String, URL>();
        List<PropertySet> files = new ArrayList<PropertySet>();
        for (int i = 0; i < num; i++) {
            PropertySet res = result.getResult(i);
            files.add(res);
            URL url = this.viewService.constructURL(res.getURI());
            Property urlProp = res.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearcher.URL_PROP_NAME);
            if (urlProp != null) {
                url = URL.parse(urlProp.getStringValue());
            }
            urls.put(res.getURI().toString(), url);
        }

        List<PropertyTypeDefinition> displayPropDefs = new ArrayList<PropertyTypeDefinition>();
        if (this.listableProperties != null) {
            for (PropertyDisplayConfig config : this.listableProperties) {
                Property hide = null;
                if (config.getPreventDisplayProperty() != null) {
                    hide = collection.getProperty(config.getPreventDisplayProperty());
                }
                if (hide == null) {
                    displayPropDefs.add(config.getDisplayProperty());
                }
            }
        }

        String title = null;
        if (this.titleLocalizationKey != null) {
            org.springframework.web.servlet.support.RequestContext springRequestContext = new org.springframework.web.servlet.support.RequestContext(
                    request);
            title = springRequestContext.getMessage(this.titleLocalizationKey, (String) null);
        }

        ResourceWrapper resourceWrapper = this.resourceManager.createResourceWrapper(collection);

        Listing listing = new Listing(resourceWrapper, title, name, offset);
        listing.setMore(more);
        listing.setFiles(files);
        listing.setUrls(urls);
        listing.setDisplayPropDefs(displayPropDefs);
        listing.setTotalHits(result.getTotalHits());
        listing.setSorting(sorting);
        return listing;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setResourceManager(ResourceWrapperManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Required
    public void setSearchSorting(SearchSorting searchSorting) {
        this.searchSorting = searchSorting;
    }

    public void setListableProperties(List<PropertyDisplayConfig> listableProperties) {
        this.listableProperties = listableProperties;
    }

    public void setTitleLocalizationKey(String titleLocalizationKey) {
        this.titleLocalizationKey = titleLocalizationKey;
    }

    public String getTitleLocalizationKey() {
        return titleLocalizationKey;
    }

    public void setConfigurablePropertySelectPointers(List<String> configurablePropertySelectPointers) {
        this.configurablePropertySelectPointers = configurablePropertySelectPointers;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
