/* Copyright (c) 2006, University of Oslo, Norway
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

package org.vortikal.web.controller.repository.tidy;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

/**
 * Test case for <code>org.vortikal.web.controller.repository.tidy.JTidyTransformer</code> 
 */
public class JTidyTransformerTest extends TestCase {

    private JTidyTransformer jti; 
    
    
    protected void setUp() throws Exception {
        super.setUp();
        this.jti = new JTidyTransformer();
    }

    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
    public void testParseValidHtmlToXhtml() {
        try {
            InputStream is = 
                this.getClass().getResourceAsStream("valid.html");
            if (is == null) {
                fail("InputStream containing valid HTML was not found");
            }
            
            assertTrue( parserTest(is) );
            
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getMessage());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
    
    
    public void testParseInvalidHtmlToXhtml() {
        try {
            InputStream is = 
                this.getClass().getResourceAsStream("invalid.html");
            if (is == null) {
                fail("InputStream containing invalid HTML was not found");
            }
                        
            assertTrue( parserTest(is) );
            
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getMessage());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
    
    
    public void testParseFrontpageHtmlToXhtml() {
        try {
            InputStream is = 
                this.getClass().getResourceAsStream("frontpage.html");
            if (is == null) {
                fail("InputStream containing invalid HTML was not found");
            }
                        
            assertTrue( parserTest(is) );
            
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getMessage());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
    
    
    // This test will fail if doctype is not set to "transitional"/"loose"
    public void testStrict() {
        try {
            InputStream is = 
                this.getClass().getResourceAsStream("strict.html");
            if (is == null) {
                fail("InputStream containing invalid HTML was not found");
            }
            
            assertTrue( parserTest(is) );
            
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getMessage());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
    public void testLoose() {
        try {
            InputStream is = 
                this.getClass().getResourceAsStream("loose.html");
            if (is == null) {
                fail("InputStream containing invalid HTML was not found");
            }
                        
            assertTrue( parserTest(is) );
            
        } catch (FileNotFoundException fnfe) {
            fail(fnfe.getMessage());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
    
    
    /*
     * Helper methods
     */
    private boolean parserTest(InputStream isOriginal) 
        throws FileNotFoundException, IOException {

        InputStream isParsed = this.jti.transform(isOriginal); 
        // throws FileNotFoundException for invalid input streams!
        
        if (isParsed == null) {
            fail("JTidy parsing failed");
        }
        
        // Write resulting contents to OutputStream in order to genereate 
        // testable string of the JTidy-parsed content
        byte[] buffer = new byte[5000];
        
        int n;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((n = isParsed.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        }
        
        isParsed.close();
        
        // Parse resulting OutputStream to String for testing of contents
        String isParsedAsString = String.valueOf(baos);
        baos.close();
                     
//        System.out.println(isParsedAsString); // debug helper
          
        if (isParsedAsString.indexOf("XHTML 1.0 Transitional") != -1 &&
                isParsedAsString.indexOf("http://www.w3.org/TR/xhtml1/DTD/xhtml1" +
                            "-transitional.dtd") != -1 &&
                isParsedAsString.indexOf("<head>") != -1 &&
                isParsedAsString.indexOf("<title>") != -1 &&
                isParsedAsString.indexOf("<body") != -1) {
            return true;
        }
        else {
            return false;
        }
    }

}
