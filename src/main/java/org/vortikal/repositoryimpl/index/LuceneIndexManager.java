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
package org.vortikal.repositoryimpl.index;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import EDU.oswego.cs.dl.util.concurrent.Mutex;


/**
 * Manages access to a single file-system based Lucene index instance.
 * 
 * Contains mutex-locking functionality.
 *
 * TODO: JavaDoc must be updated
 * TODO: Design a more elegant locking API
 * 
 * @author oyviste
 */
public class LuceneIndexManager implements InitializingBean, DisposableBean {
    
    private final Log LOG = LogFactory.getLog(LuceneIndexManager.class);

    private FSBackedLuceneIndex fsIndex;
    
    private String storageRootPath;
    private String storageId;
    private int optimizeInterval = 100;
    private int commitCounter = 0;
    private int mergeFactor = 10;
    private int maxBufferedDocs = 100;
    private int maxMergeDocs = 10000;
    private int maxLockAcquireTimeOnShutdown = 30; // 30 seconds max to wait for lock when shutting down
    private boolean eraseExistingIndex = false;
    private boolean forceUnlock = false;
    
    /** If <code>true</code>, the underlying index will be explicitly closed 
     * after initialization is complete. Can be used for index instances you 
     * don't want to have open before it is explicitly needed.
     * Such an instance must be re-initialized before it can be 
     * used (by calling {@link #reinitialize()}). 
     **/
    private boolean closeAfterInitialization = false;
    
    /* Internal mutex write lock backing the public locking functions of this class. */
    private Mutex lock = new Mutex();
    
    public void afterPropertiesSet() throws BeanInitializationException {
        
        try {
            // Initialization of physical storage directory
            File storageDirectory = initializeStorageDirectory(this.storageRootPath, 
                                                               this.storageId);
            
            // Initialization of file-system backed Lucene index
            this.fsIndex = new FSBackedLuceneIndex(storageDirectory, 
                                                   new KeywordAnalyzer(),
                                                   this.eraseExistingIndex,
                                                   this.forceUnlock);
            
            this.fsIndex.setMaxMergeDocs(this.maxMergeDocs);
            this.fsIndex.setMergeFactor(this.mergeFactor);
            this.fsIndex.setMaxBufferedDocs(this.maxBufferedDocs);
            
            LOG.info("Initialization of index '" + this.getStorageId() + "' complete.");
            
            if (this.closeAfterInitialization) {
                LOG.info("Closing instance after initialization.");
                this.fsIndex.close();
            }
            
        } catch (IOException io) {
            LOG.error("Got IOException while initializing index: " + io.getMessage());
            throw new BeanInitializationException("Got IOException while initializing index", io);
        }
    }
    
    private File initializeStorageDirectory(String storageRootPath, String storageId)
        throws IOException {
        
        File storageDirectory = new File(storageRootPath, storageId);
        
        if (storageDirectory.isDirectory()) {
            if (! storageDirectory.canWrite()) {
                throw new IOException("Resolved storage directory '"
                        + storageDirectory.getAbsolutePath() 
                        + "' is not writable");
            }
        } else if (storageDirectory.isFile()) {
            throw new IOException("Resolved storage directory '" 
                    + storageDirectory.getAbsolutePath()
                    + "' is a file");
        } else {
            // Directory does not exist, we need to create it.
            if (!storageDirectory.mkdir()) {
                throw new IOException("Failed to create resolved storage directory '"
                        + storageDirectory.getAbsolutePath() 
                        + "'");
            }
        }
        
        return storageDirectory;
    }
    
    public void releaseIndexSearcher(IndexSearcher searcher) throws IOException {
        if (searcher == null) {
            return;
        }
        
        this.fsIndex.releaseReadOnlyIndexReader(searcher.getIndexReader());
    }
    
