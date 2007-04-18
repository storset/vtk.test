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
package org.vortikal.web.view.decorating.htmlparser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.vortikal.web.view.decorating.html.HtmlContent;
import org.vortikal.web.view.decorating.html.HtmlElement;
import org.vortikal.web.view.decorating.html.HtmlNodeFilter;
import org.vortikal.web.view.decorating.html.HtmlPage;
import org.vortikal.web.view.decorating.html.HtmlPageParserException;


public class HtmlPageParserImplTestCase extends TestCase {

    private static Set<String> compositeTags = new HashSet<String>();
    private static Set<String> emptyTags = new HashSet<String>();
    
    static {
        compositeTags.add("pre");
        compositeTags.add("b");
        compositeTags.add("address");
        compositeTags.add("map");
        compositeTags.add("thead");
        compositeTags.add("tfoot");
        compositeTags.add("tbody");
        compositeTags.add("fieldset");
        compositeTags.add("optgroup");
        compositeTags.add("small");

        emptyTags.add("br");
        emptyTags.add("area");
        emptyTags.add("link");
        emptyTags.add("img");
        emptyTags.add("param");
        emptyTags.add("hr");
        emptyTags.add("input");
        emptyTags.add("col");
        emptyTags.add("base");
        emptyTags.add("meta");
    }

    private HtmlPageParserImpl parser;

    public void setUp() {
        this.parser =  new HtmlPageParserImpl();
        this.parser.setCompositeTags(compositeTags);
        this.parser.setEmptyTags(emptyTags);
    }
    


    private static final String SIMPLE_XHTML_PAGE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
        + "<html>\n"
        + "  <head attr1=\"foo\" attr2=\"bar\">\n"
        + "    <object>foobar</object>\n"
        + "    <object>foobbar</object>"
        + "    <object>syt</object>"
        + "    <object><element><under>foo</under></element></object>"
        + "    <meta name=\"keywords\" content=\"My keywords\"/>\n"
        + "    <title>My title</title>\n"
        + "  </head>\n"
        + "  <body>The body</body>\n"
        + "</html>\n";


    public void testSimpleHtmlPage() throws Exception {

        HtmlPage page = parse(SIMPLE_XHTML_PAGE);
        assertEquals("html", page.getRootElement().getName());
        assertEquals("head", page.getRootElement().getChildElements()[0].getName());
        assertEquals("My title", page.getRootElement().getChildElements()[0]
                     .getChildElements()[5].getContent());
        assertEquals("bar", page.getRootElement().getChildElements()[0]
                     .getAttributes()[1].getValue());
    }

    public void testNodeFiltering() throws Exception {
        HtmlPage page = parse(SIMPLE_XHTML_PAGE, 
            new HtmlNodeFilter() {
                public HtmlContent filterNode(HtmlContent node) {
                    if (node instanceof HtmlElement) {
                        HtmlElement element = (HtmlElement) node;
                        if (element.getName().equals("meta")) {
                            return null;
                        }
                    }
                    return node;
                }
            });
        
        HtmlElement head = page.getRootElement().getChildElements()[0];
        assertEquals(0, head.getChildElements("meta").length);
    }
    
    
    private static final String SIMPLE_XHTML_PAGE_WITH_DIRECTIVES =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
        + "<html>\n"
        + "  <head attr1=\"foo\" attr2=\"bar\">\n"
        + "    <object>foobar</object>\n"
        + "    <meta name=\"keywords\" content=\"My keywords\"/>\n"
        + "    <title>My title</title>\n"
        + "  </head>\n"
        + "  <body>"
        + "    <div>"
        + "      <span>Last modified"
        + "        <b>${resource:property id=[lastModified]}</b>"
        + "      </span>"
        + "      ${resource:property id=[lastModified]}"
        + "    </div>"
        + "  </body>\n"
        + "</html>\n";


    public void testNestedTagFiltering() throws Exception {

        final Map<String, HtmlElement> map = new HashMap<String, HtmlElement>();
        parse(SIMPLE_XHTML_PAGE_WITH_DIRECTIVES, 
             new HtmlNodeFilter() {
                public HtmlContent filterNode(HtmlContent node) {
                    if (node instanceof HtmlElement) {
                        HtmlElement element = (HtmlElement) node;
                        map.put(element.getName(), element);
                    }
                    return node;
                }
            });
        
        assertNotNull(map.get("html"));
        assertNotNull(map.get("head"));
        assertNotNull(map.get("object"));
        assertNotNull(map.get("meta"));
        assertNotNull(map.get("title"));
        assertNotNull(map.get("body"));
        assertNotNull(map.get("div"));
        assertNotNull(map.get("span"));
        assertNotNull(map.get("b"));
    }


    private static final String UNFORMATTED_STRING =
        "  body The div page div body";
    
    public void testUnformattedString() throws Exception {
        try {
            parse(UNFORMATTED_STRING);
        } catch (HtmlPageParserException e) {
            // Expected
        }
    }

    private static final String PARTIAL_HTML_PAGE =
        "  <body>The <div>page</div></body>";
    
