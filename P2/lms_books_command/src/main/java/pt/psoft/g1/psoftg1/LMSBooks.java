package pt.psoft.g1.psoftg1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
// testing comment
@SpringBootApplication
@EnableScheduling
public class LMSBooks {

    public static void main(String[] args) {
        SpringApplication.run(LMSBooks.class, args);
    }

}
