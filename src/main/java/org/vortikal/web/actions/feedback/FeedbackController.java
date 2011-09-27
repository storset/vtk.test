/* Copyright (c) 2008 University of Oslo, Norway
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

package org.vortikal.web.actions.feedback;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.util.mail.MailExecutor;
import org.vortikal.util.mail.MailHelper;
import org.vortikal.util.mail.MailTemplateProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class FeedbackController implements Controller {

    private String viewName;
    private String siteName;
    private ResourceWrapperManager resourceManager;
    private JavaMailSenderImpl javaMailSenderImpl;
    private MailExecutor mailExecutor;
    private MailTemplateProvider mailTemplateProvider;
    private LocaleResolver localeResolver;
    private Service viewService;

    private String emailTo;
    private String emailFrom;


    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();

        Resource resource = repository.retrieve(token, uri, true);
        if (resource == null) {
            return null;
        }

        String language = resource.getContentLanguage();
        if (language == null) {
            Locale locale = localeResolver.resolveLocale(request);
            language = locale.toString();
        }
        Map<String, Object> m = new HashMap<String, Object>();
        String method = request.getMethod();
        if (method.equals("POST")) {

            String yourComment = request.getParameter("yourComment");

            // Checks for userinput
            if (StringUtils.isBlank(yourComment)) {
                m.put("tipResponse", "FAILURE-NULL-FORM");
            } else {
                try {
                    String[] emailMultipleTo = emailTo.split(",");
                    if (MailHelper.isValidEmail(emailMultipleTo) && MailHelper.isValidEmail(emailFrom)) {

                        org.springframework.web.servlet.support.RequestContext springRequestContext = new org.springframework.web.servlet.support.RequestContext(
                                request);
                        
                        String url = this.viewService.constructURL(uri).toString();
                        
                        if(request.getParameter("query") != null) {
                           url += "?vrtx=search&query=" + request.getParameter("query");  
                        }

                        MimeMessage mimeMessage = MailHelper.createMimeMessage(javaMailSenderImpl,
                                mailTemplateProvider, this.siteName, url.toString(), resource.getTitle(), emailMultipleTo, emailFrom, yourComment,
                                springRequestContext.getMessage("feedback.mail.subject-header-prefix") + ": "
                                        + resource.getTitle());

                        mailExecutor.SendMail(javaMailSenderImpl, mimeMessage);

                        m.put("emailSentTo", emailTo);
                        m.put("tipResponse", "OK");

                    } else {

                        if (yourComment != null && (!yourComment.equals(""))) {
                            m.put("yourSavedComment", yourComment);
                        }

                        m.put("tipResponse", "FAILURE-INVALID-EMAIL");
                    }
                    // Unreachable because of thread
                } catch (Exception mtex) {
                    m.put("tipResponse", "FAILURE");
                    m.put("tipResponseMsg", mtex.getMessage());
                }
            }
        }

        m.put("resource", this.resourceManager.createResourceWrapper());
        if(request.getParameter("query") != null) {
           m.put("query", request.getParameter("query")); 
        }
        return new ModelAndView(this.viewName, m);
    }


    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    @Required
    public void setResourceManager(ResourceWrapperManager resourceManager) {
        this.resourceManager = resourceManager;
    }


    @Required
    public void setJavaMailSenderImpl(JavaMailSenderImpl javaMailSenderImpl) {
        this.javaMailSenderImpl = javaMailSenderImpl;
    }


    @Required
    public void setMailExecutor(MailExecutor mailExecutor) {
        this.mailExecutor = mailExecutor;
    }


    @Required
    public void setMailTemplateProvider(MailTemplateProvider mailTemplateProvider) {
        this.mailTemplateProvider = mailTemplateProvider;
    }


    @Required
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }


    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }


    @Required
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }


    @Required
    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }


    @Required
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

}