    public void testPartialHtmlPage() throws Exception {
        HtmlPage page = parse(PARTIAL_HTML_PAGE);
        assertEquals("body", page.getRootElement().getName());
        assertEquals("div", page.getRootElement().getChildElements()[0].getName());
        assertEquals("page", page.getRootElement().getChildElements()[0].getContent());
        assertEquals("The <div>page</div>", page.getRootElement().getContent());
    }


    String VALID_HTML_401_TRANS = 
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
        + "<html>\n"
        + "  <head>\n"
        + "    <!-- My comment #1 -->\n"
        + "    <link REL=\"stylesheet\" HREF=\"/some/stylesheet.css\">\n"
        + "    <link REL=\"stylesheet\" HREF=\"/some/other/stylesheet.css\">\n"
        + "    <title>My title</title>\n"
        + "  </head>\n"
        + "<body>\n"
        + "  <div class=\"body\">\n"
        + "    <div id=\"titleContainer\">\n"
        + "      <div class=\"class1 class2\">\n"
        + "        <h1>Header 1</h1>\n"
        + "      </div>\n"
        + "    </div>\n"
        + "    <!-- My comment #2 -->\n"
        + "    <br>\n"
        + "    <form action=\"http://foo.bar/post.html\" method=\"POST\">\n"
        + "      <select name=val1>\n"
        + "      <select name=val2>\n"
        + "      <select name=val3 selected>\n"
        + "    </form>\n"
        + "    <hr>\n"
        + "    <table class=\"myListing\">\n"
        + "      <!-- My comment #3 -->\n"
        + "      <tr class=\"listingHeader\">\n"
        + "        <th class=\"sortColumn name\"><a href=\"http://foo.bar?sort=1\">Name</a></th>\n"
        + "        <th class=\"size\"><a href=\"http://foo.bar?sort=2\">Size</a></th>\n"
        + "        <th class=\"lastModified\"><a href=\"http://foo.bar?sort=3\">Last modified</a></th>\n"
        + "      </tr>\n"
        + "      <tr class=\"listingRow\">\n"
        + "        <td class=\"name\">Some name</td>\n"
        + "        <td class=\"size\">200</td>\n"
        + "        <td class=\"lastModified\">2007-01-30</td>\n"
        + "      </tr>\n"
        + "    </table>\n"
        + "  </div>\n"
        + "  <div class=\"contact\">\n"
        + "    <a href=\"http://foo.bar/contact\">Contact</a>\n"
        + "  </div>\n"
        + "  </div>\n"
        + "</body>\n"
        + "</html>\n";
    

    public void testValidHtml401Trans() throws Exception {
        HtmlPage page = parse(VALID_HTML_401_TRANS);
        assertEquals("html", page.getRootElement().getName());
        assertEquals("head", page.getRootElement().getChildElements()[0].getName());
        assertEquals(" My comment #1 ", page.getRootElement()
                     .getChildElements()[0]
                     .getChildNodes()[1].getContent());

    }
    

    String SIMPLE_HTML_401_FRAMESET = 
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">\n"
        + "<html>\n"
        + "  <head>\n"
        + "    <title>Simple frameset document</title>\n"
        + "  </head>\n"
        + "<frameset cols=\"20%, 80%\">\n"
        + "  <frameset rows=\"100, 200\">\n"
        + "    <frame src=\"frame1.html\">\n"
        + "    <frame src=\"frame2.html\">\n"
        + "  </frameset>\n"
        + "</frameset>\n"
        + "</html>\n";


    public void testValidHtml401Frameset() throws Exception {
        HtmlPage page = parse(SIMPLE_HTML_401_FRAMESET);
        assertEquals("frameset", page.getRootElement().getChildElements()[1].getName());
        assertEquals("rows", page.getRootElement().getChildElements()[1]
                     .getChildElements()[0].getAttributes()[0].getName()); 
        assertEquals("100, 200", page.getRootElement().getChildElements()[1]
                     .getChildElements()[0].getAttributes()[0].getValue());
    }

    private static final String MALFORMED_XHTML_PAGE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
        + "<html\n"
        + "  <head attr1=\"foo\" attr2=\"bar\"\n"
        + "    <object>foo</object\n"
        + "    <object>bar</object"
        + "    <title>My title/title>\n"
        + "  </head>\n"
        + "  <body>The body</body>\n"
        + "</html>\n";

    public void testMalformedXHtml() throws Exception {
        HtmlPage page = parse(MALFORMED_XHTML_PAGE);
        assertEquals("html", page.getRootElement().getName());
        assertEquals("head", page.getRootElement().getChildElements()[0].getName());
        assertEquals("My title/title>\n  ", page.getRootElement()
                     .getChildElements()[0]
                     .getChildElements()[2].getContent());
    }


