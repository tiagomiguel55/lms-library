package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorRabbitmqController;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRabbitmqController;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreRabbitmqController;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;
import pt.psoft.g1.psoftg1.shared.model.AuthorEvents;
import pt.psoft.g1.psoftg1.shared.model.BookEvents;
import pt.psoft.g1.psoftg1.shared.model.GenreEvents;

@Profile("!test")
@Configuration
public class RabbitmqClientConfig {

    @Bean
    public DirectExchange direct() {
        return new DirectExchange("LMS.direct");
    }

    @Bean
    public DirectExchange directBooks() {
        return new DirectExchange("LMS.books");
    }

    @Bean
    public DirectExchange directAuthors() {
        return new DirectExchange("LMS.authors");
    }

    @Bean
    public DirectExchange directGenres() {
        return new DirectExchange("LMS.genres");
    }

    private static class ReceiverConfig {

        // Book Queues
        @Bean(name = "autoDeleteQueue_Book_Created")
        public Queue autoDeleteQueue_Book_Created() {
            System.out.println("autoDeleteQueue_Book_Created created!");
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Book_Updated() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Book_Deleted() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Book_Requested() {
            return new AnonymousQueue();
        }

        @Bean(name = "autoDeleteQueue_Validate_Book")
        public Queue autoDeleteQueue_Validate_Book() {
            System.out.println("autoDeleteQueue_Validate_Book created!");
            return new AnonymousQueue();
        }

        // Author Queues
        @Bean(name = "autoDeleteQueue_Author_Created")
        public Queue autoDeleteQueue_Author_Created() {
            System.out.println("autoDeleteQueue_Author_Created created!");
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Author_Updated() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Author_Deleted() {
            return new AnonymousQueue();
        }

        // Genre Queues
        @Bean(name = "autoDeleteQueue_Genre_Created")
        public Queue autoDeleteQueue_Genre_Created() {
            System.out.println("autoDeleteQueue_Genre_Created created!");
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Genre_Updated() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Genre_Deleted() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Lending_Returned() {
            return new AnonymousQueue();
        }

        // Book Bindings
        @Bean
        public Binding binding1(DirectExchange directBooks,
                @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created){
            return BindingBuilder.bind(autoDeleteQueue_Book_Created)
                    .to(directBooks)
                    .with(BookEvents.BOOK_CREATED);
        }

        @Bean
        public Binding binding2(DirectExchange directBooks,
                Queue autoDeleteQueue_Book_Updated){
            return BindingBuilder.bind(autoDeleteQueue_Book_Updated)
                    .to(directBooks)
                    .with(BookEvents.BOOK_UPDATED);
        }

        @Bean
        public Binding binding3(DirectExchange directBooks,
                Queue autoDeleteQueue_Book_Deleted){
            return BindingBuilder.bind(autoDeleteQueue_Book_Deleted)
                    .to(directBooks)
                    .with(BookEvents.BOOK_DELETED);
        }

        @Bean
        public Binding binding4(DirectExchange directBooks,
                Queue autoDeleteQueue_Book_Requested){
            return BindingBuilder.bind(autoDeleteQueue_Book_Requested)
                    .to(directBooks)
                    .with(BookEvents.BOOK_REQUESTED);
        }

        @Bean
        public Binding bindingValidateBook(DirectExchange directBooks,
                @Qualifier("autoDeleteQueue_Validate_Book") Queue autoDeleteQueue_Validate_Book){
            return BindingBuilder.bind(autoDeleteQueue_Validate_Book)
                    .to(directBooks)
                    .with("book.validate");
        }

        // Author Bindings
        @Bean
        public Binding bindingAuthor1(DirectExchange directAuthors,
                @Qualifier("autoDeleteQueue_Author_Created") Queue autoDeleteQueue_Author_Created){
            return BindingBuilder.bind(autoDeleteQueue_Author_Created)
                    .to(directAuthors)
                    .with(AuthorEvents.AUTHOR_CREATED);
        }

        @Bean
        public Binding bindingAuthor2(DirectExchange directAuthors,
                Queue autoDeleteQueue_Author_Updated){
            return BindingBuilder.bind(autoDeleteQueue_Author_Updated)
                    .to(directAuthors)
                    .with(AuthorEvents.AUTHOR_UPDATED);
        }

        @Bean
        public Binding bindingAuthor3(DirectExchange directAuthors,
                Queue autoDeleteQueue_Author_Deleted){
            return BindingBuilder.bind(autoDeleteQueue_Author_Deleted)
                    .to(directAuthors)
                    .with(AuthorEvents.AUTHOR_DELETED);
        }

        // Genre Bindings
        @Bean
        public Binding bindingGenre1(DirectExchange directGenres,
                @Qualifier("autoDeleteQueue_Genre_Created") Queue autoDeleteQueue_Genre_Created){
            return BindingBuilder.bind(autoDeleteQueue_Genre_Created)
                    .to(directGenres)
                    .with(GenreEvents.GENRE_CREATED);
        }

        @Bean
        public Binding bindingGenre2(DirectExchange directGenres,
                Queue autoDeleteQueue_Genre_Updated){
            return BindingBuilder.bind(autoDeleteQueue_Genre_Updated)
                    .to(directGenres)
                    .with(GenreEvents.GENRE_UPDATED);
        }

        @Bean
        public Binding bindingGenre3(DirectExchange directGenres,
                Queue autoDeleteQueue_Genre_Deleted){
            return BindingBuilder.bind(autoDeleteQueue_Genre_Deleted)
                    .to(directGenres)
                    .with(GenreEvents.GENRE_DELETED);
        }

        @Bean
        public Binding bindingLendingReturned(DirectExchange direct,
                Queue autoDeleteQueue_Lending_Returned){
            return BindingBuilder.bind(autoDeleteQueue_Lending_Returned)
                    .to(direct)
                    .with("LENDING_RETURNED");
        }

        // Receivers
        @Bean
        public BookRabbitmqController bookReceiver(BookService bookService,
                @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created){
            return new BookRabbitmqController();
        }

        @Bean
        public AuthorRabbitmqController authorReceiver(AuthorService authorService,
                @Qualifier("autoDeleteQueue_Author_Created") Queue autoDeleteQueue_Author_Created){
            return new AuthorRabbitmqController();
        }

        @Bean
        public GenreRabbitmqController genreReceiver(GenreService genreService,
                @Qualifier("autoDeleteQueue_Genre_Created") Queue autoDeleteQueue_Genre_Created){
            return new GenreRabbitmqController();
        }
    }
}
