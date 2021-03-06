/* Copyright (c) 2004,2013, University of Oslo, Norway
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
package vtk.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import vtk.context.BaseContext;
import vtk.repository.Acl;
import vtk.repository.AuthorizationException;
import vtk.repository.Comment;
import vtk.repository.FailedDependencyException;
import vtk.repository.IllegalOperationException;
import vtk.repository.Path;
import vtk.repository.ReadOnlyException;
import vtk.repository.RecoverableResource;
import vtk.repository.Repository;
import vtk.repository.RepositoryException;
import vtk.repository.Resource;
import vtk.repository.ResourceLockedException;
import vtk.repository.ResourceNotFoundException;
import vtk.repository.ResourceOverwriteException;
import vtk.repository.Revision;
import vtk.repository.Revision.Type;
import vtk.repository.StoreContext;
import vtk.security.AuthenticationException;
import vtk.security.Principal;
import vtk.security.SecurityContext;
import vtk.util.repository.RepositoryWrapper;
import vtk.web.service.Service;
import vtk.web.service.URL;

/**
 * Request context. Lives throughout one request, and contains the servlet
 * request, the current {@link Service} and the requested resource URI. The
 * request context can be obtained from application code in the following way:
 * 
 * <pre>
 * RequestContext requestContext = RequestContext.getRequestContext();
 * </pre>
 * 
 */
public class RequestContext {

    public static final String PREVIEW_UNPUBLISHED_PARAM_NAME = "vrtxPreviewUnpublished";
    public static final String PREVIEW_UNPUBLISHED_PARAM_VALUE = "true";
    private static final String HTTP_REFERER = "Referer";

    private final HttpServletRequest servletRequest;
    private final SecurityContext securityContext;
    private final boolean inRepository;
    private final Repository repository;
    private final Service service;
    private final Path resourceURI;
    private final Path currentCollection;
    private final Path indexFileURI;
    private final boolean isIndexFile;
    private final boolean viewUnauthenticated;
    private List<Message> infoMessages = new ArrayList<Message>(0);
    private List<Message> errorMessages = new ArrayList<Message>(0);

    // Set on first invocation (otherwise JUnit tests fail):
    private RevisionWrapper revisionWrapper = null;
    

    /**
     * Creates a new request context.
     * 
     * @param servletRequest
     *            the current servlet request
     * @param service
     *            the resolved service
     * @param resource
     *            the current resource (may be null)
     * @param uri
     *            the URI of the current resource
     * @param indexFileURI
     *            the URI of the current index file
     * @param viewUnauthenticated
     *            (<code>null</code> if no index file exists)
     */
    public RequestContext(HttpServletRequest servletRequest, SecurityContext securityContext, Service service,
            Resource resource, Path uri, Path indexFileURI, boolean isIndexFile, boolean viewUnauthenticated,
            boolean inRepository, Repository repository) {
        this.servletRequest = servletRequest;
        this.securityContext = securityContext;
        this.indexFileURI = indexFileURI;
        this.service = service;
        this.isIndexFile = isIndexFile;
        this.viewUnauthenticated = viewUnauthenticated;
        this.repository = repository;
        this.inRepository = inRepository;
        if (resource != null) {
            this.resourceURI = resource.getURI();
            if (resource.isCollection()) {
                this.currentCollection = resource.getURI();
            } else {
                this.currentCollection = resource.getURI().getParent();
            }
        } 
        else {
            this.resourceURI = uri;
            this.currentCollection = null;
        }
        
    }

    public static void setRequestContext(RequestContext requestContext) {
        BaseContext ctx = BaseContext.getContext();
        ctx.setAttribute(RequestContext.class.getName(), requestContext);
    }

    public static boolean exists() {
        if (BaseContext.exists()) {
            return BaseContext.getContext().getAttribute(RequestContext.class.getName()) != null;
        }
        return false;
    }

    /**
     * Gets the current request context.
     * 
     */
    public static RequestContext getRequestContext() {
        BaseContext ctx = BaseContext.getContext();
        RequestContext requestContext = (RequestContext) ctx.getAttribute(RequestContext.class.getName());
        return requestContext;
    }

    /**
     * Gets the current servlet request.
     * 
     * @return the servlet request
     */
    public HttpServletRequest getServletRequest() {
        return this.servletRequest;
    }

    /**
     * Gets the request URL
     */
    public URL getRequestURL() {
        return URL.create(this.servletRequest);
    }

