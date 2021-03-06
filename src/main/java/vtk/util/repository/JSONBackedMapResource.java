/* Copyright (c) 2010, University of Oslo, Norway
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
package vtk.util.repository;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import vtk.repository.Path;
import vtk.repository.Repository;
import vtk.util.ReverseMap;
import vtk.util.text.Json;

public class JSONBackedMapResource implements ReverseMap<Object, Object>, InitializingBean {

    private Repository repository;
    private Path uri;
    private String token;
    private Map<Object, Object> map;
    private Map<Object, Set<Object>> reverseMap;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            load();
        } catch (Throwable t) {
            // Ignore
        }
    }   

    public Map<?, ?> getMap() {
        return Collections.unmodifiableMap(this.map);
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setUri(String uri) {
        this.uri = Path.fromString(uri);
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    public void load() throws Exception {
        
        Map<Object, Object> newMap = null;
        Map<Object, Set<Object>> newReverseMap = null;
        try {
            InputStream inputStream = this.repository.getInputStream(this.token, this.uri, false);
            Object parsed = Json.parse(inputStream);
            if (!(parsed instanceof Map<?, ?>)) {
                return;
            }
            Map<?, ?> m = (Map<?, ?>) parsed;
            
            newMap = new HashMap<Object, Object>();
            newReverseMap = new HashMap<Object, Set<Object>>();

            for (Object k: m.keySet()) {
                Object v = m.get(k);
                newMap.put(k, v);
                if (!newReverseMap.containsKey(v)) {
                    newReverseMap.put(v, new HashSet<Object>());
                }
                Set<Object> keys = newReverseMap.get(v);
                keys.add(k);
            }
        } finally {
            this.map = newMap;
            this.reverseMap = newReverseMap;
        }
    }
    
    @Override
    public void clear() {
        throw new UnsupportedOperationException("Illegal operation");
    }

    @Override
    public boolean containsKey(Object key) {
        if (this.map == null) {
            return false;
        }
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (this.map == null) {
            return false;
        }
        return this.map.containsValue(value);
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        if (this.map == null) {
            return Collections.emptySet();
        }
        return this.map.entrySet();
    }

    @Override
    public Object get(Object key) {
        if (this.map == null) {
            return null;
        }
        return this.map.get(key);
    }

    @Override
    public boolean isEmpty() {
        if (this.map == null) {
            return true;
        }
        return this.map.isEmpty();
    }

    @Override
    public Set<Object> keySet() {
        if (this.map == null) {
            return Collections.emptySet();
        }
        return this.map.keySet();
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException("Illegal operation");
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> m) {
        throw new UnsupportedOperationException("Illegal operation");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("Illegal operation");
    }

    @Override
    public int size() {
        if (this.map == null) {
            return 0;
        }
        return this.map.size();
    }

    @Override
    public Collection<Object> values() {
        if (this.map == null) {
            return Collections.emptySet();
        }
        return this.map.values();
    }
    
    @Override
    public String toString() {
        if (this.map != null) {
            return this.map.toString();
        }
        return "{}";
    }

    @Override
    public Set<Object> keysOf(Object value) {
        if (this.reverseMap == null) {
            return null;
        }
        Set<Object> result = this.reverseMap.get(value);
        if (result == null) {
            return null;
        }
        return Collections.unmodifiableSet(result);
    }
}
