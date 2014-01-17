/* Copyright (c) 2013, University of Oslo, Norway
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

package org.vortikal.videoref;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


        
/**
 * Testing REST service ..
 */
public class VideoappCallbackService implements Controller {

    public static final int API_VERSION = 0;
    
    private String apiBaseUri;
    private String repositoryId;
    private View view;
    private VideoDaoSupport videoDaoSupport;
    private VideoUpdateTask videoUpdateTask;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String requestPath = request.getRequestURI();
        if (!requestPath.startsWith(apiBaseUri)) {
            throw new IllegalArgumentException("Unsupported request path: " + requestPath);
        }
        
        UriComponents baseUri = UriComponentsBuilder.fromPath(apiBaseUri).build();
        UriComponents requestUri = UriComponentsBuilder.fromPath(requestPath).build();
        List<String> baseUriSegments = baseUri.getPathSegments();
        List<String> requestUriSegments = requestUri.getPathSegments();
        
        final Map<String,Object> model = new HashMap<String,Object>();
        if (baseUriSegments.size() == requestUriSegments.size()) {
            String lastBaseSegment = baseUriSegments.get(baseUriSegments.size()-1);
            String lastRequestSegment = requestUriSegments.get(requestUriSegments.size()-1);
            if (!lastBaseSegment.equals(lastRequestSegment)) {
                handleNotFound(request, response, model);
            } else {
                handleApiMeta(request, response, model);
            }
        } else {
            List<String> apiPath = new ArrayList<String>(requestUriSegments.subList(baseUriSegments.size(), 
                                                requestUriSegments.size()));
            
            if ("videos".equals(apiPath.get(0))) {
                apiPath.remove(0);
                handleVideos(request, response, model, apiPath);
            } else {
                handleNotFound(request, response, model);
            }
        }
        
