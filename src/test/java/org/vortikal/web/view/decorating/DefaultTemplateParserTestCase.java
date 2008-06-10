/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;


public class DefaultTemplateParserTestCase extends MockObjectTestCase {

    private static final String EMPTY_TEMPLATE = "";

    public void testEmpty() throws Exception {
        DefaultTemplateParser parser = createParser();
        Reader reader = new StringReader(EMPTY_TEMPLATE);
        ComponentInvocation[] parsedTemplate = parser.parseTemplate(reader);
        assertEquals(1, parsedTemplate.length);
    }
    

    private static final String SIMPLE_TEMPLATE_WITH_PARAMS =
        "<html>${namespace:name var1=[20] var2=[30]}</html>";

    public void testSimple() throws Exception {
        DefaultTemplateParser parser = createParser();
        Reader reader = new StringReader(SIMPLE_TEMPLATE_WITH_PARAMS);
        ComponentInvocation[] parsedTemplate = parser.parseTemplate(reader);
        assertEquals(3, parsedTemplate.length);
        
        String begin = renderComponent(parsedTemplate[0].getComponent());
        String end = renderComponent(parsedTemplate[2].getComponent());
        assertEquals("<html>", begin);
        assertEquals("</html>", end);

        ComponentInvocation c = parsedTemplate[1];
        assertEquals("20", c.getParameters().get("var1"));
        assertEquals("30", c.getParameters().get("var2"));

    }


    private static final String MALFORMED_TEMPLATE =
        "<html>${namespace:name var1=[20}] var2=[30]]}</html>";

    public void testMalformed() throws Exception {
        DefaultTemplateParser parser = createParser();
        Reader reader = new StringReader(MALFORMED_TEMPLATE);
        ComponentInvocation[] parsedTemplate = parser.parseTemplate(reader);
        assertEquals(1, parsedTemplate.length);
        String result = renderComponent(parsedTemplate[0].getComponent());
        assertEquals(MALFORMED_TEMPLATE, result);
    }

    private static final String NESTED_DIRECTIVES =
        "${${component:ref}}";

    public void testMalformedNestedDirectives() throws Exception {
        DefaultTemplateParser parser = createParser();
        Reader reader = new StringReader(NESTED_DIRECTIVES);
        ComponentInvocation[] parsedTemplate = parser.parseTemplate(reader);
        assertEquals(3, parsedTemplate.length);
        assertEquals(DummyComponent.class, parsedTemplate[1].getComponent().getClass());
        String begin = renderComponent(parsedTemplate[0].getComponent());
        String end = renderComponent(parsedTemplate[2].getComponent());
        assertEquals("${", begin);
        assertEquals("}", end);
    }

    private static final String COMPLEX_TEMPLATE =
        "${<html>${namespace:name\nvar1 = [20] \r\nvar2=\r\n[30\\]] \rvar3=[400]}</html>}";

    public void testComplexTemplate() throws Exception {
        DefaultTemplateParser parser = createParser();
        Reader reader = new StringReader(COMPLEX_TEMPLATE);
        ComponentInvocation[] parsedTemplate = parser.parseTemplate(reader);
        assertEquals(3, parsedTemplate.length);
        ComponentInvocation c = parsedTemplate[1];
        assertEquals("20", c.getParameters().get("var1"));
        assertEquals("30]", c.getParameters().get("var2"));
        assertEquals("400", c.getParameters().get("var3"));
    }


    private DefaultTemplateParser createParser() {
        DefaultTemplateParser parser = new DefaultTemplateParser();
        parser.setComponentResolver(new DummyComponentResolver());
        return parser;
    }
    
    private String renderComponent(DecoratorComponent c) throws Exception {
        Writer writer = new StringWriter();
        Mock mockRequest = mock(DecoratorRequest.class);
        Mock mockResponse = mock(DecoratorResponse.class);

        mockResponse.expects(atLeastOnce()).method("getWriter").will(returnValue(writer));
        DecoratorRequest request = (DecoratorRequest) mockRequest.proxy();
        DecoratorResponse response = (DecoratorResponse) mockResponse.proxy();
        c.render(request, response);
        return writer.toString();
    }
    

    private class DummyComponentResolver implements ComponentResolver {
        public DecoratorComponent resolveComponent(String namespace, String name) {
            return new DummyComponent(namespace, name);
        }
        public List<DecoratorComponent> listComponents() {
            return new ArrayList<DecoratorComponent>();
        }
    }

    private class DummyComponent implements DecoratorComponent {
        private String namespace, name;
        public DummyComponent(String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
        }
        
        public String getNamespace() {
            return this.namespace;
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getDescription() {
            return "Dummy component " + this.namespace + ":" + this.name;
        }
        
        public Map<String, String> getParameterDescriptions() {
            return null;
        }
        
        public void render(DecoratorRequest request, DecoratorResponse response)
            throws Exception {
            response.getWriter().write("component: " + this.namespace + ":" + this.name);
        }
    }

}

