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
package org.vortikal.web.controller.repository;




public class CreateDocumentCommand {

    private String submitURL = null;
    private String cancel;
    private boolean done = false;
    private String name = null;
    private String sourceURI = null;
    
    
    public CreateDocumentCommand(String submitURL) {
        this.submitURL = submitURL;
    }

    

    /**
     * Gets the value of cancel
     *
     * @return the value of cancel
     */
    public String getCancel() {
        return this.cancel;
    }

    /**
     * Sets the value of cancel
     *
     * @param cancel Value to assign to this.cancel
     */
    public void setCancel(String cancel)  {
        this.cancel = cancel;
    }


    /**
     * Gets the value of submitURL
     *
     * @return the value of submitURL
     */
    public String getSubmitURL() {
        return this.submitURL;
    }

    /**
     * Sets the value of submitURL
     *
     * @param submitURL Value to assign to this.submitURL
     */
    public void setSubmitURL(String submitURL)  {
        this.submitURL = submitURL;
    }


    /**
     * @return Returns the done.
     */
    public boolean isDone() {
        return done;
    }
    
    /**
     * @param done The done to set.
     */
    public void setDone(boolean done) {
        this.done = done;
    }


    /**
     * Gets the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the value of name
     *
     * @param name Value to assign to this.name
     */
    public void setName(String name)  {
        this.name = name;
    }
    
    /**
     * Gets the value of sourceURI
     *
     * @return the value of sourceURI
     */
    public String getSourceURI() {
        return this.sourceURI;
    }

    /**
     * Sets the value of sourceURI
     *
     * @param sourceURI Value to assign to this.sourceURI
     */
    public void setSourceURI(String sourceURI)  {
        this.sourceURI = sourceURI;
    }
}

