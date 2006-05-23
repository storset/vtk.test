/* Copyright (c) 2005, University of Oslo, Norway
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

package org.vortikal.web.referencedata.provider;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.query.parser.XmlSearcher;
import org.vortikal.web.referencedata.ReferenceDataProvider;


/**
 * Provide an instance of a {@link org.vortikal.repositoryimpl.index.dms.DMSXslQuery}
 * object in the "xsltParameters" map in the model. Optionally,
 * provide a URL to the "raw" query service as well.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>dmsXmlQueryHelper</code> - the {@link DMSXslQueryH} to
 *   provide to the XSLT transformation
 *   <li><code>key</code> - the key to use when providing the query
 *   helper in the model. The default is
 *   <code>{http://www.uio.no/vortex/xsl-parameters}DMSXslQuery</code>.
 * </ul>
 *
 * @author oyviste
 */
public class XsltSearchProvider implements ReferenceDataProvider, InitializingBean {
    
    private static String PARAMETER_NAMESPACE = "{http://www.uio.no/vortex/xsl-parameters}";
    private static String PARAMETER_NAME      = "XmlSearcher";
    
    Log logger = LogFactory.getLog(this.getClass());
    
    private XmlSearcher xmlSearcher;
    
    private String key = PARAMETER_NAMESPACE + PARAMETER_NAME;
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public void setXmlSearcher(XmlSearcher xmlSearcher) {
        this.xmlSearcher = xmlSearcher;
    }
    

    public void afterPropertiesSet() {
        if (this.xmlSearcher == null) {
            throw new BeanInitializationException("Property 'xmlSearcher' not set.");
        }
    }
    
    public void referenceData(Map model, HttpServletRequest request) {
        // Put Folder Evaluation XSL query object into map given by key 
        // "xsltParameters" in the model map.
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters == null) {
            parameters = new HashMap();
            model.put("xsltParameters", parameters);
        }
        parameters.put(this.key, this.xmlSearcher);
    }


}
