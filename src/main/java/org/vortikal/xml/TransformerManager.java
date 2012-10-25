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
package org.vortikal.xml;

import java.io.IOException;
import java.util.Date;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Resource;



/**
 * XSLT transformer manager. This class simplifies the creation of
 * transformers for use in XSLT processing. 
 *
 * TODO: URIResolver supplied by manager instead of by stylesheet resolver?
 * Clarify URI resolving behavior.
 */
public class TransformerManager implements InitializingBean {

    private static Log logger = LogFactory.getLog(TransformerManager.class);

    private boolean alwaysCompile = false;
    private StylesheetTemplatesRegistry stylesheetRegistry = new StylesheetTemplatesRegistry();
    private StylesheetReferenceResolver[] stylesheetReferenceResolvers;
    private StylesheetURIResolver[] compilationURIResolvers = null;
    
    private ChainedURIResolver compilationURIResolver = null;
    private ChainedURIResolver transformationURIResolver = null;

    private TransformationThrottle transformationThrottle;


    public void setAlwaysCompile(boolean alwaysCompile) {
        this.alwaysCompile = alwaysCompile;
    }
    


    public void setStylesheetRegistry(StylesheetTemplatesRegistry stylesheetRegistry)  {
        this.stylesheetRegistry = stylesheetRegistry;
    }


    public StylesheetTemplatesRegistry  getStylesheetRegistry()  {
        return this.stylesheetRegistry;
    }


    public void setStylesheetReferenceResolvers(
        StylesheetReferenceResolver[] stylesheetReferenceResolvers)  {
        this.stylesheetReferenceResolvers = stylesheetReferenceResolvers;
    }


    /**
     * Sets the URI resolvers used for stylesheet compilation.
     * Specifically, this involves the XSLT <code>import</code> and
     * <code>include</code> constructs.
     *
     * @param compilationURIResolvers an <code>URIResolver[]</code> value
     */
    public void setCompilationURIResolvers(StylesheetURIResolver[] compilationURIResolvers) {
        this.compilationURIResolvers = compilationURIResolvers;
        this.compilationURIResolver = new ChainedURIResolver(compilationURIResolvers);
    }
    

    /**
     * Sets the URI resolvers that are used during transformation.
     * The transformation URI resolvers are responsible for resolving
     * URIs generated by the XSLT <code>document()</code> function.
     *
     * @param transformationURIResolvers an <code>URIResolver[]</code> value
     */
    public void setTransformationURIResolvers(URIResolver[] transformationURIResolvers) {
        this.transformationURIResolver = new ChainedURIResolver(transformationURIResolvers);
    }
    

    public void setTransformationThrottle(TransformationThrottle transformationThrottle) {
        this.transformationThrottle = transformationThrottle;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.stylesheetReferenceResolvers == null) {
            throw new BeanInitializationException(
                "JavaBean property 'stylesheetReferenceResolvers' must be set");
        }
        if (this.compilationURIResolver == null) {
            throw new BeanInitializationException(
                "JavaBean property 'compilationURIResolvers' must be set");
        }
        if (this.transformationURIResolver == null) {
            throw new BeanInitializationException(
                "JavaBean property 'transformationURIResolvers' must be set");
        }

        logger.info("Using compilation style sheet URI resolver: "
                    + this.compilationURIResolver);

