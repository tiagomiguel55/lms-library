package pt.psoft.g1.psoftg1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = {
    "pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.impl",
    "pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.impl",
    "pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl",
    "pt.psoft.g1.psoftg1.bookmanagement.repositories",
    "pt.psoft.g1.psoftg1.shared.infrastructure.repositories.impl"
})
public class LMSBooks {

    public static void main(String[] args) {
        SpringApplication.run(LMSBooks.class, args);
    }

}