    public IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(this.fsIndex.getReadOnlyIndexReader());
    }
    
    public IndexReader getReadOnlyIndexReader() throws IOException {
        return this.fsIndex.getReadOnlyIndexReader();
    }
    
    public void releaseReadOnlyIndexReader(IndexReader readOnlyReader) throws IOException {
        this.fsIndex.releaseReadOnlyIndexReader(readOnlyReader);
    }

    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public IndexReader getIndexReader() throws IOException {
        return this.fsIndex.getIndexReader();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public IndexWriter getIndexWriter() throws IOException {
        return this.fsIndex.getIndexWriter();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void clearContents() throws IOException {
        this.fsIndex.createNewIndex();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void reinitialize() throws IOException {
        this.fsIndex.reinitialize();
    }
    
    //  CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void close() throws IOException {
        this.fsIndex.close();
    }
    
    public boolean isClosed() {
        return this.fsIndex.isClosed();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void optimize() throws IOException {
        this.fsIndex.getIndexWriter().optimize();
    }
    
    // Commit changes done by reader/writer and optimize index if necessary.
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void commit() throws IOException {
        // Optimize index, if we've reached 'optimizeInterval' number of commits.
        if (++this.commitCounter % this.optimizeInterval == 0) {
            LOG.info("Reached " + this.commitCounter 
                                        + " commits, auto-optimizing index ..");
            this.fsIndex.getIndexWriter().optimize();
            LOG.info("Auto-optimization completed.");
        }
        
        this.fsIndex.commit();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void corruptionTest() throws IOException {
        this.fsIndex.corruptionTest();
    }
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
       LOG.info("Graceful index shutdown, waiting for write lock on index '" 
               + this.storageId + "' ..");
       if (writeLockAttempt(this.maxLockAcquireTimeOnShutdown * 1000)) {
           LOG.info("Got write lock on index '" + this.storageId 
                   + "', closing down.");
           this.fsIndex.close();
       } else {
           LOG.warn("Failed to acquire the write lock on index '" 
              + this.storageId + "' within "
              + " the time limit of " + this.maxLockAcquireTimeOnShutdown 
              + " seconds, index might be corrupted.");
           
           // XXX: maybe force closing anyway, here.
       }
    }

    /**
     * Explicit write locking. Must be acquired before doing any write
     * operations on index, using either the reader or the writer.
     */
    public boolean writeLockAcquire() {
        try {
            this.lock.acquire();
        } catch (InterruptedException ie) {
            return false;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Thread '" + Thread.currentThread().getName() + 
                         "' got write lock on index '" 
                    + this.storageId + "'.");
        }
        
        return true;

    }

    /**
     * Attempt write lock with timeout 
     */
    public boolean writeLockAttempt(long timeout) {
        try {
            boolean acquired = this.lock.attempt(timeout);
            
            if (LOG.isDebugEnabled()) {
                if (acquired) {
                    LOG.debug("Thread '" + Thread.currentThread().getName() + 
                    "' got write lock on index '" + this.storageId + "'.");
                } else {
                    LOG.debug("Thread '" + Thread.currentThread().getName() + 
                      "' failed to acquire write lock on index '" 
                            + this.storageId 
                            + "' after waiting for " + timeout + " ms");
                }
            }
            
            return acquired;
        } catch (InterruptedException ie) {
            return false;
        }
    }

    /**
     * Write lock release.
     */
    public void writeLockRelease() {
        
        this.lock.release();
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Thread '" + Thread.currentThread().getName() + 
                         "' released write lock on index '" 
                    + this.storageId + "'.");
        }
        
    }
    
    public boolean isEraseExistingIndex() {
        return this.eraseExistingIndex;
    }

    public void setEraseExistingIndex(boolean eraseExistingIndex) {
        this.eraseExistingIndex = eraseExistingIndex;
    }

    public boolean isForceUnlock() {
        return this.forceUnlock;
    }

    public void setForceUnlock(boolean forceUnlock) {
        this.forceUnlock = forceUnlock;
    }

    public int getMaxMergeDocs() {
        return this.maxMergeDocs;
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    public int getMergeFactor() {
        return this.mergeFactor;
    }

    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }

    public int getMaxBufferedDocs() {
        return this.maxBufferedDocs;
    }

    public void setMaxBufferedDocs(int maxBufferedDocs) {
        this.maxBufferedDocs = maxBufferedDocs;
    }

    public int getOptimizeInterval() {
        return this.optimizeInterval;
    }

    public void setOptimizeInterval(int optimizeInterval) {
        this.optimizeInterval = optimizeInterval;
    }

    public void setMaxLockAcquireTimeOnShutdown(int maxLockAcquireTimeOnShutdown) {
        this.maxLockAcquireTimeOnShutdown = maxLockAcquireTimeOnShutdown;
    }

    public String getStorageId() {
        return this.storageId;
    }

    @Required
    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public String getStorageRootPath() {
        return this.storageRootPath;
    }

    @Required
    public void setStorageRootPath(String storageRootPath) {
        this.storageRootPath = storageRootPath;
    }

    public boolean isCloseAfterInitialization() {
        return closeAfterInitialization;
    }

    public void setCloseAfterInitialization(boolean closeAfterInitialization) {
        this.closeAfterInitialization = closeAfterInitialization;
    }
    
}
