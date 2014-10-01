/* Copyright (c) 2011, University of Oslo, Norway
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
package vtk.repository.store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import vtk.repository.content.AbstractInputStreamWrapper;
import vtk.util.codec.MD5;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public final class Revisions {
    private static final int LINE = 80;
    //private static final int MAX_DIFF_SIZE = 1000000;

    public static String checksum(byte[] buffer) {
        return MD5.md5sum(buffer);
    }
    
    public static ChecksumWrapper wrap(InputStream input) {
        return new ChecksumWrapper(input);
    }
    
    public static class ChecksumWrapper extends AbstractInputStreamWrapper {
        private MD5.MD5InputStream md5Wrapper;
        private ChecksumWrapper(InputStream input) {
            super(MD5.wrap(input));
            this.md5Wrapper = (MD5.MD5InputStream)super.getWrappedStream();
        }
        
        public String checksum() {
            String result = this.md5Wrapper.md5sum();
            return result;
        }
    }
    
    public static Integer changeAmount(InputStream stream1, InputStream stream2) {
        try {
            List<String> lines1 = getLines(stream1);
            List<String> lines2 = getLines(stream2);
            if (lines1.isEmpty()) {
                return lines2.size();
            }
            if (lines2.isEmpty()) {
                return lines1.size();
            }
            Patch diff = DiffUtils.diff(lines1, lines2);
            List<Delta> deltas = diff.getDeltas();
            int linesChanged = 0;
            for (Delta d: deltas) {
                // XXX: Review:
                linesChanged += Math.max(
                        d.getOriginal().size(), d.getRevised().size());
            }
            return linesChanged;
        } catch (Throwable t) {
            return null;
        }
    }
    
    private static List<String> getLines(InputStream stream) throws IOException {
        String line = "";
        List<String> result = new LinkedList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        while ((line = in.readLine()) != null) {
            if (line.length() < LINE) {
                result.add(line);
                continue;
            }
            while (line.length() >= LINE) {
                result.add(line.substring(0, LINE) + "\n");                
                line = line.substring(LINE);
            }
            if (line.length() > 0) {
                result.add(line);
            }
        }
        return result;
    }
    
}