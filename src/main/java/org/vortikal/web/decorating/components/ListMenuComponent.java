/* Copyright (c) 2007, 2008, University of Oslo, Norway
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
package org.vortikal.web.decorating.components;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.NameTermQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.vortikal.web.view.components.menu.ListMenu;
import org.vortikal.web.view.components.menu.MenuItem;

/**
 * <p>
 * The following input is evaluated
 * <ul>
 * <li>uri - the collection uri to create a menu from</li>
 * <li>include-children - if specified, only these child names is search for</li>
 * <li>exclude-children - alternative to include-children, exclude named children</li>
 * <li>depth - if greater than one and current resource is <b>below</b> uri, build sub menus</li>
 * <li>include-parent-folder - include uri collection first in menu</li>
 * <li>authenticated - default is listing only read-for-all resources</li>
 * </ul>
 * 
 */
public class ListMenuComponent extends ViewRenderingDecoratorComponent {

    private static final int DEFAULT_DEPTH = 1;
    private static final int DEFAULT_SEARCH_LIMIT = 500;

    private static final String STYLE_NONE = "none";
    private static final String STYLE_VERTICAL = "vertical";
    private static final String STYLE_HORIZONTAL = "horizontal";
    private static final String STYLE_TABS = "tabs";

    private static final String DEFAULT_STYLE = STYLE_NONE;

    private static final Set<String> VALID_STYLES;
    static {
        VALID_STYLES = new HashSet<String>();
        VALID_STYLES.add(STYLE_NONE);
        VALID_STYLES.add(STYLE_VERTICAL);
        VALID_STYLES.add(STYLE_HORIZONTAL);
        VALID_STYLES.add(STYLE_TABS);
    }

    private static final String PARAMETER_INCLUDE_CHILDREN = "include-children";
    private static final String PARAMETER_INCLUDE_CHILDREN_DESC = "An explicit listing of the child resources to include. (Only applicable for resources at level 1.)";

    private static final String PARAMETER_EXCLUDE_CHILDREN = "exclude-children";
    private static final String PARAMETER_EXCLUDE_CHILDREN_DESC = "A listing of child resources to exclude (cannot be used in conjunction with '"
            + PARAMETER_INCLUDE_CHILDREN + "').";

    private static final String PARAMETER_INCLUDE_PARENT = "include-parent-folder";
    private static final String PARAMETER_INCLUDE_PARENT_DESC = "Whether or not to include the selected folder itself in the menu. Defaults to 'false'.";

    private static final String PARAMETER_PARENT_FOLDER_TITLE = "parent-folder-title";
    private static final String PARAMETER_PARENT_FOLDER_TITLE_DESC = "Overrides the parent folder title if include-parent-folder is set to true.";

    private static final String PARAMETER_STYLE = "style";
    private static final String PARAMETER_STYLE_DESC = "Defines the style of the menu. Must be one of "
            + VALID_STYLES.toString() + ". Defaults to " + DEFAULT_STYLE + ".";

    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_URI_DESC = "The URI (path) to the selected folder.";

    private static final String PARAMETER_AUTENTICATED = "authenticated";
    private static final String PARAMETER_AUTENTICATED_DESC = "The default is that only resources readable for everyone is listed. "
            + "If this is set to 'true', the listing is done as the currently " + "logged in user (if any).";

    private static final String PARAMETER_DEPTH = "depth";
    private static final String PARAMETER_DEPTH_DESC = "Specifies the number of levels to retrieve subfolders for. The default value is '1', which retrieves the top level.";

    private static final String PARAMETER_DISPLAY_FROM_LEVEL = "display-from-level";
    private static final String PARAMETER_DISPLAY_FROM_LEVEL_DESC = "Defines the starting URI level for the menu (cannot be used with the '"
            + PARAMETER_URI + "' parameter)";

    private Service viewService;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition hiddenPropDef;
    private PropertyTypeDefinition importancePropdef;
    private ResourceTypeDefinition collectionResourceType;
    private PropertyTypeDefinition navigationTitlePropDef;
    private String modelName = "menu";
    private int searchLimit = DEFAULT_SEARCH_LIMIT;
    private Searcher searcher;


