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
package org.vortikal.security.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.AbstractHtmlPageFilter;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlText;
import org.vortikal.text.html.HtmlUtil;
import org.vortikal.util.text.TextUtils;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;


public class CSRFPreventionHandler extends AbstractHtmlPageFilter 
implements HandlerInterceptor {

	private static Log logger = LogFactory.getLog(CSRFPreventionHandler.class);

	private String ALGORITHM = "HmacSHA1";

	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		if (!"POST".equals(request.getMethod())) {
			return true;
		}
		if (request.getContentType().startsWith("multipart/form-data")) {
			// XXX: skipping multipart requests for now
			return true;
		}
		SecurityContext securityContext = SecurityContext.getSecurityContext();
		if (securityContext.getPrincipal() == null) {
			throw new AuthenticationException("Illegal anonymous action");
		}
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new IllegalStateException("A session must be present");
		}
		SecretKey secret = (SecretKey) 
		session.getAttribute("csrf-prevention-secret");
		if (secret == null) {
			throw new AuthorizationException(
			"Missing CSRF prevention secret in session");
		}

		String suppliedToken = request.getParameter("csrf-prevention-token");

		if (suppliedToken == null) {
			throw new AuthorizationException(
					"Missing CSRF prevention token in request");
		}

		URL requestURL = URL.create(request);
		String computed =  generateToken(requestURL, secret, session.getId());

		if (logger.isDebugEnabled()) {
			logger.debug("Check token: url: " + requestURL 
					+ ", supplied token: " + suppliedToken 
					+ ", computed token: " + computed + ", secret: " + secret);
		}
		if (!computed.equals(suppliedToken)) {
			throw new AuthorizationException(
					"CSRF prevention token mismatch");
		}
		return true;
	}

	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
	}

	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
	throws Exception {
	}


	public NodeResult filter(HtmlContent node) {
		if (!(node instanceof HtmlElement)) {
			return NodeResult.keep;
		}
		HtmlElement element = ((HtmlElement) node);
		if (!"form".equals(element.getName().toLowerCase())) {
			return NodeResult.keep;
		}
		HtmlAttribute method = element.getAttribute("method");
		if (method == null || "".equals(method.getValue().trim()) 
				|| "get".equals(method.getValue().toLowerCase())) {
			return NodeResult.keep;
		}

		URL url;
		HtmlAttribute actionAttr = element.getAttribute("action");
		if (actionAttr == null || actionAttr.getValue() == null 
				|| "".equals(actionAttr.getValue().trim())) {
			HttpServletRequest request = 
				RequestContext.getRequestContext().getServletRequest();
			url = URL.create(request);
		} else {
			try {
				url = parseActionURL(actionAttr.getValue());
			} catch (Throwable t) {
				logger.warn("Unable to find URL in action attribute: " 
						+ actionAttr.getValue(), t);
				return NodeResult.keep;
			}
		}
		url.setRef(null);

		RequestContext requestContext = RequestContext.getRequestContext();
		HttpSession session = requestContext.getServletRequest().getSession(false);


		if (session != null) {
			SecretKey secret = (SecretKey) session.getAttribute("csrf-prevention-secret");
			if (secret == null) {
				secret = generateSecret();
				session.setAttribute("csrf-prevention-secret", secret);
			}

			String csrfPreventionToken = generateToken(url, secret, session.getId());
			if (logger.isDebugEnabled()) {
				logger.debug("Generate token: url: " + url + ", token: " 
						+ csrfPreventionToken + ", secret: " + secret);
			}
			HtmlElement input = createElement("input", true, true);
			List<HtmlAttribute> attrs = new ArrayList<HtmlAttribute>();
			attrs.add(createAttribute("name", "csrf-prevention-token"));
			attrs.add(createAttribute("type", "hidden"));
			attrs.add(createAttribute("value", csrfPreventionToken));
			input.setAttributes(attrs.toArray(new HtmlAttribute[attrs.size()]));
			element.addContent(0, input);
			element.addContent(0, new HtmlText() {
				public String getContent() {
					return "\r\n";
				}
			});
			element.addContent(new HtmlText() {
				public String getContent() {
					return "\r\n";
				}
			});
		}
		return NodeResult.keep;
	}

	private SecretKey generateSecret() {
		try {
			KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
			return kg.generateKey();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to generate secret key", e);
		}
	}

	private String generateToken(URL url, SecretKey sk, String sessionID) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(sk);
			byte[] buffer = (url.toString() + sessionID).getBytes("utf-8");
			byte[] hashed = mac.doFinal(buffer);
			return new String(TextUtils.toHex(hashed));
		} catch (Exception e) {
			throw new IllegalStateException("Unable to generate token", e);
		}
	}

	private URL parseActionURL(String action) {
		if (action.startsWith("http://") || action.startsWith("https://")) {
			URL url = URL.parse(HtmlUtil.unescapeHtmlString(action));
			return url;
		}

		HttpServletRequest request = 
			RequestContext.getRequestContext().getServletRequest();
		URL url = URL.create(request);
		url.clearParameters();
		Path path = null;
		String[] segments = action.split("/");
		int startIdx = 0;
		if (action.startsWith("/")) {
			path = Path.ROOT;
			startIdx = 1;
		} else {
			path = RequestContext.getRequestContext().getCurrentCollection();
		}

		String query = null;
		for (int i = startIdx; i < segments.length; i++) {
			String elem = segments[i];
			if (elem.contains("?")) {
				query = elem.substring(elem.indexOf("?"));
				elem = elem.substring(0, elem.indexOf("?"));
			}
			path = path.expand(elem);
		}

		url.setPath(path);
		if (query != null) {
			Map<String, String[]> queryMap = URL.splitQueryString(query);
			for (String key : queryMap.keySet()) {
				for (String value : queryMap.get(key)) {
					url.addParameter(key, value);
				}
			}
		}
		return url;
	}

}