    public void testTables()  throws Exception {
        HtmlPage page = parseFile("test-tables.html", "utf-8");
        assertEquals("html", page.getRootElement().getName());
        assertEquals("head", page.getRootElement().getChildElements()[0].getName());

        HtmlElement body = page.getRootElement().getChildElements()[1];
        HtmlElement table1 = body.getChildElements()[0];
        assertEquals("table", table1.getName());
        assertEquals(4, table1.getChildElements().length);
        HtmlElement tr1 = table1.getChildElements()[0];
        assertEquals("tr", tr1.getName());
        HtmlElement th1 = tr1.getChildElements()[0];
        assertEquals("th", th1.getName());
        assertEquals("Heading 1", th1.getContent());
        HtmlElement th2 = tr1.getChildElements()[1];
        assertEquals("th", th2.getName());
        assertEquals("Heading 2", th2.getContent());
        assertEquals("Cell 4", table1.getChildElements()[2].getChildElements()[1].getContent());

        HtmlElement table2 = body.getChildElements()[1];
        assertEquals("table", table2.getName());
        assertEquals(3, table2.getChildElements().length);
        HtmlElement thead1 = table2.getChildElements()[0];
        assertEquals("thead", thead1.getName());
        HtmlElement tfoot1 = table2.getChildElements()[1];
        assertEquals("tfoot", tfoot1.getName());

        HtmlElement tbody1 = table2.getChildElements()[2];
        assertEquals("tbody", tbody1.getName());
    }


    public void testHtml401Strict() throws Exception {
        HtmlPage page = parseFile("test-html401strict.html", "utf-8");
        assertEquals("html", page.getRootElement().getName());
        assertEquals("head", page.getRootElement().getChildElements()[0].getName());

        HtmlElement body = page.getRootElement().getChildElements()[1];
        assertEquals("body", body.getName());
        
        HtmlElement[] h1 = body.getChildElements("h1");
        assertEquals(1, h1.length);
        assertEquals("h1", h1[0].getName());
        
        HtmlElement p1 = body.getChildElements("p")[0];
        assertEquals("p", p1.getName());
        HtmlElement abbr = p1.getChildElements()[0];
        assertEquals("abbr", abbr.getName());
        assertEquals(1, abbr.getAttributes().length);
        assertEquals("html.", abbr.getContent());
        assertEquals("title", abbr.getAttributes()[0].getName());
        assertEquals("Hyper Text Markup Language", abbr.getAttributes()[0].getValue());

        HtmlElement acronym = p1.getChildElements()[1];
        assertEquals("Anything But Cheese", acronym.getAttributes()[0].getValue());
        assertEquals("ABC", acronym.getContent());

        HtmlElement small = p1.getChildElements()[2];
        assertEquals("small <br>text", small.getContent());
        // TODO: verify the rest of the document content
    }

    
    public void testHtmlFile1() throws Exception {
        HtmlPage page = parseFile("test-1.html", "utf-8");
        assertEquals("html", page.getRootElement().getName());
        assertEquals("head", page.getRootElement().getChildElements()[0].getName());

        assertEquals("Maecenas lobortis", page.getRootElement()
                     .getChildElements()[1]
                     .getChildElements()[1]
                     .getChildElements()[1]
                     .getChildElements("span")[0].getContent());

        assertEquals("Maecenas lobortis", page.getRootElement()
                     .getChildElements()[1]
                     .getChildElements()[1]
                     .getChildElements()[1]
                     .getChildElements("span")[0].getContent());

        assertEquals("sollicitudin", page.getRootElement()
                     .getChildElements("body")[0]
                     .getChildElements("div")[0]
                     .getChildElements("p")[2]
                     .getChildElements("span")[3].getContent());
    }


    public void testHtmlBr() throws Exception {
        HtmlPage page = parseFile("test-2.html", "utf-8");
        HtmlElement body = page.getRootElement().getChildElements()[1];
        HtmlElement br = body.getChildElements("p")[0]
                               .getChildElements("br")[0];
        assertNotNull(br);
        assertEquals(br.getEnclosedContent(), "<br>");
    }

    
    public void testHtmlHr() throws Exception {
        HtmlPage page = parseFile("test-2.html", "utf-8");
        HtmlElement body = page.getRootElement().getChildElements()[1];
        HtmlElement hr = body.getChildElements("hr")[0];
        assertNotNull(hr);
        assertEquals(hr.getEnclosedContent(), "<hr>");
    }

    private HtmlPage parse(String content) throws Exception {
        HtmlPage page = this.parser.parse(new java.io.ByteArrayInputStream(
                                         content.getBytes("utf-8")), "utf-8");
        return page;
    }


    private HtmlPage parse(String content, HtmlNodeFilter filter) throws Exception {
        HtmlPage page = this.parser.parse(
            new java.io.ByteArrayInputStream(content.getBytes("utf-8")), "utf-8",
            filter);        
        return page;
    }
    
    private HtmlPage parseFile(String fileName, String encoding) throws Exception {
        HtmlPage page = this.parser.parse(getClass().getResourceAsStream(fileName), encoding);
        return page;
    }
    

    private void print(HtmlElement element, String indent) {
        System.out.println(indent + "el: " + element.getName());
        HtmlElement[] children = element.getChildElements();
        for (int i = 0; i < children.length; i++) {
            print(children[i], indent + "  ");
        }
    }
}
