package pt.psoft.g1.psoftg1.bootstrapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service to handle distributed locking for bootstrap operations using MongoDB.
 * Ensures that only one replica executes the bootstrap data initialization
 * when multiple replicas share the same database.
 */
@Service
public class BootstrapLockService {
    
    private static final Logger logger = LoggerFactory.getLogger(BootstrapLockService.class);
    private static final String LOCK_COLLECTION = "bootstrap_lock";
    
    private final MongoTemplate mongoTemplate;
    
    public BootstrapLockService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * Attempts to acquire a distributed lock for bootstrap operations using default lock name.
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryAcquireLock() {
        return tryAcquireLock("BOOTSTRAP_LOCK");
    }
    
    /**
     * Attempts to acquire a distributed lock for bootstrap operations.
     * Uses MongoDB's atomic insert operation to ensure only one replica can hold the lock.
     * 
     * @param lockName the name of the lock to acquire
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryAcquireLock(String lockName) {
        String instanceId = getInstanceId();
        
        try {
            // Check if lock already exists
            Query query = new Query(Criteria.where("_id").is(lockName));
            BootstrapLock existingLock = mongoTemplate.findOne(query, BootstrapLock.class, LOCK_COLLECTION);
            
            if (existingLock != null) {
                logger.info("Bootstrap lock '{}' already held by: {}", lockName, existingLock.getLockedBy());
                return false;
            }
            
            // Try to insert lock document - this is atomic in MongoDB
            BootstrapLock lock = new BootstrapLock(lockName, instanceId, Instant.now().toString());
            mongoTemplate.insert(lock, LOCK_COLLECTION);
            logger.info("Bootstrap lock '{}' acquired by instance: {}", lockName, instanceId);
            return true;
        } catch (Exception e) {
            // Duplicate key error means another replica acquired the lock
            logger.info("Could not acquire bootstrap lock '{}' (likely held by another replica): {}", lockName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if bootstrap has already been completed using default lock name.
     * @return true if bootstrap was already done, false otherwise
     */
    public boolean isBootstrapCompleted() {
        return isBootstrapCompleted("BOOTSTRAP_LOCK");
    }
    
    /**
     * Checks if bootstrap has already been completed.
     * 
     * @param lockName the name of the lock to check
     * @return true if bootstrap was already done, false otherwise
     */
    public boolean isBootstrapCompleted(String lockName) {
        try {
            Query query = new Query(Criteria.where("_id").is(lockName));
            return mongoTemplate.exists(query, LOCK_COLLECTION);
        } catch (Exception e) {
            logger.debug("Lock check failed for '{}': {}", lockName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generates a unique identifier for this instance.
     */
    private String getInstanceId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.isEmpty()) {
            hostname = "instance-" + ProcessHandle.current().pid();
        }
        return hostname + "-" + System.currentTimeMillis();
    }
    
    /**
     * Inner class representing the lock document in MongoDB.
     */
    public static class BootstrapLock {
        private String id;
        private String lockedBy;
        private String lockedAt;
        
        public BootstrapLock() {}
        
        public BootstrapLock(String id, String lockedBy, String lockedAt) {
            this.id = id;
            this.lockedBy = lockedBy;
            this.lockedAt = lockedAt;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLockedBy() { return lockedBy; }
        public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
        public String getLockedAt() { return lockedAt; }
        public void setLockedAt(String lockedAt) { this.lockedAt = lockedAt; }
    }
}
