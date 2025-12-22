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

    // Exchanges permanecem iguais
    @Bean
    public DirectExchange direct() {
        return new DirectExchange("books.exchange");
    }
    @Bean
    public DirectExchange directAuthors() {
        return new DirectExchange("authors.exchange");
    }
    @Bean
    public DirectExchange directGenres() {
        return new DirectExchange("genres.exchange");
    }

    // ✅ Queues NOMEADAS e DURÁVEIS (work queue - apenas 1 réplica processa)
    @Bean
    public Queue autoDeleteQueue_Book_Created() {
        return new Queue("book.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Updated() {
        return new Queue("book.updated", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Deleted() {
        return new Queue("book.deleted", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Requested() {
        return new Queue("book.requested", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Requested_Author() {
        return new Queue("book.requested.author", true);
    }

    @Bean
    public Queue autoDeleteQueue_Author_Pending_Created() {
        return new Queue("book.author.pending.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Finalized() {
        return new Queue("book.finalized", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Finalized_Author() {
        return new Queue("book.finalized.author", true);
    }

    @Bean
    public Queue autoDeleteQueue_Author_Creation_Failed() {
        return new Queue("book.author.creation.failed", true);
    }

    @Bean
    public Queue autoDeleteQueue_Genre_Pending_Created() {
        return new Queue("book.genre.pending.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Genre_Creation_Failed() {
        return new Queue("book.genre.creation.failed", true);
    }

    @Bean
    public Queue autoDeleteQueue_Genre_Finalized() {
        return new Queue("book.genre.finalized", true);
    }

    @Bean
    public Queue autoDeleteQueue_Author_Created() {
        return new Queue("book.author.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Genre_Created() {
        return new Queue("book.genre.created", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Requested_Genre() {
        return new Queue("book.requested.genre", true);
    }

    @Bean
    public Queue autoDeleteQueue_Book_Finalized_Genre() {
        return new Queue("book.finalized.genre", true);
    }

    // Bindings permanecem iguais
    @Bean
    public Binding binding1(DirectExchange direct,
                            @Qualifier("autoDeleteQueue_Book_Created") Queue autoDeleteQueue_Book_Created){
        return BindingBuilder.bind(autoDeleteQueue_Book_Created)
                .to(direct)
                .with(BookEvents.BOOK_CREATED);
    }

    @Bean
    public Binding binding2(DirectExchange direct,
                            Queue autoDeleteQueue_Book_Updated){
        return BindingBuilder.bind(autoDeleteQueue_Book_Updated)
                .to(direct)
                .with(BookEvents.BOOK_UPDATED);
    }

    @Bean
    public Binding binding3(DirectExchange direct,
                            Queue autoDeleteQueue_Book_Deleted){
        return BindingBuilder.bind(autoDeleteQueue_Book_Deleted)
                .to(direct)
                .with(BookEvents.BOOK_DELETED);
    }

    @Bean
    public Binding binding4(DirectExchange direct,
                            Queue autoDeleteQueue_Book_Requested){
        return BindingBuilder.bind(autoDeleteQueue_Book_Requested)
                .to(direct)
                .with(BookEvents.BOOK_REQUESTED);
    }

    @Bean
    public Binding binding4b(DirectExchange direct,
                             Queue autoDeleteQueue_Book_Requested_Author){
        return BindingBuilder.bind(autoDeleteQueue_Book_Requested_Author)
                .to(direct)
                .with(BookEvents.BOOK_REQUESTED);
    }

    @Bean
    public Binding binding5(DirectExchange directAuthors,
                            Queue autoDeleteQueue_Author_Pending_Created){
        return BindingBuilder.bind(autoDeleteQueue_Author_Pending_Created)
                .to(directAuthors)
                .with(AuthorEvents.AUTHOR_PENDING_CREATED);
    }

    @Bean
    public Binding binding6(DirectExchange direct,
                            Queue autoDeleteQueue_Book_Finalized){
        return BindingBuilder.bind(autoDeleteQueue_Book_Finalized)
                .to(direct)
                .with(BookEvents.BOOK_FINALIZED);
    }

    @Bean
    public Binding binding6b(DirectExchange direct,
                             Queue autoDeleteQueue_Book_Finalized_Author){
        return BindingBuilder.bind(autoDeleteQueue_Book_Finalized_Author)
                .to(direct)
                .with(BookEvents.BOOK_FINALIZED);
    }

    @Bean
    public Binding binding7(DirectExchange directAuthors,
                            Queue autoDeleteQueue_Author_Creation_Failed){
        return BindingBuilder.bind(autoDeleteQueue_Author_Creation_Failed)
                .to(directAuthors)
                .with(AuthorEvents.AUTHOR_CREATION_FAILED);
    }

    @Bean
    public Binding binding8(DirectExchange directGenres,
                            Queue autoDeleteQueue_Genre_Pending_Created){
        return BindingBuilder.bind(autoDeleteQueue_Genre_Pending_Created)
                .to(directGenres)
                .with(pt.psoft.g1.psoftg1.shared.model.GenreEvents.GENRE_PENDING_CREATED);
    }

    @Bean
    public Binding binding9(DirectExchange directGenres,
                            Queue autoDeleteQueue_Genre_Creation_Failed){
        return BindingBuilder.bind(autoDeleteQueue_Genre_Creation_Failed)
                .to(directGenres)
                .with(pt.psoft.g1.psoftg1.shared.model.GenreEvents.GENRE_CREATION_FAILED);
    }

    @Bean
    public Binding binding10(DirectExchange directGenres,
                             Queue autoDeleteQueue_Genre_Finalized){
        return BindingBuilder.bind(autoDeleteQueue_Genre_Finalized)
                .to(directGenres)
                .with(pt.psoft.g1.psoftg1.shared.model.GenreEvents.GENRE_PENDING_CREATED);
    }

    @Bean
    public Binding binding11(DirectExchange directAuthors,
                             Queue autoDeleteQueue_Author_Created){
        return BindingBuilder.bind(autoDeleteQueue_Author_Created)
                .to(directAuthors)
                .with(AuthorEvents.AUTHOR_CREATED);
    }

    @Bean
    public Binding binding12(DirectExchange directGenres,
                             Queue autoDeleteQueue_Genre_Created){
        return BindingBuilder.bind(autoDeleteQueue_Genre_Created)
                .to(directGenres)
                .with(pt.psoft.g1.psoftg1.shared.model.GenreEvents.GENRE_CREATED);
    }

    @Bean
    public Binding binding13(DirectExchange direct,
                             Queue autoDeleteQueue_Book_Requested_Genre){
        return BindingBuilder.bind(autoDeleteQueue_Book_Requested_Genre)
                .to(direct)
                .with(BookEvents.BOOK_REQUESTED);
    }

    @Bean
    public Binding binding14(DirectExchange direct,
                             Queue autoDeleteQueue_Book_Finalized_Genre){
        return BindingBuilder.bind(autoDeleteQueue_Book_Finalized_Genre)
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