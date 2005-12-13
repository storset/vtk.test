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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;

import org.vortikal.util.codec.MD5;


/**
 * An implementation of MD5PasswordPrincipalManager that stores its
 * user and group information in-memory.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>principals</code> - a {@link Properties} object
 *   containing names and encoded passwords of the principals. This map
 *   is assumed to contain entries of type <code>(username,
 *   md5hash)</code>, and <code>md5hash</code> is in turn assumed to be
 *   a hashed value of the string <code>username:realm:password</code>.
 *   <li><code>groups</code> - This {@link Map} is assumed to contain
 *   entries of type <code>(group, memberlist)</code>, where
 *   <code>memberlist</code> is a <code>java.util.List</code> of
 *   strings which represent the names of those principals that are
 *   members of the group.
 *   <li><code>realm</code> - the authentication realm
 *   <li><code>domain</code> - the domain of the principals in this
 *   store (may be <code>null</code>)
 *   <li><code>order</code> - the order returned in {@link Order#getOrder}
 * </ul>
 * 
 */
public class PropertyConfigurableMD5PrincipalStore
  implements MD5PasswordPrincipalStore, InitializingBean, Ordered {

    private Log logger = LogFactory.getLog(this.getClass());

    private Properties principals;
    private Map groups;
    private String realm;
    private String domain;

    private int order = Integer.MAX_VALUE;
    
    
    public void setPrincipals(Properties principals) {
        this.principals = principals;
    }


    public void setGroups(Map groups) {
        this.groups = groups;
    }
    

    public void setOrder(int order) {
        this.order = order;
    }


    public int getOrder() {
        return this.order;
    }
    

    /**
     * @deprecated
     */
    public String getRealm() {
        return realm;
    }
    
    
    public void setRealm(String realm) {
        this.realm = realm;
    }


    public void setDomain(String domain) {
        this.domain = domain;
    }

    
    public void afterPropertiesSet() throws Exception {
    }


    public boolean validatePrincipal(Principal principal) {

        if (principal == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Validate principal: " + principal + ": false");
            }
            return false;
        }
        
        if (this.domain != null && !this.domain.equals(principal.getDomain())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Validate principal: " + principal + ": false");
            }
            return false;
        }

        boolean hit = this.principals.getProperty(principal.getQualifiedName()) != null;
        if (logger.isDebugEnabled()) {
            logger.debug("Validate principal: " + principal + ": " + hit);
        }
        return hit;
    }
    


    public boolean validateGroup(String groupName) {
        boolean hit = this.groups.containsKey(groupName);
        if (logger.isDebugEnabled()) {
            logger.debug("Validate group: " + groupName + ": " + hit);
        }
        return hit;
    }
    

    /**
     * @deprecated
     */
    public String getMD5HashString(Principal principal) {
        return this.principals.getProperty(principal.getQualifiedName());
    }
    

    public String[] resolveGroup(String groupName) {
        if (!this.groups.containsKey(groupName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resolve group: " + groupName + ": unknown group");
            }

            return new String[0];
        }

        List members = (List) this.groups.get(groupName);
        if (logger.isDebugEnabled()) {
            logger.debug("Resolve group: " + groupName + ": " + members);
        }

        return (String[]) members.toArray(new String[members.size()]);
    }


    public boolean isMember(Principal principal, String groupName) {
        if (!this.groups.containsKey(groupName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Check membership for principal " + principal
                             + ", group: " + groupName + ": unknown group");
            }
            return false;
        }

        List members = (List) this.groups.get(groupName);
        boolean hit = members.contains(principal.getQualifiedName());
        if (logger.isDebugEnabled()) {
            logger.debug("Check membership for principal " + principal
                         + ", group: " + groupName + ": " + hit);
        }
        return hit;
    }



    public void authenticate(Principal principal, String password)
        throws AuthenticationException {
        
        String hash = getMD5HashString(principal);
        String clientHash = 
            MD5.md5sum(principal.getQualifiedName() + ":" + realm + ":" + password); 

        if (hash == null || !hash.equals(clientHash)) {
            throw new AuthenticationException(
                "Authentication failed for principal " + principal.getQualifiedName()
                + ", " + "wrong credentials.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully authenticated principal: " + principal);
        }

    }

}

