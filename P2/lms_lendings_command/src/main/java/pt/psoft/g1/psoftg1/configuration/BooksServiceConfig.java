package pt.psoft.g1.psoftg1.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class BooksServiceConfig {

    @Value("${books.service.url}")
    private String booksServiceUrl;

    @Bean(name = "booksServiceWebClient")
    public WebClient booksServiceWebClient() {
        return WebClient.builder()
                .baseUrl(booksServiceUrl)
                .build();
    }
}
