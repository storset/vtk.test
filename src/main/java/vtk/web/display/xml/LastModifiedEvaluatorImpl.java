/* Copyright (c) 2005, University of Oslo, Norway
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
package vtk.web.display.xml;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vtk.repository.Property;
import vtk.repository.Resource;
import vtk.repository.resourcetype.PropertyTypeDefinition;

public class LastModifiedEvaluatorImpl implements LastModifiedEvaluator {
    
    private static Log logger = LogFactory.getLog(LastModifiedEvaluatorImpl.class);
    private List<String> lookupList;

    private boolean handleLastModifiedForValuesInList;

    private PropertyTypeDefinition propertyDefinition;

    /**
     * @param lookupList
     *            A list of the values we want (or do not want) to handle lastModified for. The
     *            values in the list are compared with the value for the selected property.
     */
    public void setLookupList(List<String> lookupList) {
        this.lookupList = lookupList;
    }

    /**
     * @param handleLastModifiedForValuesInList
     *            If true, report last-modified if value found in lookupList. If false, report
     *            last-modified if value not ofund in list
     */
    public void setHandleLastModifiedForValuesInList(boolean handleLastModifiedForValuesInList) {
        this.handleLastModifiedForValuesInList = handleLastModifiedForValuesInList;
    }

    /**
     * 
     * @param propertyDefinition
     *            The property type definition we want to look for.
     */
    public void setPropertyDefinition(PropertyTypeDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public boolean reportLastModified(Resource resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource can't be null");
        }
        if (lookupList == null || lookupList.isEmpty()) {
            return !handleLastModifiedForValuesInList;
        }

        Property prop = resource.getProperty(this.propertyDefinition);
        if (prop == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Can't find property '" + this.propertyDefinition
                             + "' for resource with uri " + resource.getURI());
            }
            // Since we havn't found the property, we know it is not in the list of accepted
            // values, and we should behave as it is not found in the list
            return !handleLastModifiedForValuesInList;
        }
        String schema = prop.getStringValue();
        boolean found = false;
        for (String schemaFromList: this.lookupList) {
            if (schemaFromList.equals(schema)) {
                found = true;
                break;
            }
        }
        return found == handleLastModifiedForValuesInList;
    }

}