        logger.info("Using transformation style sheet URI resolver: "
                    + this.transformationURIResolver);
    }
    
    
    /**
     * Get a transformer for the given stylesheetIdentifier.
     * 
     * @param stylesheetIdentifier an abstract stylesheet identifier,
     * e.g. a URL or some other reference.
     * @return a transformer for the (compiled) stylesheet.
     * @throws IOException if an I/O error occurs
     * @throws TransformerConfigurationException if the transformer
     * could not be instantiated.
     * @throws StylesheetCompilationException if an error occurred
     * while compiling the stylesheet.
     */
    public Transformer getTransformer(String stylesheetIdentifier) throws 
        Exception {
        
        StylesheetURIResolver resolver = getStylesheetResolver(stylesheetIdentifier);
        
        if (resolver == null) {
            throw new StylesheetCompilationException(
                "Unable to compile XSLT stylesheet '" + stylesheetIdentifier +
                "': No matching stylesheet resolvers");
        }


        Date lastCompile = this.stylesheetRegistry.getLastModified(stylesheetIdentifier);
        Date lastModified = resolver.getLastModified(stylesheetIdentifier);
        if (logger.isDebugEnabled()) {
            logger.debug("Stylesheet '" + stylesheetIdentifier
                         + "' was last compiled: " + lastCompile
                         + ", last modified " + lastModified);
        }

        Templates templates = null;

        if (this.alwaysCompile || lastCompile == null || lastModified == null ||
            (lastModified.getTime() > lastCompile.getTime())) {

            if (logger.isDebugEnabled()) {
                logger.debug("Compiling stylesheet '" + stylesheetIdentifier);
            }
            templates = this.stylesheetRegistry.compile(
                stylesheetIdentifier, this.compilationURIResolver, lastModified);
        }
        
        templates = this.stylesheetRegistry.getTemplates(stylesheetIdentifier);

        if (templates == null) {
            throw new StylesheetCompilationException(
                "Unable to compile XSLT stylesheet '" +
                stylesheetIdentifier + "'");
        }

        Transformer transformer = templates.newTransformer();
        transformer.setURIResolver(this.transformationURIResolver);
        if (this.transformationThrottle != null) {
            transformer = new ThrottledTransformer(transformer, this.transformationThrottle);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Returning [transformer: " + transformer + " URI resolver: "
                + this.transformationURIResolver
                + "] for style sheet identifier " + stylesheetIdentifier + "");
        }

        return transformer;
    }
    
    /**
     * Gets a transformer for a given XML resource. 
     *
     * @param resource the XML resource
     * @param document a JDOM Document representation of the XML resource
     * @return a transformer representing a compiled stylesheet with
     * URI resolvers set.
     * @exception IOException if an error occurs
     * @exception TransformerConfigurationException if an error occurs
     * @exception StylesheetCompilationException if an error occurs
     */
    public Transformer getTransformer(Resource resource, Document document)
        throws Exception {

        String stylesheetIdentifier = resolveTemplateReference(resource, document);

        if (stylesheetIdentifier == null) {
            throw new StylesheetCompilationException(
                "Unable to find XSLT stylesheet identifier for resource " +
                resource);
        }
        return getTransformer(stylesheetIdentifier);
    }



    private String resolveTemplateReference(Resource resource, Document document) {
        
        for (int i = 0; i < this.stylesheetReferenceResolvers.length; i++) {
            StylesheetReferenceResolver resolver = this.stylesheetReferenceResolvers[i];
            // Obtain the stylesheet identifier:
            String reference = resolver.getStylesheetIdentifier(resource, document);
            
            if (reference != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found stylesheet identifier for resource '" +
                                 resource + "': '" + reference + "'");
                }
                return reference;
            }
        }
        return null;
    }
    


    private StylesheetURIResolver getStylesheetResolver(String stylesheetIdentifier) {
        
        for (int i = 0; i < this.compilationURIResolvers.length; i++) {
            StylesheetURIResolver resolver = this.compilationURIResolvers[i];
            // Map the identifier to a physical resource:
            if (resolver.matches(stylesheetIdentifier)) {
                if (logger.isDebugEnabled())
                    logger.debug("Using stylesheet resolver " + resolver +
                                 " for stylesheet identifier: '" + stylesheetIdentifier + "'");
                return resolver;
            }
        }
        return null;
    }
    


    private static class ChainedURIResolver implements URIResolver {
        private URIResolver[] chain = null;

        public ChainedURIResolver(URIResolver[] chain) {
            this.chain = chain;
        }

        public Source resolve(String href, String base) throws TransformerException {
            String uri = "[href = " + href + ", base = " + base + "]";

            for (int i = 0; i < this.chain.length; i++) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Attempting to resolve URI " + uri
                                 + " using resolver " + this.chain[i]);
                }

                Source s = this.chain[i].resolve(href, base);
                if (s != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resolved URI " + uri + " to source " + s
                                     + " using resolver " + this.chain[i]);
                    }
                    return s;
                }                
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to resolve URI " + uri + " using resolvers "
                             + java.util.Arrays.asList(this.chain));
            }
            // FIXME: return empty Source
            return null;
        }

        public String toString() {
            return "Chain: " + java.util.Arrays.asList(this.chain);
        }
        
    }

    private static class ThrottledTransformer extends Transformer {
        private Transformer transformer;
        private TransformationThrottle throttle;

        public ThrottledTransformer(Transformer transformer, TransformationThrottle throttle) {
            super();
            this.transformer = transformer;
            this.throttle = throttle;
        }
        
        public void transform(javax.xml.transform.Source xmlSource,
                                       javax.xml.transform.Result outputTarget)
            throws TransformerException {
            try {
                this.throttle.start();
                this.transformer.transform(xmlSource, outputTarget);
            } finally {
                this.throttle.end();
            }
        }
        
        public void setParameter(String name, Object value) {
            this.transformer.setParameter(name, value);
        }
        
        public Object getParameter(String name) {
            return this.transformer.getParameter(name);
        }

        public void clearParameters() {
            this.transformer.clearParameters();
        }

        public void setURIResolver(URIResolver resolver) {
            this.transformer.setURIResolver(resolver);
        }

        public URIResolver getURIResolver() {
            return this.transformer.getURIResolver();
        }

        public void setOutputProperties(java.util.Properties properties) {
            this.transformer.setOutputProperties(properties);
        }
        
        public java.util.Properties getOutputProperties() {
            return this.transformer.getOutputProperties();
        }

        public void setOutputProperty(String name, String value) {
            this.transformer.setOutputProperty(name, value);
        }

        public String getOutputProperty(String name) {
            return this.transformer.getOutputProperty(name);
        }

        public void setErrorListener(javax.xml.transform.ErrorListener listener) {
            this.transformer.setErrorListener(listener);
        }

        public javax.xml.transform.ErrorListener getErrorListener() {
            return this.transformer.getErrorListener();
        }
    }
    
}
