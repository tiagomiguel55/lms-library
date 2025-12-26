package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorRabbitmqController;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRabbitmqController;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreRabbitmqController;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.model.AuthorEvents;
import pt.psoft.g1.psoftg1.shared.model.BookEvents;
import pt.psoft.g1.psoftg1.shared.model.GenreEvents;

@Profile("!test")
@Configuration
public class RabbitmqClientConfig {

    // ========== EXCHANGES ==========
    @Bean
    @Primary
    public DirectExchange direct() {
        return new DirectExchange("books.exchange");
    }

    // Shared exchange across services for validation flows
    @Bean(name = "lmsDirectExchange")
    public DirectExchange lmsDirectExchange() {
        return new DirectExchange("LMS.direct");
    }

    @Bean
    public DirectExchange directAuthors() {
        return new DirectExchange("authors.exchange");
    }

    @Bean
    public DirectExchange directGenres() {
        return new DirectExchange("genres.exchange");
    }

    // ========== BOOK QUEUES (DURABLE) ==========
    @Bean(name = "autoDeleteQueue_Book_Created")
    public Queue autoDeleteQueue_Book_Created() {
        return new Queue("query.book.created", true);  // ✅ Durable, partilhada
    }

    @Bean
    public Queue autoDeleteQueue_Book_Updated() {
        return new Queue("query.book.updated", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Deleted() {
        return new Queue("query.book.deleted", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Finalized() {
        return new Queue("query.book.finalized", true);
    }

    // ========== AUTHOR QUEUES (DURABLE) ==========
    @Bean(name = "autoDeleteQueue_Author_Created")
    public Queue autoDeleteQueue_Author_Created() {
        return new Queue("query.author.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Author_Updated() {
        return new Queue("query.author.updated", true);
    }

    @Bean
    public Queue autoDeleteQueue_Author_Deleted() {
        return new Queue("query.author.deleted", true);
    }

    // ========== GENRE QUEUES (DURABLE) ==========
    @Bean(name = "autoDeleteQueue_Genre_Created")
    public Queue autoDeleteQueue_Genre_Created() {
        return new Queue("query.genre.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Genre_Updated() {
        return new Queue("query.genre.updated", true);
    }

    @Bean
    public Queue autoDeleteQueue_Genre_Deleted() {
        return new Queue("query.genre.deleted", true);
    }

        @Bean
        public Queue autoDeleteQueue_Lending_Returned() {
            return new AnonymousQueue();
        }

    // ========== BOOK BINDINGS ==========
    @Bean
    public Binding binding1(@Qualifier("direct") DirectExchange direct,
                            @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created) {
        return BindingBuilder.bind(autoDeleteQueue_Book_Created)
                .to(direct)
                .with(BookEvents.BOOK_CREATED);
    }

    @Bean
    public Binding binding2(@Qualifier("direct") DirectExchange direct,
                            Queue autoDeleteQueue_Book_Updated) {
        return BindingBuilder.bind(autoDeleteQueue_Book_Updated)
                .to(direct)
                .with(BookEvents.BOOK_UPDATED);
    }

    @Bean
    public Binding binding3(@Qualifier("direct") DirectExchange direct,
                            Queue autoDeleteQueue_Book_Deleted) {
        return BindingBuilder.bind(autoDeleteQueue_Book_Deleted)
                .to(direct)
                .with(BookEvents.BOOK_DELETED);
    }

    @Bean
    public Binding binding4(@Qualifier("direct") DirectExchange direct,
                            Queue autoDeleteQueue_Book_Finalized) {
        return BindingBuilder.bind(autoDeleteQueue_Book_Finalized)
                .to(direct)
                .with(BookEvents.BOOK_FINALIZED);
    }

    // ========== AUTHOR BINDINGS ==========
    @Bean
    public Binding bindingAuthor1(DirectExchange directAuthors,
                                  @Qualifier("autoDeleteQueue_Author_Created") Queue autoDeleteQueue_Author_Created) {
        return BindingBuilder.bind(autoDeleteQueue_Author_Created)
                .to(directAuthors)
                .with(AuthorEvents.AUTHOR_CREATED);
    }

    @Bean
    public Binding bindingAuthor2(DirectExchange directAuthors,
                                  Queue autoDeleteQueue_Author_Updated) {
        return BindingBuilder.bind(autoDeleteQueue_Author_Updated)
                .to(directAuthors)
                .with(AuthorEvents.AUTHOR_UPDATED);
    }

    @Bean
    public Binding bindingAuthor3(DirectExchange directAuthors,
                                  Queue autoDeleteQueue_Author_Deleted) {
        return BindingBuilder.bind(autoDeleteQueue_Author_Deleted)
                .to(directAuthors)
                .with(AuthorEvents.AUTHOR_DELETED);
    }

    // ========== GENRE BINDINGS ==========
    @Bean
    public Binding bindingGenre1(DirectExchange directGenres,
                                 @Qualifier("autoDeleteQueue_Genre_Created") Queue autoDeleteQueue_Genre_Created) {
        return BindingBuilder.bind(autoDeleteQueue_Genre_Created)
                .to(directGenres)
                .with(GenreEvents.GENRE_CREATED);
    }

    @Bean
    public Binding bindingGenre2(DirectExchange directGenres,
                                 Queue autoDeleteQueue_Genre_Updated) {
        return BindingBuilder.bind(autoDeleteQueue_Genre_Updated)
                .to(directGenres)
                .with(GenreEvents.GENRE_UPDATED);
    }

    @Bean
    public Binding bindingGenre3(DirectExchange directGenres,
                                 Queue autoDeleteQueue_Genre_Deleted) {
        return BindingBuilder.bind(autoDeleteQueue_Genre_Deleted)
                .to(directGenres)
                .with(GenreEvents.GENRE_DELETED);
    }

        @Bean
        public Binding bindingLendingReturned(@Qualifier("direct") DirectExchange direct,
                Queue autoDeleteQueue_Lending_Returned){
            return BindingBuilder.bind(autoDeleteQueue_Lending_Returned)
                    .to(direct)
                    .with("LENDING_RETURNED");
        }

    // ========== VALIDATION QUEUES/BINDINGS (book.validate) ==========
    @Bean(name = "validateBookQueue")
    public Queue validateBookQueue() {
        // Anonymous queue bound to LMS.direct with routing key book.validate
        return new AnonymousQueue();
    }

    @Bean
    public Binding bindingValidateBook(@Qualifier("lmsDirectExchange") DirectExchange lmsDirectExchange,
                                       @Qualifier("validateBookQueue") Queue validateBookQueue) {
        return BindingBuilder.bind(validateBookQueue)
                .to(lmsDirectExchange)
                .with("book.validate");
    }

    // ========== RECEIVERS ==========
    @Bean
    public BookRabbitmqController bookReceiver(
            BookService bookService,
            @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created) {
        return new BookRabbitmqController(bookService);
    }

    @Bean
    public AuthorRabbitmqController authorReceiver(
            AuthorRepository authorRepository,
            BookService bookService,
            @Qualifier("autoDeleteQueue_Author_Created") Queue autoDeleteQueue_Author_Created) {
        return new AuthorRabbitmqController(authorRepository, bookService);
    }

    @Bean
    public GenreRabbitmqController genreReceiver(
            GenreRepository genreRepository,
            BookService bookService,
            @Qualifier("autoDeleteQueue_Genre_Created") Queue autoDeleteQueue_Genre_Created) {
        return new GenreRabbitmqController(genreRepository, bookService);
    }
}