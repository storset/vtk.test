package org.vortikal.web.actions.copymove;

import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;


public class CopyBackupController implements Controller {
    
    private CopyHelper copyHelper;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Path resourceToCopySrcUri = null;
        try {
            String uri = URLDecoder.decode((String) request.getParameter("uri"), "UTF-8");
            resourceToCopySrcUri = Path.fromStringWithTrailingSlash(uri);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        if (resourceToCopySrcUri == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        // Retrieve resource
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, resourceToCopySrcUri, false);
        
        if(resource.getURI().getParent() != requestContext.getResourceURI()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        
        // Copy resource
        Path resourceCopyDestUri = copyHelper.makeDestUri(resourceToCopySrcUri, repository, token, resource);
        repository.copy(token, resourceToCopySrcUri, resourceCopyDestUri, false, true);

        // Return 201 with destination uri in the Location-header
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setHeader("Location", resourceCopyDestUri.toString());
        
        return null;
    }

    @Required
    public void setCopyHelper(CopyHelper copyHelper) {
        this.copyHelper = copyHelper;
    }

}