    /**
     * Gets the current {@link Service} that this request executes under.
     * 
     * @return the service, or <code>null</code> if there is no current service.
     */
    public Service getService() {
        return this.service;
    }

    /**
     * Gets the {@link vtk.repository.Resource#getURI URI} that the
     * current request maps to.
     * 
     * @return the URI of the requested resource.
     */
    public Path getResourceURI() {
        return this.resourceURI;
    }

    /**
     * Gets the URI of the current collection. If the request is for a
     * collection, the current collection and {@link #getResourceURI resource
     * URI} are the same, otherwise the current collection is the nearest
     * collection towards the root.
     */
    public Path getCurrentCollection() {
        return this.currentCollection;
    }

    /**
     * Gets the index file URI.
     * 
     * @return the index file URI, or <code>null</code> if this is not an index
     *         file request.
     */
    public Path getIndexFileURI() {
        return this.indexFileURI;
    }

    public void addInfoMessage(Message msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        this.infoMessages.add(msg);
    }

    public List<Message> getInfoMessages() {
        return Collections.unmodifiableList(this.infoMessages);
    }

    public void addErrorMessage(Message msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        this.errorMessages.add(msg);
    }

    public List<Message> getErrorMessages() {
        return Collections.unmodifiableList(this.errorMessages);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append(": [");
        sb.append("resourceURI = ").append(this.resourceURI);
        sb.append(", service = ").append(this.service.getName());
        sb.append(", servletRequest = ").append(this.servletRequest);
        sb.append("]");
        return sb.toString();
    }

    public boolean isIndexFile() {
        return isIndexFile;
    }

    /**
     * This flag will be set to <code>true</code> if request should be processed
     * for viewing as unauthenticated principal.
     */
    public boolean isViewUnauthenticated() {
        return this.viewUnauthenticated;
    }

    public boolean isInRepository() {
        return this.inRepository;
    }

    public Repository getRepository() {
        if (revisionWrapper != null) return revisionWrapper;
        if (servletRequest != null && servletRequest.getParameter("revision") != null 
                && isPreviewUnpublished()) {
            this.revisionWrapper = new RevisionWrapper(
                    this.repository, resourceURI, servletRequest.getParameter("revision"));
            return this.revisionWrapper;
        }
        return this.repository;
    }

    public String getSecurityToken() {
        return this.securityContext.getToken();
    }

    public Principal getPrincipal() {
        return this.securityContext.getPrincipal();
    }

    public RepositoryTraversal rootTraversal(String token, Path uri) {
        return new RepositoryTraversal(this.repository, token, uri);
    }

    public static RepositoryTraversal rootTraversal(Repository repository, String token, Path uri) {
        return new RepositoryTraversal(repository, token, uri);
    }

    public static final class RepositoryTraversal {
        private Repository repository;
        private String token;
        private Path uri;

        private RepositoryTraversal(Repository repository, String token, Path uri) {
            if (repository == null) {
                throw new IllegalArgumentException("Repository is NULL");
            }
            this.repository = repository;
            this.token = token;
            this.uri = uri;
        }

        public void traverse(TraversalCallback callback) {
            Path uri = this.uri;
            while (uri != null) {
                try {
                    Resource resource = this.repository.retrieve(this.token, uri, true);
                    if (!callback.callback(resource)) {
                        return;
                    }
                } catch (Throwable t) {
                    if (!callback.error(uri, t)) {
                        return;
                    }
                }
                uri = uri.getParent();
            }
        }
    }

    public static interface TraversalCallback {
        public boolean callback(Resource resource);

        public boolean error(Path uri, Throwable error);
    }

    public boolean isPreviewUnpublished() {
    	boolean result = false;
        if (servletRequest != null) {
            result = servletRequest.getParameter(PREVIEW_UNPUBLISHED_PARAM_NAME) != null;
            if (!result) {
                String referer = servletRequest.getHeader(HTTP_REFERER);
                if (referer != null) {
                    try {
                        URL refererUrl = URL.parse(referer);
                        result = refererUrl.getParameter(PREVIEW_UNPUBLISHED_PARAM_NAME) != null;
                    } catch (Exception e) {
                        //probably invalid url
                        result = false;
                    }
                }
            }
        }
        return result;
    }
    
    private static class RevisionWrapper extends RepositoryWrapper {
        private Path uri;
        private String revision;
        private Map<String, Revision> cache = new HashMap<>();
        
        public RevisionWrapper(Repository repository, Path uri, String revision) {
            super(repository);
            this.uri = uri;
            this.revision = revision;
        }
        
