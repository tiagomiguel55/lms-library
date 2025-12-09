package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookRabbitmqController;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.model.AuthorEvents;
import pt.psoft.g1.psoftg1.shared.model.BookEvents;

@Profile("!test")
@Configuration
public class RabbitmqClientConfig {

    @Bean
    public DirectExchange direct() {
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

    @Configuration
    public static class ReceiverConfig {

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

        @Bean
        public Queue autoDeleteQueue_Author_Pending_Created() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Book_Finalized() {
            return new AnonymousQueue();
        }

            @Bean
            public Binding binding1 (DirectExchange direct,
                    @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created){
                return BindingBuilder.bind(autoDeleteQueue_Book_Created)
                        .to(direct)
                        .with(BookEvents.BOOK_CREATED);
            }

            @Bean
            public Binding binding2 (DirectExchange direct,
                    Queue autoDeleteQueue_Book_Updated){
                return BindingBuilder.bind(autoDeleteQueue_Book_Updated)
                        .to(direct)
                        .with(BookEvents.BOOK_UPDATED);
            }

            @Bean
            public Binding binding3 (DirectExchange direct,
                    Queue autoDeleteQueue_Book_Deleted){
                return BindingBuilder.bind(autoDeleteQueue_Book_Deleted)
                        .to(direct)
                        .with(BookEvents.BOOK_DELETED);
            }

            @Bean
            public Binding binding4 (DirectExchange direct,
                    Queue autoDeleteQueue_Book_Requested){
                return BindingBuilder.bind(autoDeleteQueue_Book_Requested)
                        .to(direct)
                        .with(BookEvents.BOOK_REQUESTED);
            }

            @Bean
            public Binding binding5 (DirectExchange directAuthors,
                    Queue autoDeleteQueue_Author_Pending_Created){
                return BindingBuilder.bind(autoDeleteQueue_Author_Pending_Created)
                        .to(directAuthors)
                        .with(AuthorEvents.AUTHOR_PENDING_CREATED);
            }

            @Bean
            public Binding binding6 (DirectExchange direct,
                    Queue autoDeleteQueue_Book_Finalized){
                return BindingBuilder.bind(autoDeleteQueue_Book_Finalized)
                        .to(direct)
                        .with(BookEvents.BOOK_FINALIZED);
            }

            @Bean
            public BookRabbitmqController receiver(
                    BookService bookService,
                    BookRepository bookRepository,
                    AuthorRepository authorRepository,
                    GenreRepository genreRepository,
                    PendingBookRequestRepository pendingBookRequestRepository,
                    @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created){
                return new BookRabbitmqController(bookService, bookRepository, authorRepository, genreRepository, pendingBookRequestRepository);
            }
        }
    }
