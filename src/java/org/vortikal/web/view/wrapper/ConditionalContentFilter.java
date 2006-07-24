/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.view.wrapper;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;


/**
 * A text content filter that takes two filters, a conditional filter
 * and a target filter, and runs the target filter only if the
 * conditional filter returns without altering the content.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>conditionalContentFilter</code> - the content filter
 *   that is always invoked. The resulting content is then examined
 *   and the target content filter is invoked if the result has not
 *   been modified.
 *   <li><code>targetContentFilter</code> - the content filter to
 *   invoke if the first filter did not modified the result.
 *   <li><code>checkContentLengthOnly</code> - whether to only compare
 *   the length of the content before and after the first filter is
 *   run - if the content length has not changed, it is interpreted as
 *   if the content itself had not changed, otherwise a full
 *   <code>equals()</code> is performed. Default is
 *   <code>false</code>.
 * </ul>
 */
public class ConditionalContentFilter implements TextContentFilter {

    private TextContentFilter conditionalContentFilter;
    private TextContentFilter targetContentFilter;
    private boolean checkContentLengthOnly = false;
    
        
    public void setConditionalContentFilter(TextContentFilter conditionalContentFilter) {
        this.conditionalContentFilter = conditionalContentFilter;
    }

    public void setTargetContentFilter(TextContentFilter targetContentFilter) {
        this.targetContentFilter = targetContentFilter;
    }
    
    public void setCheckContentLengthOnly(boolean checkContentLengthOnly) {
        this.checkContentLengthOnly = checkContentLengthOnly;
    }
    
    public String process(Map model, HttpServletRequest request,
                          String content) throws Exception {
        String processedContent = this.conditionalContentFilter.process(
            model, request, content);
        boolean runContentFilter = false;
        if (this.checkContentLengthOnly) {
            runContentFilter = processedContent.length() == content.length();
        } else if (processedContent.length() == content.length()) {
            runContentFilter = content.equals(processedContent);
        } else {
            runContentFilter = false;
        }

        if (runContentFilter) {
            return this.targetContentFilter.process(model, request, processedContent);
        }
        return processedContent;
    }
}
