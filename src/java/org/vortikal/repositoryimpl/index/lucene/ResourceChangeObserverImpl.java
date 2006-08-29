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
package org.vortikal.repositoryimpl.index.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.index.observation.ResourceChange;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeNotifier;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver;
import org.vortikal.repositoryimpl.index.observation.ResourceDeletion;

/**
 * Apply resource changes to Lucene index.
 * 
 * TODO: Redo-logging probably belongs here.
 * @author oyviste
 *
 */
public class ResourceChangeObserverImpl implements BeanNameAware,
        ResourceChangeObserver, InitializingBean {

    private static Log logger = LogFactory.getLog(ResourceChangeObserverImpl.class);
    
    private LuceneIndex index;
    private ResourceChangeNotifier notifier;
    private String beanName;
    private boolean enabled;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        }
        
        // If a notifier is configured, we register ourselves.
        enable();
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#disable()
     */
    public synchronized void disable() {
        if (this.notifier != null) {
            if (this.notifier.unregisterObserver(this)) {
                logger.info("Un-registered from resource change notifier.");
            }
        }
        this.enabled = false;
        logger.info("Disabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#enable()
     */
    public synchronized void enable() {
        if (this.notifier != null) {
            if (this.notifier.registerObserver(this)) {
                logger.info("Registered with resource change notifier.");
            }
        }
        this.enabled = true;
        logger.info("Enabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#isEnabled()
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @see ResourceChangeObserver#notifyResourceChanges(List)
     * 
     * TODO: Perhaps make this method non-blocking, so that all resource changes
     * can be distributed to all indexes independently. This would require us
     * to keep our own private redo-log at this level, in case things go wrong. 
     */
    public synchronized void notifyResourceChanges(List changes) {
        if (! isEnabled()) {
            logger.warn("Discarding received changes. Reason: disabled.");
            return;
        }
        
        // Take lock immediately, we'll be doing some writing.
        if (! this.index.lockAcquire()) {
            logger.error("Unable to acquire lock on index, will not attempt to " +
                         "apply modifications, changes are lost !");
            return;
        }
        
        // Update Lucene index according to changes in list.
        // List of resources that will be updated or added.
        List documentUpdates = new ArrayList();
        
        // List of resources that will be deleted from index.
        // (ResourceDeletion objects)
        List documentDeletes = new ArrayList();
        
        // List of subtree/collection deletions
        // (ResourceDeletion objects)
        List deltrees = new ArrayList();
        
        Iterator i = changes.iterator();
        while (i.hasNext()) {
            ResourceChange c = (ResourceChange)i.next();
            
            // Unfortunatly we cannot skip ACL modification events, even though
            // they are not siginificant for the old indexing system.
            //
            // This is because an ACL modification might mask earlier creation/modification
            // events in changelog, and thus, the resource needs to be 
            // added/updated anyway.
            
            // Non-selective (re-)indexing (all resource creation/modification types will
            // trigger re-indexing, except deletion).
            if (! (c instanceof ResourceDeletion)) {
                documentDeletes.add(c);
                documentUpdates.add(c);
            } else {
                if (((ResourceDeletion)c).isCollection()) {
                    deltrees.add(c);
                } else {
                    documentDeletes.add(c);
                }
            }
        }
        
        try {
            // Perform deletions first
            if (logger.isDebugEnabled()) {
                if (documentDeletes.size() > 0) {
                    logger.debug("Performing " + documentDeletes.size() + " document deletion(s).");
                }
            }
            deleteDocuments(documentDeletes);
            
            // Perform any subtree deletions
            delTrees(deltrees);
            
            // Perform additions/updates
            if (logger.isDebugEnabled()) {
                if (documentUpdates.size() > 0) {
                    logger.debug("Performing " + documentUpdates.size() + " document addition(s)/update(s)");
                }
            }
            addDocuments(documentUpdates);
            this.index.commit(); // commit (will close IndexWriter and IndexReader)
        } catch (IOException io) {
            logger.warn("Got IOException while indexing: ", io);
        } finally {
            // Release the index lock !
            this.index.lockRelease();
        }
    }
    
    /**
     * Add documents in uri list to index.
     */
    private void addDocuments(List uris)
    throws IOException {
        IndexWriter writer = this.index.getIndexWriter();
        // Iterate through list of URLs and add
        Iterator i = uris.iterator();
        while (i.hasNext()) {
            ResourceChange c = (ResourceChange)i.next();
            String uri = c.getUri();
            boolean added = this.index.addDocument(writer, uri);
            if (logger.isDebugEnabled()) {
                if (added) logger.debug("Added/updated document: '" + uri + "'");
            }
        }
    }
    
    /**
     * Delete documents in list from index.
     */
    private void deleteDocuments(List uris)
    throws IOException {
        // Iterate through list of URIs and delete from index.
        IndexReader reader = this.index.getIndexReader();
        Iterator i = uris.iterator();
        while (i.hasNext()) {
            String uri = ((ResourceChange)i.next()).getUri();
            if (this.index.deleteDocument(reader, uri)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleted document from index: '" + uri + "'");
                }
            }
        }
    }
    
    /**
     * Delete subtrees from index using using parent collection IDs.
     */
    private void delTrees(List uris)
    throws IOException {
        IndexReader reader = this.index.getIndexReader();
        Iterator i = uris.iterator();
        while (i.hasNext()) {
            ResourceDeletion r = (ResourceDeletion)i.next();
            String collectionId = r.getResourceId();
            String uri = r.getUri();
            logger.debug("Deleting subtree '" + uri + "' from index..");
            
            int deleted = this.index.deleteSubtree(reader, collectionId, uri);
            
            if (logger.isDebugEnabled()) {
                if (deleted > 0) logger.debug("Deleted " + deleted + " document(s) from index.");
            }
        }
    }
    
    public String getObserverId() {
        return this.beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public void setIndex(LuceneIndex index) {
        this.index = index;
    }

    public void setNotifier(ResourceChangeNotifier notifier) {
        this.notifier = notifier;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[ Observer: id='");
        buffer.append(getObserverId());
        buffer.append("', for index '");
        buffer.append(this.index.getIndexId());
        buffer.append("' ]");
        return buffer.toString();
    }
}
