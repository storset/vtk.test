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

import junit.framework.TestCase;

import org.vortikal.web.view.decorating.HtmlElement;
import org.vortikal.web.view.decorating.HtmlPage;
import org.vortikal.web.view.decorating.HtmlPageParser;


public class HtmlPageParserImplTestCase extends TestCase {

    private static final String SIMPLE_XHTML_PAGE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
        + "<html>\n"
        + "  <head attr1=\"foo\" attr2=\"bar\">\n"
        + "    <object>foobar</object>\n"
        + "    <object>foobbar</object>"
        + "    <object>syt</object>"
        + "    <object><miz><under>foo</under></miz></object>"
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
    
    String VALID_HTML_401_TRANS = 
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
        + "<html>\n"
        + "  <head>\n"
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
        + "    <!-- My comment #1 -->\n"
        + "    <br>\n"
        + "    <form action=\"http://foo.bar/post.html\" method=\"POST\">\n"
        + "      <select name=val1>\n"
        + "      <select name=val2>\n"
        + "      <select name=val3 selected>\n"
        + "    </form>\n"
        + "    <hr>\n"
        + "    <table class=\"myListing\">\n"
        + "      <!-- My comment #2 -->\n"
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


    private HtmlPage parse(String content) throws Exception {
        HtmlPageParser parser = new HtmlPageParserImpl();
        long before = System.currentTimeMillis();
        HtmlPage page = parser.parse(new java.io.ByteArrayInputStream(
                                         content.getBytes("utf-8")), "utf-8");
        long duration = System.currentTimeMillis() - before;
        return page;
    }
    
}