        return new ModelAndView(view, model);
    }
    
    private UriComponentsBuilder videosBaseUri() {
        return UriComponentsBuilder.fromPath(apiBaseUri).pathSegment("videos");
    }
    
    private UriComponentsBuilder resourcesUriTemplate() {
        return videosBaseUri().pathSegment("resources").queryParam("videoExternalId", "{videoExternalId}");
    }
    
    private UriComponentsBuilder videoUriTemplate() {
        return videosBaseUri().queryParam("videoExternalId", "{videoExternalId}");
    }
    
    private UriComponentsBuilder updateUriTemplate() {
        return videosBaseUri().pathSegment("notifyUpdate").queryParam("videoExternalId", "{videoExternalId}");
    }

    private void handleNotFound(HttpServletRequest request, HttpServletResponse response, 
            Map<String,Object> model) throws Exception {
        model.put("status", 404);
        model.put("json", errorBody("Not found"));
    }

    private void handleApiMeta(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> model) throws Exception {
        model.put("json", body("version", API_VERSION,
                               "description", "Vortex videoapp callback support service",
                               "videosBaseURI", videosBaseUri().build().toUriString()));
                               
    }
    
    private void handleVideos(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> model, List<String> subPaths) throws Exception {

        if (! subPaths.isEmpty()) {
            if (subPaths.size() == 1 && "resources".equals(subPaths.get(0))) {
                handleVideoRefResources(request, response, model);
                return;
            } 
            
            if (subPaths.size() == 1 && "notifyUpdate".equals(subPaths.get(0))) {
                handleUpdateNotification(request, response, model);
                return;
            }
            
            handleNotFound(request, response, model);
            return;
        }
        
        if (request.getParameter("videoExternalId") == null) {
            model.put("json", 
                    body("description",
                         "Listing not available. Use 'videoExternalId' param to get single video."));
            return;
        }
        
        String videoIdParam = request.getParameter("videoExternalId");
        VideoId videoId = videoIdFromInput(videoIdParam);
        if (videoId == null) {
            model.put("status", 404);
            model.put("json", errorBody("Invalid video id, expected parameter 'videoExternalId' to provide it."));
            return;
        }
        
        if (!isValidHost(videoId)) {
            model.put("status", 400);
            model.put("json", errorBody("Unexpected Vortex hostname in video id: " + videoId + ", expected host " + repositoryId));
            return;
        }
        
        int refCount = videoDaoSupport.countReferences(videoId);

        model.put("json", body("videoExternalId", videoId.toString(),
                               "refCount", refCount,
                               "refListURI", resourcesUriTemplate().buildAndExpand(videoId.toString()).toUriString(),
                               "notifyUpdateURI", updateUriTemplate()
                                       .buildAndExpand(videoId.toString())
                                       .toUriString()));
    }
    
    private void handleVideoRefResources(HttpServletRequest request,
            HttpServletResponse response, Map<String,Object> model) {

        if (request.getParameter("videoExternalId") == null) {
            model.put("status", 400);
            model.put("json", errorBody("Missing required parameter 'videoExternalId'"));
            return;
        }
        
        VideoId videoId = videoIdFromInput(request.getParameter("videoExternalId"));
        if (videoId == null) {
            model.put("status", 400);
            model.put("json", errorBody("Invalid video id: " + request.getParameter("videoExternalId")));
            return;
        }
        
        if (!isValidHost(videoId)) {
            model.put("status", 400);
            model.put("json", errorBody("Unexpected Vortex hostname in video id: " 
                    + videoId + ", expected host " + repositoryId));
            return;
        }
        
        model.put("json", videoDaoSupport.listURIs(videoId));
    }


    private void handleUpdateNotification(HttpServletRequest request, HttpServletResponse response,
            Map<String,Object> model) throws Exception {

        if (request.getParameter("videoExternalId") == null) {
            model.put("status", 400);
            model.put("json", errorBody("Missing required parameter 'videoExternalId'"));
            return;
        }
        
        VideoId videoId = videoIdFromInput(request.getParameter("videoExternalId"));
        if (videoId == null) {
            model.put("status", 400);
            model.put("json", errorBody("Invalid video id: " + request.getParameter("videoExternalId")));
            return;
        }
        
        if (!isValidHost(videoId)) {
            model.put("status", 400);
            model.put("json", errorBody("Unexpected Vortex hostname in video id: " 
                    + videoId + ", expected host " + repositoryId));
            return;
        }
        
        if (!"POST".equals(request.getMethod())) {
            model.put("status", 405);
            response.addHeader("Allow", "POST");
            model.put("json", errorBody("POST method required"));
            return;
        }
        
        // Refresh video data for all resources which reference the given video id
        // by notifying system job
        try {
            videoUpdateTask.videoUpdated(videoId);
        } catch (IllegalStateException e) {
            model.put("status", 507); // HTTP 507 "Insufficient storage"
            model.put("json", errorBody(e.getMessage()));
            return;
        }
        
        // Put something in response
        model.put("json", body("videoExternalId", videoId.toString(),
                               "notifyUpdateReceived", true));
    }
    
    private VideoId videoIdFromInput(String input) {
        if (input == null) return null;
        try {
            return VideoId.fromString(input);
        } catch (Exception e) {}
        
        return null;
    }
    
    private boolean isValidHost(VideoId videoId) {
        return repositoryId.equals(videoId.hostname());
    }
    
    private JSONObject body(Object... keyValue) {
        if (keyValue.length % 2 != 0) {
            throw new IllegalArgumentException("Require even number of parameters");
        }
        JSONObject body = new JSONObject();
        Object key = null;
        for (int i=0; i<keyValue.length; i++) {
            if (i % 2 == 0) {
                key = keyValue[i];
            } else {
                body.put(key, keyValue[i]);
            }
        }
        return body;
    }

    private JSONObject errorBody(String message) {
        return body("error", message);
    }

    /**
     * @param apiBaseUri the apiBaseUri to set
     */
    @Required
    public void setApiBaseUri(String apiBaseUri) {
        this.apiBaseUri = apiBaseUri;
    }

    /**
     * @param repositoryId the repositoryId to set
     */
    @Required
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    /**
     * @param videoDaoSupport the videoDaoSupport to set
     */
    @Required
    public void setVideoDaoSupport(VideoDaoSupport videoDaoSupport) {
        this.videoDaoSupport = videoDaoSupport;
    }

    /**
     * @param videoUpdateTask the videoUpdateTask to set
     */
    @Required
    public void setVideoUpdateTask(VideoUpdateTask videoUpdateTask) {
        this.videoUpdateTask = videoUpdateTask;
    }

    /**
     * @param view the view to set
     */
    @Required
    public void setView(View view) {
        this.view = view;
    }

}