        @Override
        public Resource retrieve(String token, Path uri, boolean forProcessing)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, Exception {
            if (this.uri.equals(uri)) {
                Revision rev = findRevision(token);
                if (rev != null) {
                    return retrieve(token, uri, forProcessing, rev);
                }
            }
            return super.retrieve(token, uri, forProcessing);
        }

        @Override
        public InputStream getInputStream(String token, Path uri,
                boolean forProcessing) throws ResourceNotFoundException,
                AuthorizationException, AuthenticationException, Exception {
            
            if (this.uri.equals(uri)) {
                Revision rev = findRevision(token);
                if (rev != null) {
                    return getInputStream(token, uri, forProcessing, rev);
                }
            }
            return super.getInputStream(token, uri, forProcessing);
        }
        
        @Override
        public Resource store(String token, Resource resource)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, ResourceLockedException,
                IllegalOperationException, ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource store(String token, Resource resource,
                StoreContext storeContext) throws ResourceNotFoundException,
                AuthorizationException, AuthenticationException,
                ResourceLockedException, IllegalOperationException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource storeContent(String token, Path uri, InputStream stream)
                throws AuthorizationException, AuthenticationException,
                ResourceNotFoundException, ResourceLockedException,
                IllegalOperationException, ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource storeContent(String token, Path uri,
                InputStream stream, Revision revision)
                throws AuthorizationException, AuthenticationException,
                ResourceNotFoundException, ResourceLockedException,
                IllegalOperationException, ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource createDocument(String token, Path uri,
                InputStream inputStream) throws IllegalOperationException,
                AuthorizationException, AuthenticationException,
                ResourceLockedException, ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource createCollection(String token, Path uri)
                throws AuthorizationException, AuthenticationException,
                IllegalOperationException, ResourceLockedException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void copy(String token, Path srcUri, Path destUri,
                boolean overwrite, boolean preserveACL)
                throws IllegalOperationException, AuthorizationException,
                AuthenticationException, FailedDependencyException,
                ResourceOverwriteException, ResourceLockedException,
                ResourceNotFoundException, ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void move(String token, Path srcUri, Path destUri,
                boolean overwrite) throws IllegalOperationException,
                AuthorizationException, AuthenticationException,
                FailedDependencyException, ResourceOverwriteException,
                ResourceLockedException, ResourceNotFoundException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void delete(String token, Path uri, boolean restoreable)
                throws IllegalOperationException, AuthorizationException,
                AuthenticationException, ResourceNotFoundException,
                ResourceLockedException, FailedDependencyException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void recover(String token, Path parentUri,
                RecoverableResource recoverableResource)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void deleteRecoverable(String token, Path parentUri,
                RecoverableResource recoverableResource) throws Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource lock(String token, Path uri, String ownerInfo,
                Depth depth, int requestedTimoutSeconds, String lockToken)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, FailedDependencyException,
                ResourceLockedException, IllegalOperationException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void unlock(String token, Path uri, String lockToken)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, ResourceLockedException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource storeACL(String token, Path uri, Acl acl)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, IllegalOperationException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource storeACL(String token, Path uri, Acl acl,
                boolean validateAcl) throws ResourceNotFoundException,
                AuthorizationException, AuthenticationException,
                IllegalOperationException, ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Resource deleteACL(String token, Path uri)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, IllegalOperationException,
                ReadOnlyException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Revision createRevision(String token, Path uri, Type type)
                throws AuthorizationException, ResourceNotFoundException,
                AuthenticationException, IOException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void deleteRevision(String token, Path uri, Revision revision)
                throws ResourceNotFoundException, AuthorizationException,
                AuthenticationException, Exception {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Comment addComment(String token, Resource resource,
                String title, String text) throws RepositoryException,
                AuthenticationException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Comment addComment(String token, Comment comment)
                throws AuthenticationException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void deleteComment(String token, Resource resource,
                Comment comment) throws RepositoryException,
                AuthenticationException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void deleteAllComments(String token, Resource resource)
                throws RepositoryException, AuthenticationException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Comment updateComment(String token, Resource resource,
                Comment comment) throws RepositoryException,
                AuthenticationException {
            throw new UnsupportedOperationException("Not supported");
        }

        private Revision findRevision(String token) throws Exception {
            if (cache.containsKey(token)) return cache.get(token);

            for (Revision r: getRevisions(token, uri)) {
                if (r.getName().equals(this.revision)) {
                    cache.put(token, r);
                    return r;
                }
            }
            cache.put(token,  null);
            return null;
        }
    }

}
