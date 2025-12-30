package pt.psoft.g1.psoftg1.configuration;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MongoDB configuration - no need for JPA auditing
 *
 * @author pgsousa
 *
 */
@Configuration
@EnableTransactionManagement
public class MongoConfig {

    /**
     * in case there is no authenticated user, for instance during bootstrapping, we will write SYSTEM
     *
     * @return
     */
    @Bean("auditorProvider")
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext()).map(SecurityContext::getAuthentication)
                .map(Authentication::getName).or(() -> Optional.of("SYSTEM"));
    }

    /**
     * MongoDB Transaction Manager bean required for @Transactional support
     *
     * @param dbFactory the MongoDB database factory
     * @return the transaction manager
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
