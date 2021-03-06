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
package vtk.edit.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.jdom.ProcessingInstruction;


/**
 * Controller that marks elements for moving, based on request input.
 *
 */
public class MoveController implements ActionHandler {

    @SuppressWarnings("unchecked")
    public Map<String, Object> handle(HttpServletRequest request,
            EditDocument document,
            SchemaDocumentDefinition documentDefinition) throws XMLEditException {

        Map<String, Object> model = new HashMap<String, Object>();
        String mode = document.getDocumentMode();

        if (mode.equals("default")) {
            Enumeration<String> enumeration = request.getParameterNames();

            List<String> paths = new ArrayList<String>();
            while (enumeration.hasMoreElements()) {
                String s = enumeration.nextElement();
                if (s.matches("\\d+(\\.\\d+)*")) {
                    paths.add(s);
                }
            }
            Collections.sort(paths, new PathComparator());

            List<Element> elements = new ArrayList<Element>();
            for (String param: paths) {
                Element e = document.findElementByPath(param);
                e.addContent(new ProcessingInstruction("marked", "true"));
                elements.add(e);
            }
            if (elements.size() > 0) {
                document.setDocumentMode("move");
                document.setElements(elements);
            } else {
                Util.setXsltParameter(model,"ERRORMESSAGE", "MISSING_ELEMENTS_TO_MOVE");
            }
            return model;
        }
        return null;
    }

    private static class PathComparator implements Comparator<String> {

        public PathComparator() {
        }

        public int compare(String s1, String s2) {
            StringTokenizer st1 = new StringTokenizer(s1, ".");
            StringTokenizer st2 = new StringTokenizer(s2, ".");

            while (st1.hasMoreElements() && st2.hasMoreElements()) {
                s1 = st1.nextToken();
                s2 = st2.nextToken();
                try {
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);

                    if (i1 != i2) {
                        return new Integer(i1).compareTo(new Integer(i2));
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Unable to compare strings "
                        + s1 + ", " + s2 + ": paths must must consist "
                        + "of numbers, separated by dots");
                }
            }

            return new Integer(st1.countTokens()).compareTo(new Integer(st2
                    .countTokens()));
        }
    }
}
