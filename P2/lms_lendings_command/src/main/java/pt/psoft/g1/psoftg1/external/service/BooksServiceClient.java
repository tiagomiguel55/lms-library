package pt.psoft.g1.psoftg1.external.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BooksServiceClient {

    @Qualifier("booksServiceWebClient")
    private final WebClient booksServiceWebClient;

    /**
     * Check if a book exists in the Books service
     * @param isbn The ISBN of the book to check
     * @return true if the book exists, false otherwise
     */
    public boolean checkBookExists(String isbn) {
        try {
            Map<String, Boolean> response = booksServiceWebClient.get()
                    .uri("/api/books/exists/{isbn}", isbn)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            return response != null && response.getOrDefault("exists", false);
        } catch (Exception e) {
            System.err.println("Error checking book existence: " + e.getMessage());
            return false;
        }
    }
}
