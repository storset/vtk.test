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
package org.vortikal.util.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;


/**
 * Various URL utility methods.
 *
 */
public class URLUtil {

    /**
     * Get the request URI stripped of the context path.
     *
     * @param requestURI
     * @param contextPath  
     * @return the stripped URI
     */
    public static String getRequestURI(String requestURI, String contextPath) {
        String strippedURI = null;
        try {
            strippedURI = java.net.URLDecoder.decode(requestURI, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                "Java doesn't seem to support utf-8. Not much to do about that.");
        }
        strippedURI = strippedURI.substring(contextPath.length(),
                                          strippedURI.length());
        
        if (strippedURI == null || strippedURI.trim().equals("")) {
            strippedURI = "/";
        }
        
        if (!strippedURI.equals("/") && strippedURI.endsWith("/")) {
            strippedURI = strippedURI.substring(0, strippedURI.length() - 1);
        }
        
        return strippedURI;
    }



    /**
     * Splits a URI into path elements. For example, the URI
     * <code>/foo/bar</code> would be split into the following
     * components: <code>{"/", "foo", "bar"}</code>
     *
     * @param uri the uri to split
     * @return an array consisting of the components
     */
    public static String[] splitUri(String uri) {
         ArrayList list = new ArrayList();
         StringTokenizer st = new StringTokenizer(uri, "/");
 
         while (st.hasMoreTokens()) {
             String name = st.nextToken();
             list.add(name);
         }
 
         list.add(0, "/");
         return (String[]) list.toArray(new String[0]);
     }




    /**
     * Splits a URI into incremental path elements. For example, the
     * URI <code>/foo/bar/baaz</code> produce the result: 
     * <code>{"/", "/foo", "/foo/bar/", "/foo/bar/baaz"}</code>
     *
     * @param uri the uri to split
     * @return an array consisting of the components
     */
    public static String[] splitUriIncrementally(String uri) {

        if (uri == null || uri.equals("") || uri.equals("/")) {
            return new String[] { "/" };
        }
        
        ArrayList elements = new ArrayList();
        ArrayList incremental = new ArrayList();
        
        StringTokenizer tokenizer = new StringTokenizer(uri, "/");

        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            elements.add(s);
            String path = "";
            for (Iterator i = elements.iterator(); i.hasNext();) {
                path += "/" + (String) i.next();
            }
            incremental.add(path);
        }
        
        incremental.add(0, "/");

        return (String[]) incremental.toArray(new String[]{});
    }





    /**
     * This method is probably not needed
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlDecode(String uri) {

        try {

            return urlDecode(uri, "utf-8");
            
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                "Java doesn't seem to support utf-8. Not much to do about that.");
        }
    }
    


    /**
     * This method is probably not needed
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlDecode(String uri, String encoding)
        throws  UnsupportedEncodingException {

        if (uri.equals("/")) {

            return "/";
        }

        if (!uri.startsWith("/")) {

            return URLDecoder.decode(uri, encoding);
        }

        String[] path = splitUri(uri);
        String result = "";
        
        for (int i = 1; i < path.length; i++) {

            result += "/";

            result += URLDecoder.decode(path[i], encoding);
        }

        return result;
    }


    

    /**
     * URL encodes a URI using encoding UTF-8, preserving path
     * separators ('/').
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlEncode(String uri) {
        
        try {

            return urlEncode(uri, "utf-8");
            
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                "Java doesn't seem to support utf-8. Not much to do about that.");
        }
    }
    



    /**
     * URL encodes a URI using specified encoding, preserving path
     * separators ('/').
     *
     * @param uri a <code>String</code> value
     * @return a <code>String</code>
     */
    public static String urlEncode(String uri, String encoding)
        throws  UnsupportedEncodingException {

        if (uri.equals("/")) {

            return "/";
        }

        if (!uri.startsWith("/")) {

            return URLEncoder.encode(uri, encoding);
        }        

        String[] path = splitUri(uri);
        StringBuffer result = new StringBuffer("");
        
        for (int i = 1; i < path.length; i++) {

            result.append("/");

            result.append(URLEncoder.encode(path[i], encoding));
        }
        
        // The method 'splitUri' removes trailing slashes. Handles this.
        if (uri.endsWith("/") && !result.toString().endsWith("/")) {
            result.append("/");
        }
        
        return result.toString().replaceAll("\\+", "%20");
    }



    /**
     * Gets the host name from the HTTP request.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>String</code>
     */
    public static String getHostName(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();

        // FIXME: handle user names and passwords in URLs
        String hostName = url.substring(url.indexOf("://") + 3);
        hostName = hostName.substring(0, hostName.indexOf("/"));
        if (hostName.indexOf(":") > 0) {
            hostName = hostName.substring(0, hostName.indexOf(":"));
        }
        return hostName;
    }
    
    
    
    /**
     * Return parent of given url
     * @param String url
     * @return parent url if it exists, or NULL if invalid argument   
     */
    private String getParentURL( String url ) {
        // url is root or has no parent or child-url is empty (e.g. link on the form "parent-url/")
        if( url.lastIndexOf("/") == 0 
         || url.lastIndexOf("/") == -1 
         || "".equals( url.substring((url.lastIndexOf("/"))+1, url.length()) ) )
            return null;
        else
            return url.substring( 0, url.lastIndexOf("/") );
    }
}
