package pt.psoft.g1.psoftg1.bootstrapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service to handle distributed locking for bootstrap operations.
 * Ensures that only one replica executes the bootstrap data initialization
 * when multiple replicas share the same database.
 */
@Service
public class BootstrapLockService {
    
    private static final Logger logger = LoggerFactory.getLogger(BootstrapLockService.class);
    private static final String LOCK_TABLE = "bootstrap_lock";
    
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    public BootstrapLockService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        initLockTable();
    }
    
    /**
     * Creates the lock table if it doesn't exist.
     */
    private void initLockTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS " + LOCK_TABLE + " (" +
                "lock_name VARCHAR(100) PRIMARY KEY, " +
                "locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "locked_by VARCHAR(255)" +
                ")"
            );
            logger.info("Bootstrap lock table initialized");
        } catch (Exception e) {
            logger.warn("Could not create lock table (may already exist): {}", e.getMessage());
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
     * Uses database-level locking to ensure only one replica can hold the lock.
     * 
     * @param lockName the name of the lock to acquire
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryAcquireLock(String lockName) {
        String instanceId = getInstanceId();
        
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            
            // Try to insert a lock record - this will fail if lock already exists
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO " + LOCK_TABLE + " (lock_name, locked_by) VALUES (?, ?)")) {
                ps.setString(1, lockName);
                ps.setString(2, instanceId);
                ps.executeUpdate();
                connection.commit();
                logger.info("Bootstrap lock '{}' acquired by instance: {}", lockName, instanceId);
                return true;
            } catch (SQLException e) {
                connection.rollback();
                // Lock already exists, check if it's held by this instance
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT locked_by FROM " + LOCK_TABLE + " WHERE lock_name = ?")) {
                    ps.setString(1, lockName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String lockedBy = rs.getString("locked_by");
                            logger.info("Bootstrap lock '{}' already held by: {}", lockName, lockedBy);
                        }
                    }
                }
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error acquiring bootstrap lock '{}'", lockName, e);
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
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + LOCK_TABLE + " WHERE lock_name = ?",
                Integer.class,
                lockName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            logger.debug("Lock table check failed for '{}': {}", lockName, e.getMessage());
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
