package pt.psoft.g1.psoftg1.bootstrapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service to handle distributed locking for bootstrap operations using PostgreSQL.
 * Ensures that only one replica executes the bootstrap data initialization
 * when multiple replicas share the same database.
 */
@Service
public class BootstrapLockService {
    
    private static final Logger logger = LoggerFactory.getLogger(BootstrapLockService.class);
    private static final String LOCK_TABLE = "bootstrap_lock";
    
    private final JdbcTemplate jdbcTemplate;
    
    public BootstrapLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createLockTableIfNotExists();
    }
    
    /**
     * Creates the bootstrap_lock table if it doesn't exist.
     */
    private void createLockTableIfNotExists() {
        try {
            String createTableSql = 
                "CREATE TABLE IF NOT EXISTS " + LOCK_TABLE + " (" +
                "    lock_name VARCHAR(255) PRIMARY KEY," +
                "    locked_by VARCHAR(255) NOT NULL," +
                "    locked_at TIMESTAMP NOT NULL" +
                ")";
            jdbcTemplate.execute(createTableSql);
        } catch (DataAccessException e) {
            logger.warn("Could not create bootstrap_lock table: {}", e.getMessage());
        }
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
     * Uses PostgreSQL's unique constraint to ensure only one replica can hold the lock.
     * 
     * @param lockName the name of the lock to acquire
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryAcquireLock(String lockName) {
        String instanceId = getInstanceId();
        
        try {
            // Check if lock already exists
            String checkSql = "SELECT locked_by FROM " + LOCK_TABLE + " WHERE lock_name = ?";
            String existingLock = jdbcTemplate.queryForObject(checkSql, String.class, lockName);
            
            if (existingLock != null) {
                logger.info("Bootstrap lock '{}' already held by: {}", lockName, existingLock);
                return false;
            }
        } catch (Exception e) {
            // No lock exists, continue to acquire
        }
        
        try {
            // Try to insert lock record - this is atomic with unique constraint
            String insertSql = "INSERT INTO " + LOCK_TABLE + " (lock_name, locked_by, locked_at) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, lockName, instanceId, Instant.now());
            logger.info("Bootstrap lock '{}' acquired by instance: {}", lockName, instanceId);
            return true;
        } catch (DataAccessException e) {
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
            String sql = "SELECT COUNT(*) FROM " + LOCK_TABLE + " WHERE lock_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, lockName);
            return count != null && count > 0;
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
}
