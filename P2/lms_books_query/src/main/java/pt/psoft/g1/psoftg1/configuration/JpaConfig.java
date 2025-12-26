package pt.psoft.g1.psoftg1.configuration;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MongoDB Configuration for lms_books_query
 *
 * @author pgsousa
 */
@Configuration
@EnableMongoAuditing(auditorAwareRef = "auditorProvider")
@EnableMongoRepositories(basePackages = {
    "pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.impl",
    "pt.psoft.g1.psoftg1.bookmanagement.repositories",
    "pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.impl",
    "pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl",
    "pt.psoft.g1.psoftg1.shared.infrastructure.repositories.impl"
})
@EnableTransactionManagement
public class JpaConfig {

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
     * Provide a TransactionManager backed by MongoDB so that @Transactional annotations
     * used in this service resolve correctly. Note: Mongo transactions require a replica set.
     */
    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
