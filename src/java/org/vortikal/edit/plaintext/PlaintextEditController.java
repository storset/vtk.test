/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.edit.plaintext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Lock;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.web.servlet.mvc.SimpleFormController;




/**
 * Controller that handles editing of plaintext resource content.
 *
 * @version $Id$
 */
public class PlaintextEditController extends SimpleFormController {

    private static final int MAX_XML_DECLARATION_SIZE = 500;

    private static Log logger = LogFactory.getLog(
        PlaintextEditController.class);
    
    private Repository repository = null;
    private int lockTimeoutSeconds = 300;
    
    
    /**
     * Sets the repository.
     *
     * @param repository a <code>Repository</code> value
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    /**
     * Sets the requested number of seconds for lock timeout.
     *
     * @param lockTimeoutSeconds an <code>int</code> value
     */
    public void setLockTimeoutMinutes(int lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }
    

    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        
        String type = Lock.LOCKTYPE_EXCLUSIVE_WRITE;
        repository.lock(token, uri, type, principal.getQualifiedName(), "0",
                        this.lockTimeoutSeconds);

        Resource resource = repository.retrieve(token, uri, false);

        String url = service.constructLink(resource, principal);
        String content = getResourceContent(resource, token);
        
        PlaintextEditCommand command =
            new PlaintextEditCommand(content, url);

        if (resource.getContentType().startsWith("text/html"))
            command.setHtml(true);
        return command;
    }



    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        PlaintextEditCommand plaintextEditCommand =
            (PlaintextEditCommand) command;

        Resource resource = repository.retrieve(token, uri, false);

        String characterEncoding = resource.getCharacterEncoding();
        if (characterEncoding == null) {
            if ("text/xml".equals(resource.getContentType())) {
                characterEncoding = getXMLCharacterEncoding(resource, token);
            } else if ("text/plain".equals(resource.getContentType())) {
                characterEncoding = "iso-8859-1";
            }
        }
        if (logger.isDebugEnabled())
            logger.debug("Character encoding for document "
                         + resource + " resolved to: " + characterEncoding);

        repository.storeContent(token, uri, 
                new ByteArrayInputStream(
                    plaintextEditCommand.getContent().getBytes(
                        characterEncoding)));
    }
    


    private String getResourceContent(Resource resource, String token)
        throws IOException {

        // FIXME: if character encoding is set on the resource, just
        // read it as a plain stream, regardless of whether it is an
        // XML resource. Otherwise, let the XML parser handle the job,
        // BOTH on "model building" and on form submits.
        if (resource.getContentType().startsWith("text/xml")
            || resource.getContentType().startsWith("application/xml")) {
            return getXMLContent(resource, token);
        }
        return getPlainTextContent(resource, token);
    }
    

    private String getXMLContent(Resource resource, String token)
        throws IOException {
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(
                repository.getInputStream(token, resource.getURI(), false));
            //Format format = Format.getPrettyFormat();
            Format format = Format.getRawFormat();
            XMLOutputter xmlOutputter = new XMLOutputter(format);
            String xml = xmlOutputter.outputString(doc);
            return xml;
        
        } catch (JDOMException e) {
            throw new IOException("Unable to build document from resource " 
                                  + resource + ": " + e.getMessage());
        }
    }
    


    private String getPlainTextContent(Resource resource, String token)
        throws IOException {

        InputStream is = repository.getInputStream(token, resource.getURI(),
                                                   false);
        byte[] bytes = StreamUtil.readInputStream(is);
        String encoding = resource.getCharacterEncoding();
        if (encoding == null) encoding = "iso-8859-1";
        String content = new String(bytes, encoding);
        return content;
    }




    private String getXMLCharacterEncoding(Resource resource, String token)
        throws IOException {
        // FIXME: more accurate regexp:
        Pattern charsetPattern = Pattern.compile(
            "^\\s*<\\?xml.*\\s+encoding=[\"']"
            + "([A-Za-z0-9._\\-]+)[\"'][^>]*\\?>.*$");

        int len = MAX_XML_DECLARATION_SIZE;
        BufferedReader reader = null;
        InputStream inStream = null;
        String characterEncoding = "utf-8";
      

        try {
            if (logger.isDebugEnabled())
                logger.debug("Opening document " + resource.getURI());
            inStream = repository.getInputStream(
                token, resource.getURI(), false);
            reader = new BufferedReader(new InputStreamReader(
                                            inStream, "utf-8"));
            
            char[] chars = new char[len];
            int read = reader.read(chars, 0, len);

            String string = new String(chars, 0, read);
            java.util.regex.Matcher m = charsetPattern.matcher(string);

            if (m.matches()) {
                if (logger.isDebugEnabled())
                    logger.debug("Regexp match in XML declaration for pattern "
                                 + charsetPattern.pattern());
                characterEncoding = m.group(1);
            }

            if (logger.isDebugEnabled())
                logger.debug("No regexp match in XML declaration for pattern "
                             + charsetPattern.pattern());

            try {
                Charset.forName(characterEncoding);
            } catch (Exception e) {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "Invalid character encoding '" + characterEncoding
                        + "' for XML document " + resource + ", using utf-8");
                characterEncoding = "utf-8";
            }
        } catch (IOException e) {
            
        }

        return characterEncoding;
    }
}

