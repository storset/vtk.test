/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repository.systemjob;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class SystemChangeContext {

    private String jobName;
    private String time;
    private List<PropertyTypeDefinition> affectedProperties;
    private PropertyTypeDefinition systemJobStatusPropDef;

    public SystemChangeContext(String jobName, String time, List<PropertyTypeDefinition> affectedProperties,
            PropertyTypeDefinition systemJobStatusPropDef) {
        this.jobName = jobName;
        this.time = time;
        this.affectedProperties = affectedProperties;
        this.systemJobStatusPropDef = systemJobStatusPropDef;
    }

    public String getJobName() {
        return jobName;
    }

    public String getTime() {
        return time;
    }

    public List<PropertyTypeDefinition> getAffectedProperties() {
        return affectedProperties;
    }
    
    public PropertyTypeDefinition getSystemJobStatusPropDef() {
        return this.systemJobStatusPropDef;
    }

    public static String dateAsTimeString(Date date) {
        return new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(date);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.jobName);
        sb.append(", running at ").append(this.time);
        sb.append(", affecting ").append(this.affectedProperties);
        return sb.toString();
    }

}
