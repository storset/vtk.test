/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.resourcemanagement;

import java.io.InputStream;

import net.sf.json.JSONObject;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.JSONUtil;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.service.AbstractRepositoryAssertion;
import org.vortikal.web.service.Assertion;

public class ValidDocumentAssertion extends AbstractRepositoryAssertion {

	private Repository repository;
	private StructuredResourceManager resourceManager;
	
	@Override
	public boolean matches(Resource resource, Principal principal) {
		try {
			SecurityContext securityContext = SecurityContext.getSecurityContext();
			String token = securityContext.getToken();

			InputStream inputStream = this.repository.getInputStream(
					token, resource.getURI(), true);
			byte[] buffer = StreamUtil.readInputStream(inputStream);
			String content = new String(buffer, "utf-8");
			JSONObject object = JSONObject.fromObject(content);
                
			Object o = JSONUtil.select(object, "resourcetype");
			if (o == null) {
				return false;
			}
			StructuredResourceDescription description = resourceManager.get(o.toString());
			if (description == null) {
				return false;
			}
			StructuredResource r = new StructuredResource(description);
			r.parse(content);
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

	public boolean conflicts(Assertion assertion) {
		return false;
	}

	public void setResourceManager(StructuredResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}
