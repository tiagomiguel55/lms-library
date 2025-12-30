package pt.psoft.g1.psoftg1.configuration;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * MongoDB configuration - no need for JPA auditing
 * Transaction management is disabled since MongoDB standalone doesn't support transactions
 *
 * @author pgsousa
 */
@Configuration
public class MongoConfig {

    /**
     * in case there is no authenticated user, for instance during bootstrapping, we will write SYSTEM
     *
     * @return the auditor aware bean
     */
    @Bean("auditorProvider")
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext()).map(SecurityContext::getAuthentication)
                .map(Authentication::getName).or(() -> Optional.of("SYSTEM"));
    }

    /**
     * Provide a TransactionManager backed by MongoDB so that @Transactional annotations
     * used in this service resolve correctly. Note: Mongo transactions require a replica set.
     */
    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
