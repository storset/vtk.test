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
package org.vortikal.repository;


/**
 * This class encapsulates meta information about a privilege.
 */
public class Privilege implements java.io.Serializable, Cloneable {
    
    private static final long serialVersionUID = 3978429109086926897L;

    private String name = null;
    private String namespace = Namespace.STANDARD_NAMESPACE;

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
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value of namespace
     *
     * @return the value of namespace
     */
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * Sets the value of namespace
     *
     * @param namespace Value to assign to this.namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int hashCode() {
        // FIXME: implement properly:
        return 239 + name.hashCode() + namespace.hashCode();
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof Privilege)) {
            return false;
        }

        Privilege other = (Privilege) o;

        if (((this.name == null) && (other.name != null)) ||
                ((this.name != null) && (other.name == null)) ||
                !this.name.equals(other.name)) {
            return false;
        }

        if (((this.namespace == null) && (other.namespace != null)) ||
                ((this.namespace != null) && (other.namespace == null)) ||
                !this.namespace.equals(other.namespace)) {
            return false;
        }

        return true;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[namespace: ").append(namespace);
        sb.append(", name: ").append(name).append("]");

        return sb.toString();
    }
}
