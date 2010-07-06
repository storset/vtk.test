/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.resourcemanagement.view.tl.ComponentInvokerNodeFactory;
import org.vortikal.resourcemanagement.view.tl.JSONAttributeHandler;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.tl.CaptureNodeFactory;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.IfNodeFactory;
import org.vortikal.text.tl.ListNodeFactory;
import org.vortikal.text.tl.StripNodeFactory;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.ValNodeFactory;
import org.vortikal.text.tl.expr.Function;
import org.vortikal.util.repository.PropertyAspectDescription;
import org.vortikal.util.repository.PropertyAspectResolver;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class DynamicDecoratorTemplateFactory implements TemplateFactory, InitializingBean {

    private Repository repository;
    private PropertyTypeDefinition aspectsPropdef;
    PropertyAspectDescription fieldConfig;
    
    private Map<String, DirectiveNodeFactory> directiveHandlers;
    private Set<Function> functions = new HashSet<Function>();
    private ComponentResolver componentResolver;

    public Template newTemplate(TemplateSource templateSource) throws InvalidTemplateException {
        return new DynamicDecoratorTemplate(templateSource, this.componentResolver, this.directiveHandlers);
    }

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setAspectsPropdef(PropertyTypeDefinition aspectsPropdef) {
        this.aspectsPropdef = aspectsPropdef;
    }
    
    public void setFieldConfig(PropertyAspectDescription fieldConfig) {
        this.fieldConfig = fieldConfig;
    }

    @Required public void setComponentResolver(ComponentResolver componentResolver) {
        this.componentResolver = componentResolver;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, DirectiveNodeFactory> directiveHandlers = new HashMap<String, DirectiveNodeFactory>();
        directiveHandlers.put("if", new IfNodeFactory());
        directiveHandlers.put("strip", new StripNodeFactory());

        ValNodeFactory val = new ValNodeFactory();
        //val.addValueFormatHandler(PropertyImpl.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        //val.addValueFormatHandler(Value.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        //val.addValueFormatHandler(Value[].class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        directiveHandlers.put("val", val);

        ListNodeFactory list = new ListNodeFactory();
        list.setFunctions(this.functions);
        directiveHandlers.put("list", list);
        
        //directiveHandlers.put("resource-props", new ResourcePropsNodeFactory(this.repository));

        DefineNodeFactory def = new DefineNodeFactory();

        Set<Function> functions = new HashSet<Function>();
        functions.addAll(this.functions);
        //functions.add(new RequestURLFunction(new Symbol("request-url")));
        functions.add(new RepositoryIDFunction(new Symbol("repo-id")));
        functions.add(new RequestParameterFunction(new Symbol("request-param")));
        functions.add(new ResourceLocaleFunction(new Symbol("resource-locale")));
        functions.add(new TemplateParameterFunction(new Symbol("template-param")));
        functions.add(new ResourceAspectFunction(new Symbol("resource-aspect"), this.aspectsPropdef, this.fieldConfig));
        functions.add(new ResourcePropHandler(new Symbol("resource-prop"), this.repository));
        functions.add(new JSONAttributeHandler(new Symbol("json-attr")));
        def.setFunctions(functions);
        directiveHandlers.put("def", def);
        
        directiveHandlers.put("capture", new CaptureNodeFactory());

        //directiveHandlers.put("localized", new LocalizationNodeFactory(this.resourceModelKey));
        directiveHandlers.put("call", new ComponentInvokerNodeFactory(new DynamicDecoratorTemplate.ComponentSupport()));

        this.directiveHandlers = directiveHandlers;
    }
    
    public void setFunctions(Set<Function> functions) {
        if (functions == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        this.functions = functions;
    }

    private class RepositoryIDFunction extends Function {
        public RepositoryIDFunction(Symbol symbol) {
            super(symbol, 0);
        }
        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            return repository.getId();
        }        
    }
    
    private class RequestURLFunction extends Function {
        public RequestURLFunction(Symbol symbol) {
            super(symbol, 0);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            URL url = URL.create(request);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("url", url);
            map.put("scheme", url.getProtocol());
            map.put("host", url.getHost());
            map.put("port", url.getPort());
            map.put("path", url.getPath());
            map.put("query", url.getQueryString());
            return map;
        }
    }
    
    private class TemplateParameterFunction extends Function {
        public TemplateParameterFunction(Symbol symbol) {
            super(symbol, 1);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            Object o = args[0];
            if (o == null) {
                throw new IllegalArgumentException("Argument cannot be NULL");
            }
            String name = o.toString();
            return DynamicDecoratorTemplate.getTemplateParam(request, name);
        }
    }
    
    private class RequestParameterFunction extends Function {
        
        public RequestParameterFunction(Symbol symbol) {
            super(symbol, 1);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            Object o = args[0];
            if (o == null) {
                throw new IllegalArgumentException("Argument cannot be NULL");
            }
            String name = o.toString();
            return request.getParameter(name);
        }
        
    }
    
    private class ResourceLocaleFunction extends Function {
        
        public ResourceLocaleFunction(Symbol symbol) {
            super(symbol, 0);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            return RequestContextUtils.getLocale(request).toString();
        }
        
    }
    
    private class ResourceAspectFunction extends Function {
        private PropertyAspectResolver resolver = null;

        public ResourceAspectFunction(Symbol symbol, PropertyTypeDefinition aspectsPropdef, PropertyAspectDescription fieldConfig) {
            super(symbol, 1);
            this.resolver = new PropertyAspectResolver(repository, aspectsPropdef, fieldConfig);
        }
        
        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            Object o = args[0];
            if (o == null) {
                throw new IllegalArgumentException("Argument must be a valid name");
            }
            String aspect = o.toString();
            return this.resolver.resolve(requestContext.getResourceURI(), aspect);
        }
     }
    
    
    private class ResourcePropHandler extends Function {

        private Repository repository;

        public ResourcePropHandler(Symbol symbol, Repository repository) {
            super(symbol, 2);
            this.repository = repository;
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            final Object arg1 = args[0];
            final Object arg2 = args[1];
            PropertySet resource = null;
            String ref = null;

            if (arg1 instanceof PropertySet) {
                resource = (PropertySet) arg1;
            } else {
                ref = arg1.toString();
            }

            if (resource == null) {
                Path uri = ".".equals(ref) 
                ? RequestContext.getRequestContext().getResourceURI()
                        : Path.fromString(ref);
                String token = SecurityContext.getSecurityContext().getToken();
                resource = this.repository.retrieve(token, uri, true);
            }
            String propName = arg2.toString();
            if ("uri".equals(propName)) {
                return resource.getURI();
            }

            Property property = resource.getProperty(Namespace.DEFAULT_NAMESPACE, propName);
            if (property == null) {
                for (Property prop : resource.getProperties()) {
                    if (propName.equals(prop.getDefinition().getName())) {
                        property = prop;
                    }
                }
            }
            if (property == null) {
                return null;
            }
            if (property.getDefinition().isMultiple()) {
                return getObjectValue(property.getValues());
            } else {
                return getObjectValue(property.getValue());
            }
        }

    }
    
    private Object getObjectValue(Object obj) {        
        if (obj instanceof Value) 
            return ((Value)obj).getObjectValue();
        
        if (obj instanceof Value[]) {
            Value[] values = (Value[])obj;
            Object[] objValues = new Object[values.length];
            for (int i=0; i<values.length; i++) {
                objValues[i] = values[i].getObjectValue();
            }
            return objValues;
        }
        return obj;
    }

}
