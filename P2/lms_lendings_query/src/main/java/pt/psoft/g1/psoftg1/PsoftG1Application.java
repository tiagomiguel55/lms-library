package pt.psoft.g1.psoftg1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
	"pt.psoft.g1.psoftg1.bookmanagement.repositories",
	"pt.psoft.g1.psoftg1.lendingmanagement.repositories",
	"pt.psoft.g1.psoftg1.readermanagement.repositories",
	"pt.psoft.g1.psoftg1.usermanagement.repositories"
})
public class PsoftG1Application {

	public static void main(String[] args) {
		SpringApplication.run(PsoftG1Application.class, args);
	}

}
