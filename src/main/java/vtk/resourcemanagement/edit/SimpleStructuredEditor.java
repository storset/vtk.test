/* Copyright (c) 2014, University of Oslo, Norway
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
package vtk.resourcemanagement.edit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import vtk.repository.AuthorizationException;
import vtk.repository.Path;
import vtk.repository.Property;
import vtk.repository.Repository;
import vtk.repository.Repository.Depth;
import vtk.repository.Resource;
import vtk.repository.resourcetype.PropertyTypeDefinition;
import vtk.security.AuthenticationException;
import vtk.security.Principal;
import vtk.util.text.Json;
import vtk.util.text.JsonStreamer;
import vtk.web.RequestContext;
import vtk.web.service.Service;
import vtk.web.service.URL;

public class SimpleStructuredEditor implements Controller {

    public static final String ACTION_PARAMETER_VALUE_CANCEL = "cancel";
    public static final String ACTION_PARAMETER_VALUE_DELETE = "delete";
    public static final String ACTION_PARAMETER_VALUE_NEW = "new";
    public static final String ACTION_PARAMETER_VALUE_UPDATE = "update";

    private static final String TITLE_PARAMETER = "title";
    private String viewName;
    private PropertyTypeDefinition publishDatePropDef;
    private Service viewService;

    private String resourceType = "structured-message";
    private String[] properties = { "message", TITLE_PARAMETER };

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();

        model.put("url", URL.create(request));

        Resource currentResource = repository.retrieve(token, uri, false);
        model.put("isCollection", currentResource.isCollection());
        
        if ("POST".equals(request.getMethod())) {
            if (request.getParameter(ACTION_PARAMETER_VALUE_CANCEL) != null) {
                repository.unlock(token, uri, null);
                setRedirect(response, currentResource.isCollection() ? uri : uri.getParent(), null, null);
                return null;
            } else if (request.getParameter(ACTION_PARAMETER_VALUE_DELETE) != null) {
                // This is going to be replaced later
                repository.unlock(token, uri, null);
                repository.delete(token, uri, true);
                setRedirect(response, uri.getParent(), uri, ACTION_PARAMETER_VALUE_DELETE);
                return null;
            } else if (currentResource.isCollection() && request.getParameter("save") != null) {
                Path newUri = createNewDocument(request, repository, token, uri);
                setRedirect(response, uri, newUri, ACTION_PARAMETER_VALUE_NEW);
                return null;
            } else if (resourceType.equals(currentResource.getResourceType()) && request.getParameter("save") != null) {
                updateDocument(request, token, repository, uri);
                repository.unlock(token, uri, null);
                setRedirect(response, uri.getParent(), uri, ACTION_PARAMETER_VALUE_UPDATE);
                return null;
            }
        } else if (resourceType.equals(currentResource.getResourceType())) {
            // Edit some document
            Principal principal = requestContext.getPrincipal();
            repository.lock(token, uri, principal.getQualifiedName(), Depth.ZERO, 600, null);
            InputStream stream = repository.getInputStream(token, uri, false);
            Json.MapContainer document = Json.parseToContainer(stream).asObject();
            model.put("properties", document.get("properties"));
        } else {
            model.put("isNew", true);
        }
        return new ModelAndView(viewName, model);
    }

    private void setRedirect(HttpServletResponse response, Path collection, Path document, String action)
            throws IOException, Exception {
        response.addIntHeader("Refresh", 0);

        URL url = viewService.constructURL(collection);
        if (action != null || document != null) {
            url.addParameter("action", action);
            url.addParameter("uri", document.toString());
        }
        response.sendRedirect(url.toString());
    }

    @SuppressWarnings("unchecked")
    private void updateDocument(HttpServletRequest request, String token, Repository repository, Path uri)
            throws Exception, UnsupportedEncodingException {

        InputStream stream = repository.getInputStream(token, uri, false);
        Json.MapContainer document = Json.parseToContainer(stream).asObject();
        
        Map<String, String> propertyValues = (Map<String, String>) document.get("properties");
        for (String propertyName : properties) {
            propertyValues.put(propertyName, request.getParameter(propertyName));
        }
        document.put("properties", propertyValues);
        InputStream is = new ByteArrayInputStream(document.toString().getBytes("UTF-8"));
        repository.storeContent(token, uri, is);
    }

    private Path createNewDocument(HttpServletRequest request, Repository repository, String token, Path uri)
            throws Exception {
        Json.MapContainer document = new Json.MapContainer();
        document.put("resourcetype", resourceType);
        Map<String, String> propertyValues = new HashMap<String, String>();
        for (String propertyName : properties) {
            propertyValues.put(propertyName, request.getParameter(propertyName));
        }
        document.put("properties", propertyValues);

        String str = JsonStreamer.toJson(document);
        InputStream is = new ByteArrayInputStream(str.getBytes("utf-8"));
        Path newUri = generateFilename(request, repository, token, uri);

        Resource resource = repository.createDocument(token, newUri, is);
        publishResource(resource, token, repository);
        return newUri;
    }

    private Path generateFilename(HttpServletRequest request, Repository repository, String token, Path uri)
            throws AuthorizationException, AuthenticationException, Exception {

        String name = "message";
        if (request.getParameter(TITLE_PARAMETER) != null && !"".equals(request.getParameter(TITLE_PARAMETER))) {
            name = request.getParameter(TITLE_PARAMETER).toLowerCase();
            name = name.replace("æ", "ae").replace("ø", "o").replace("å", "aa");
            name = name.replaceAll("[^A-Za-z0-9 ]", "");
            name = name.replaceAll(" ", "-");
            if (name.length() > 40) {
                name = name.substring(0, 40);
            }
        }

        Path p = uri.expand(name + ".html");
        for (int i = 2; repository.exists(token, p); i++) {
            p = uri.expand(name + "-" + i + ".html");
        }

        return p;
    }

    private void publishResource(Resource resource, String token, Repository repository) throws Exception {
        Property publishDateProp = resource.getProperty(this.publishDatePropDef);
        if (publishDateProp == null) {
            publishDateProp = this.publishDatePropDef.createProperty();
            resource.addProperty(publishDateProp);
        }
        publishDateProp.setDateValue(Calendar.getInstance().getTime());
        repository.store(token, resource);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

}
