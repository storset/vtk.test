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
package org.vortikal.repository.store.fs.jca;

import java.io.File;
import java.io.IOException;

import org.vortikal.repository.Path;

public class CopyOperation extends AbstractFileOperation {
    private Path srcURI, destURI;
    private File src;
    private File tmp;
    private FileMapper mapper;
    
    public CopyOperation(Path srcURI, Path destURI, FileMapper mapper) throws IOException {

        this.srcURI = srcURI;
        this.src = mapper.getFile(srcURI);
        this.tmp = new File(mapper.newTempFileName());
        if (!this.tmp.createNewFile()) {
            throw new IOException("Unable to create temporary file " + this.tmp);
        }

        if (this.src.isDirectory()) {
            copyDir(this.src, this.tmp);
        } else {
            copyFile(this.src, this.tmp);
        }
        mapper.mapFile(srcURI, this.tmp);
        this.mapper = mapper;
    }

    public void persist() throws IOException {
        File orig = this.mapper.getFile(this.destURI);
        String origName = orig.getAbsolutePath();
        File deleted = new File(this.mapper.newTempFileName());
        if (orig.exists()) {
            move(orig, deleted);
        }
        orig = new File(origName);
        move(this.tmp, orig);
        delete(deleted);
    }

    public void forget() throws IOException {
        delete(this.tmp);
    }

    public String toString() {
        return getClass().getName() + "[copy: from " + this.srcURI
            + " to " + this.destURI + " via " + this.tmp + "]";
    }

}
