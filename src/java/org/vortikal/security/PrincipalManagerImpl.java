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
package org.vortikal.security;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.ACLPrincipal;
import java.util.Map;


/**
 * A simple principal manager implementation.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>defaultDomain</code> - a {@link String} specifying the
 *   domain to append to unqualified principal names. If this
 *   property is not specified, the behavior will be as if no domains
 *   exist at all.
 *   <li><code>domainURLMap</code> - a map of (<code>domain,
 *   URL-pattern</code>) entries. The domain corresponds to a
 *   principal {@link Principal#getDomain domain}, and the URL pattern
 *   is a string in which the sequence <code>%u</code> is substituted
 *   with the principal's (short) {@link Principal#getName name},
 *   thereby forming a unique URL for each principal. This URL can be
 *   acessed using the {@link Principal#getURL} method.
 * </ul>
 */
public class PrincipalManagerImpl implements PrincipalManager, InitializingBean {

    private String delimiter = "@";
    private String defaultDomain;
    private Map domainURLMap;
    private PrincipalStore principalStore;
    
    public void setDefaultDomain(String defaultDomain) {
        if (defaultDomain != null) {
            if ("".equals(defaultDomain.trim())) {
                throw new InvalidPrincipalException(
                    "Invalid domain: " + defaultDomain);
            }
            if (defaultDomain.indexOf(this.delimiter) != -1) {
                throw new InvalidPrincipalException(
                    "Invalid domain: " + defaultDomain + ": "
                    + "must not contain delimiter: '" + this.delimiter + "'");
            }
            this.defaultDomain = defaultDomain;
        }
    }
    

    public void setDomainURLMap(Map domainURLMap) {
        this.domainURLMap = domainURLMap;
    }
    

    public Principal getPrincipal(String id) {
        if (id == null) {
            throw new InvalidPrincipalException("Tried to get null principal");
        }

        id = id.trim();
        
        if (id.equals(""))
            throw new InvalidPrincipalException("Tried to get \"\" (empty string) principal");
        
        // FIXME: merge org.vortikal.repository.ACLPrincipal and
        // org.vortikal.security.Principal to avoid hacks like this:
        if (id.startsWith("dav:")) {
            return getSystemPrincipal(id);
        }

        if (id.startsWith(this.delimiter)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not start with delimiter: '" + this.delimiter + "'");
        }
        if (id.endsWith(this.delimiter)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not end with delimiter: '" + this.delimiter + "'");
        }

        if (id.indexOf(this.delimiter) != id.lastIndexOf(this.delimiter)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not contain more that one delimiter: '"
                + this.delimiter + "'");
        }


        /* Initialize name, domain and qualifiedName to default values
         * matching a setup "without" domains: */
        String name = id;
        String domain = null;
        String qualifiedName = id;
        
        if (id.indexOf(this.delimiter) > 0) {

            /* id is a fully qualified principal with a domain part: */
            domain = id.substring(id.indexOf(this.delimiter) + 1);

            if (this.defaultDomain != null && this.defaultDomain.equals(domain)) {
                /* In cases where domain equals default domain, strip
                 * the domain part off the name: */
                name = id.substring(0, id.indexOf(this.delimiter));
            } 
                        
        } else if (this.defaultDomain != null) {

            /* id is not a fully qualified principal, but since we
             * have a default domain, we append it: */
            domain = this.defaultDomain;
            qualifiedName = name + this.delimiter + domain;
        }

        String url = null;
        if (domain != null && this.domainURLMap != null) {
            String pattern = (String) this.domainURLMap.get(domain);
            if (pattern != null) {
                url = pattern.replaceAll("%u", name);
            }
        }


        return new PrincipalImpl(name, qualifiedName, domain, url);
    }

    private Principal getSystemPrincipal(String id) {
        if (ACLPrincipal.NAME_DAV_ALL.equals(id)
            || ACLPrincipal.NAME_DAV_AUTHENTICATED.equals(id)
            || ACLPrincipal.NAME_DAV_UNAUTHENTICATED.equals(id)
            || ACLPrincipal.NAME_DAV_SELF.equals(id)
            || ACLPrincipal.NAME_DAV_SELF.equals(id)
            || ACLPrincipal.NAME_DAV_OWNER.equals(id))  {

            return new PrincipalImpl(id, id, null, null);
        }
        throw new InvalidPrincipalException("Invalid principal: " + id);
    }


    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {

        if (principalStore == null)
            throw new BeanInitializationException("Property 'principalStore' must be set");
    }
    

    /**
     * @param principalStore The principalStore to set.
     */
    public void setPrincipalStore(PrincipalStore principalStore) {
        this.principalStore = principalStore;
    }


    /**
     * @see org.vortikal.security.PrincipalStore#validatePrincipal(org.vortikal.security.Principal)
     */
    public boolean validatePrincipal(Principal principal) throws AuthenticationProcessingException {
        return principalStore.validatePrincipal(principal);
    }


    /**
     * @see org.vortikal.security.PrincipalStore#validateGroup(java.lang.String)
     */
    public boolean validateGroup(String groupName) throws AuthenticationProcessingException {
        return principalStore.validateGroup(groupName);
    }


    /**
     * @see org.vortikal.security.PrincipalStore#resolveGroup(java.lang.String)
     */
    public String[] resolveGroup(String groupName) throws AuthenticationProcessingException {
        return principalStore.resolveGroup(groupName);
    }


    /**
     * @see org.vortikal.security.PrincipalStore#isMember(org.vortikal.security.Principal, java.lang.String)
     */
    public boolean isMember(Principal principal, String groupName) {
        return principalStore.isMember(principal, groupName);
    }
}