    public void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        MenuRequest menuRequest = new MenuRequest(request);

        int currentLevel = menuRequest.getCurrentFolder().getDepth() + 1;

        if (menuRequest.getDisplayFromLevel() != -1) {
            if (currentLevel < menuRequest.getDisplayFromLevel()) {
                return;
            }
        }

        // Build main menu
        ListMenu<PropertySet> menu = buildMainMenu(menuRequest);

        // Add sub menu?
        MenuItem<PropertySet> activeItem = menu.getActiveItem();
        if (activeItem != null && menuRequest.getDepth() > 1) {
            ListMenu<PropertySet> submenu = buildSubMenu(menuRequest);
            if (submenu != null) {
                activeItem.setSubMenu(submenu);
            }
        }

        model.put(this.modelName, menu);
    }


    private ListMenu<PropertySet> buildMainMenu(MenuRequest menuRequest) {

        ListMenu<PropertySet> menu = new ListMenu<PropertySet>();
        menu.setLabel(menuRequest.getStyle());

        Query query = buildMainSearch(menuRequest);
        ResultSet rs = search(menuRequest.getToken(), query);

        Path currentURI = menuRequest.getCurrentURI();
        String[] childNames = menuRequest.getChildNames();
        MenuItem<PropertySet> parent = null;

        List<MenuItem<PropertySet>> items = new ArrayList<MenuItem<PropertySet>>();
        Map<String, MenuItem<PropertySet>> nameItemMap = new HashMap<String, MenuItem<PropertySet>>();

        for (int i = 0; i < rs.getSize(); i++) {
            PropertySet resource = rs.getResult(i);

            MenuItem<PropertySet> item = buildItem(resource);
            Path uri = resource.getURI();

            // Hidden?
            if (childNames == null && this.hiddenPropDef != null && resource.getProperty(this.hiddenPropDef) != null) {
                continue;
            }

            // Parent?
            if (uri.equals(menuRequest.getURI())) {
                parent = item;

                String title = menuRequest.getParentTitle();

                if (title != null) {
                    if (!title.equals("")) {
                        parent.setTitle(title);
                    }
                }

                continue;
            }

            // Active item?
            if (isActive(currentURI, uri)) {
                item.setActive(true);
                menu.setActiveItem(item);
            }

            nameItemMap.put(resource.getName(), item);
            items.add(item);
        }

        if (childNames != null) {
            items = sortSpecifiedOrder(childNames, nameItemMap);
        } else {
            items = sortDefaultOrder(items, menuRequest.getLocale());
        }

        // Insert parent first in list
        if (parent != null) {
            parent.setLabel(parent.getLabel() + " parent-folder");
            items.add(0, parent);

            if (menu.getActiveItem() == null
                    && (menuRequest.getURI().isAncestorOf(menuRequest.getCurrentURI()) || menuRequest.getURI().equals(
                            menuRequest.getCurrentURI()))) {
                parent.setActive(true);
                menu.setActiveItem(parent);
            }
        }

        menu.addAllItems(items);

        return menu;
    }


    private Query buildMainSearch(MenuRequest menuRequest) {
        Path uri = menuRequest.getURI();
        int startDepth = uri.getDepth() + 1;
        AndQuery query = new AndQuery();

        String[] childNames = menuRequest.getChildNames();

        if (childNames == null) {
            // Get all children
            query.add(getChildrenQuery(uri, startDepth));

            // Excluded children?
            String[] excludedChildren = menuRequest.getExcludedChildren();
            if (excludedChildren != null) {
                query.add(getExcludedChildrenQuery(excludedChildren));
            }

        } else {
            query.add(getRequestedChildren(uri, childNames));
        }

        if (menuRequest.isParentIncluded()) {
            OrQuery or = new OrQuery();
            or.add(query);
            or.add(new UriTermQuery(uri.toString(), TermOperator.EQ));
            return or;
        }
        return query;
    }


    private Query getRequestedChildren(Path uri, String[] childNames) {

        if (childNames.length == 1) {
            String name = childNames[0];
            return getUriQuery(uri, name);
        }

        // An explicit list of child names is provided
        OrQuery orQ = new OrQuery();
        for (int i = 0; i < childNames.length; i++) {
            String name = childNames[i];
            orQ.add(getUriQuery(uri, name));
        }
        return orQ;

    }


    private Query getUriQuery(Path uri, String name) {
        if (name.indexOf("/") != -1) {
            throw new DecoratorComponentException("Invalid child name: '" + name + "'");
        }
        String childURI = uri.extend(name.trim()).toString();
        return new UriTermQuery(childURI, TermOperator.EQ);
    }


    private Query getExcludedChildrenQuery(String[] excludedChildren) {
        AndQuery query = new AndQuery();

        for (String child : excludedChildren) {
            if (child.indexOf("/") != -1) {
                throw new DecoratorComponentException("Invalid excluded child name: '" + child + "'");
            }
            child = child.trim();
            query.add(new NameTermQuery(child, TermOperator.NE));
        }

        return query;
    }


    // List all children based on depth:
    private Query getChildrenQuery(Path uri, int depth) {
        AndQuery q = new AndQuery();
        String uriString = uri.toString();
        if (!uri.isRoot()) {
            uriString += "/";
        }
        q.add(new UriPrefixQuery(uriString));
        q.add(new UriDepthQuery(depth));
        return q;
    }


    private ResultSet doSubSearch(MenuRequest menuRequest) {

        OrQuery orQuery = new OrQuery();

        int startDepth = menuRequest.getURI().getDepth() + 1;
        List<Path> uris = menuRequest.getCurrentURI().getPaths();
        int maxDepth = startDepth + menuRequest.getDepth() - 1;
        maxDepth = Math.min(maxDepth, uris.size());

        for (int i = startDepth; i < maxDepth; i++) {
            Path uri = uris.get(i);
            orQuery.add(getChildrenQuery(uri, i + 1));
        }

        return search(menuRequest.getToken(), orQuery);
    }


    private ResultSet search(String token, Query query) {

        // We are searching for collections
        AndQuery q = new AndQuery();
        q.add(new TypeTermQuery(this.collectionResourceType.getQName(), TermOperator.IN));
        q.add(query);

        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        select.addPropertyDefinition(this.titlePropDef);
        select.addPropertyDefinition(this.navigationTitlePropDef);
        if (this.hiddenPropDef != null) {
            select.addPropertyDefinition(this.hiddenPropDef);
        }
        if (this.importancePropdef != null) {
            select.addPropertyDefinition(this.importancePropdef);
        }

        Search search = new Search();
        search.setSorting(null);
        search.setQuery(q);
        search.setLimit(this.searchLimit);
        search.setPropertySelect(select);

        return this.searcher.execute(token, search);
    }


    private boolean isActive(Path currentURI, Path uri) {
        return currentURI.equals(uri) || uri.isAncestorOf(currentURI);
    }


    private List<MenuItem<PropertySet>> sortSpecifiedOrder(String[] childNames,
            Map<String, MenuItem<PropertySet>> nameItemMap) {

        List<MenuItem<PropertySet>> result = new ArrayList<MenuItem<PropertySet>>();
        for (String name : childNames) {
            name = name.trim();
            MenuItem<PropertySet> item = nameItemMap.get(name);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }


    private List<MenuItem<PropertySet>> sortDefaultOrder(List<MenuItem<PropertySet>> items, Locale locale) {
        Collections.sort(items, new ListMenuComparator(locale, this.importancePropdef, this.navigationTitlePropDef));
        return items;
    }


    /**
     * Add sub menu if current uri is below uri
     */
    private ListMenu<PropertySet> buildSubMenu(MenuRequest menuRequest) {

        ResultSet rs = doSubSearch(menuRequest);

        if (rs == null || rs.getSize() == 0) {
            return null;
        }

        Map<Path, List<PropertySet>> childMap = new HashMap<Path, List<PropertySet>>();

        for (PropertySet resource : rs.getAllResults()) {

            Path uri = resource.getURI();
            Path parentURI = uri.getParent();

            List<PropertySet> childList = childMap.get(parentURI);
            if (childList == null) {
                childList = new ArrayList<PropertySet>();
                childMap.put(parentURI, childList);
            }

            // Hidden?
            if (this.hiddenPropDef != null && resource.getProperty(this.hiddenPropDef) != null) {
                continue;
            }
            childList.add(resource);
        }

        int rootDepth = menuRequest.getURI().getDepth() + 1;
        List<Path> uris = menuRequest.getCurrentURI().getPaths();
        Path rootUri = uris.get(rootDepth);

        return buildSubItems(rootUri, childMap, menuRequest);
    }


    private ListMenu<PropertySet> buildSubItems(Path childrenKey, Map<Path, List<PropertySet>> childMap,
            MenuRequest menuRequest) {

        List<MenuItem<PropertySet>> items = new ArrayList<MenuItem<PropertySet>>();
        List<PropertySet> children = childMap.get(childrenKey);

        if (children == null) {
            return null;
        }

        for (PropertySet resource : children) {
            MenuItem<PropertySet> item = buildItem(resource);

            items.add(item);

            if (isActive(menuRequest.getCurrentURI(), resource.getURI())) {
                item.setSubMenu(buildSubItems(resource.getURI(), childMap, menuRequest));
                item.setActive(true);
            }
        }
        items = sortDefaultOrder(items, menuRequest.getLocale());
        ListMenu<PropertySet> submenu = new ListMenu<PropertySet>();
        submenu.addAllItems(items);
        return submenu;
    }


    private MenuItem<PropertySet> buildItem(PropertySet resource) {
        MenuItem<PropertySet> item = new MenuItem<PropertySet>(resource);

        // Url
        Path uri = resource.getURI();
        URL url = this.viewService.constructURL(uri);
        // Know it's a folder, append "/"
        url.setCollection(true);
        item.setUrl(url);

        // Label
        String label = resource.getName().replace(' ', '-');
        if (label.equals("/")) {
            label = "root-folder";
        }
        item.setLabel(label);

        // Title
        String title = null;
        Property navigationTitleProperty = resource.getProperty(this.navigationTitlePropDef);
        if (navigationTitleProperty != null) {
            title = navigationTitleProperty.getStringValue();
        }
        if (title == null) {
            Property titleProperty = resource.getProperty(this.titlePropDef);
            title = (titleProperty != null) ? titleProperty.getStringValue(): label;
        }
        item.setTitle(title);

        return item;
    }

    private class MenuRequest {
        private Path uri;
        private int displayFromLevel = -1;
        private Path currentURI;
        private Path currentFolder;
        private boolean parentIncluded;
        private String parentTitle;
        private String[] childNames;
        private String style;
        private int depth = DEFAULT_DEPTH;
        private Locale locale;
        private String token;
        private String[] excludedChildren;


        public MenuRequest(DecoratorRequest request) {
            RequestContext requestContext = RequestContext.getRequestContext();
            this.currentURI = requestContext.getResourceURI();
            this.currentFolder = requestContext.getCurrentCollection();
            this.locale = request.getLocale();

            String uri = request.getStringParameter(PARAMETER_URI);
            String displayFromLevel = request.getStringParameter(PARAMETER_DISPLAY_FROM_LEVEL);

            if ((uri == null || "".equals(uri.trim()))
                    && (displayFromLevel == null || "".equals(displayFromLevel.trim()))) {
                throw new DecoratorComponentException("One of parameters '" + PARAMETER_URI + "' or '"
                        + PARAMETER_DISPLAY_FROM_LEVEL + "' must be specified");
            }
            if ((uri != null && !"".equals(uri.trim()))
                    && (displayFromLevel != null && !"".equals(displayFromLevel.trim()))) {
                throw new DecoratorComponentException("At most one of parameters '" + PARAMETER_URI + "' or '"
                        + PARAMETER_DISPLAY_FROM_LEVEL + "' can be specified");
            }

            if (uri != null && !"".equals(uri.trim())) {
                if (!"/".equals(uri) && uri.endsWith("/")) {
                    uri = uri.substring(0, uri.length() - 1);
                }
                this.uri = Path.fromString(uri);
            }

            if (displayFromLevel != null && !"".equals(displayFromLevel.trim())) {
                int level = Integer.parseInt(displayFromLevel);
                if (level <= 0) {
                    throw new DecoratorComponentException("Parameter '" + PARAMETER_DISPLAY_FROM_LEVEL
                            + "' must be an integer > 0");
                }

                if (level <= requestContext.getCurrentCollection().getPaths().size()) {
                    this.uri = requestContext.getCurrentCollection().getPaths().get(level - 1);
                }
                this.displayFromLevel = level;
            }

            boolean authenticated = "true".equals(request.getStringParameter(PARAMETER_AUTENTICATED));
            if (authenticated) {
                SecurityContext securityContext = SecurityContext.getSecurityContext();
                this.token = securityContext.getToken();
            }

            this.style = request.getStringParameter(PARAMETER_STYLE);
            if (this.style == null) {
                this.style = DEFAULT_STYLE;
            } else if (!VALID_STYLES.contains(this.style)) {
                throw new DecoratorComponentException("Invalid value for parameter 'style': must be one of "
                        + VALID_STYLES.toString());
            }

            this.parentIncluded = "true".equals(request.getStringParameter(PARAMETER_INCLUDE_PARENT));

            if (this.parentIncluded) {
                if (request.getStringParameter(PARAMETER_PARENT_FOLDER_TITLE) != null) {
                    this.parentTitle = request.getStringParameter(PARAMETER_PARENT_FOLDER_TITLE);
                }

            }

            String includeChildrenParam = request.getStringParameter(PARAMETER_INCLUDE_CHILDREN);
            if (includeChildrenParam != null) {
                childNames = includeChildrenParam.split(",");
                if (childNames.length == 0) {
                    throw new DecoratorComponentException("Invalid value for parameter '" + PARAMETER_INCLUDE_CHILDREN
                            + "': must provide at least one child name");
                }
            }
            String excludeChildrenParam = request.getStringParameter(PARAMETER_EXCLUDE_CHILDREN);
            if (excludeChildrenParam != null) {
                if (this.childNames != null) {
                    throw new DecoratorComponentException("Cannot use both parameters '" + PARAMETER_INCLUDE_CHILDREN
                            + "' and '" + PARAMETER_EXCLUDE_CHILDREN + "'");
                }
                this.excludedChildren = excludeChildrenParam.split(",");
                if (this.excludedChildren.length == 0) {
                    throw new DecoratorComponentException("Invalid value for parameter '" + PARAMETER_EXCLUDE_CHILDREN
                            + "': must provide at least one child name");
                }
            }
            String depthStr = request.getStringParameter(PARAMETER_DEPTH);
            if (depthStr != null) {
                try {
                    int depth = Integer.parseInt(depthStr);
                    if (depth < 1) {
                        throw new IllegalArgumentException("Depth must be an integer >= 1");
                    }
                    this.depth = depth;
                } catch (Throwable t) {
                    throw new DecoratorComponentException("Illegal value for parameter '" + PARAMETER_DEPTH + "': "
                            + depthStr);
                }
            }
        }


        public String[] getExcludedChildren() {
            return this.excludedChildren;
        }


        public Path getURI() {
            return this.uri;
        }


        public int getDisplayFromLevel() {
            return this.displayFromLevel;
        }


        public Path getCurrentURI() {
            return this.currentURI;
        }


        public Path getCurrentFolder() {
            return this.currentFolder;
        }


        public boolean isParentIncluded() {
            return this.parentIncluded;
        }


        public String getParentTitle() {
            return this.parentTitle;
        }


        public String[] getChildNames() {
            return this.childNames;
        }


        public String getStyle() {
            return this.style;
        }


        public int getDepth() {
            return this.depth;
        }


        public Locale getLocale() {
            return this.locale;
        }


        public String getToken() {
            return token;
        }
    }

    private class ListMenuComparator implements Comparator<MenuItem<PropertySet>> {

        private Collator collator;
        private PropertyTypeDefinition importancePropertyDef;
        private PropertyTypeDefinition navigationTitlePropDef;


        public ListMenuComparator(Locale locale, PropertyTypeDefinition importancePropertyDef,
                PropertyTypeDefinition navigationTitlePropDef) {
            this.collator = Collator.getInstance(locale);
            this.importancePropertyDef = importancePropertyDef;
            this.navigationTitlePropDef = navigationTitlePropDef;
        }


        public int compare(MenuItem<PropertySet> i1, MenuItem<PropertySet> i2) {
            if (this.importancePropertyDef != null) {
                int importance1 = 0, importance2 = 0;
                if (i1.getValue().getProperty(this.importancePropertyDef) != null) {
                    importance1 = i1.getValue().getProperty(this.importancePropertyDef).getIntValue();
                }
                if (i2.getValue().getProperty(this.importancePropertyDef) != null) {
                    importance2 = i2.getValue().getProperty(this.importancePropertyDef).getIntValue();
                }
                if (importance1 != importance2) {
                    return importance2 - importance1;
                }
            }
            String x1 = null, x2 = null;
            if (i1.getValue().getProperty(this.navigationTitlePropDef) != null) {
                x1 = i1.getValue().getProperty(navigationTitlePropDef).getStringValue();
            }
            if (i2.getValue().getProperty(this.navigationTitlePropDef) != null) {
                x2 = i2.getValue().getProperty(navigationTitlePropDef).getStringValue();
            }
            String value1, value2;
            if (x1 != null) {
                value1 = x1;
            } else {
                value1 = i1.getTitle();
            }
            if (x2 != null) {
                value2 = x2;
            } else {
                value2 = i2.getTitle();
            }
            return this.collator.compare(value1, value2);
        }
    }


    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }


    @Required
    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }


    public void setHiddenPropDef(PropertyTypeDefinition hiddenPropDef) {
        this.hiddenPropDef = hiddenPropDef;
    }


    public void setImportancePropDef(PropertyTypeDefinition importancePropDef) {
        this.importancePropdef = importancePropDef;
    }


    @Required
    public void setCollectionResourceType(ResourceTypeDefinition collectionResourceType) {
        this.collectionResourceType = collectionResourceType;
    }


    @Required
    public void setNavigationTitlePropDef(PropertyTypeDefinition navigationTitlePropDef) {
        this.navigationTitlePropDef = navigationTitlePropDef;
    }


    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }


    public void setModelName(String modelName) {
        this.modelName = modelName;
    }


    public void setSearchLimit(int searchLimit) {
        if (searchLimit <= 0) {
            throw new IllegalArgumentException("JavaBean property 'searchLimit' must be a positive integer");
        }
        this.searchLimit = searchLimit;
    }


    protected String getDescriptionInternal() {
        return "Displays a menu based on the subfolders of a specified folder (path)";
    }


    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_URI, PARAMETER_URI_DESC);
        map.put(PARAMETER_STYLE, PARAMETER_STYLE_DESC);
        map.put(PARAMETER_INCLUDE_CHILDREN, PARAMETER_INCLUDE_CHILDREN_DESC);
        map.put(PARAMETER_EXCLUDE_CHILDREN, PARAMETER_EXCLUDE_CHILDREN_DESC);
        map.put(PARAMETER_INCLUDE_PARENT, PARAMETER_INCLUDE_PARENT_DESC);
        map.put(PARAMETER_PARENT_FOLDER_TITLE, PARAMETER_PARENT_FOLDER_TITLE_DESC);
        map.put(PARAMETER_AUTENTICATED, PARAMETER_AUTENTICATED_DESC);
        map.put(PARAMETER_DEPTH, PARAMETER_DEPTH_DESC);
        map.put(PARAMETER_DISPLAY_FROM_LEVEL, PARAMETER_DISPLAY_FROM_LEVEL_DESC);
        return map;
    }

}
