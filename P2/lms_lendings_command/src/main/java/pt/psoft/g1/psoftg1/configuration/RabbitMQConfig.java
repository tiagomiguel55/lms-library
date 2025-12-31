package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.psoft.g1.psoftg1.bookmanagement.listeners.BookEventListener;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.readermanagement.listeners.ReaderEventListener;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.model.BookEvents;
import pt.psoft.g1.psoftg1.shared.model.LendingEvents;
import pt.psoft.g1.psoftg1.shared.model.ReaderEvents;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("LMS.direct");
    }

    // ========== BOOKS EXCHANGE (to receive events from Books Command service) ==========
    @Bean
    public DirectExchange booksExchange() {
        return new DirectExchange("books.exchange");
    }

    @Bean
    public Queue bookLendingRequestQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue bookLendingResponseQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue readerLendingRequestQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue readerLendingResponseQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue bookCreatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue bookUpdatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue bookDeletedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue lendingCreatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue lendingUpdatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue lendingDeletedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue lendingReturnedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue readerCreatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue readerUpdatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue readerDeletedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue validateBookQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue bookValidatedQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue validateReaderQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue readerValidatedQueue() {
        return new AnonymousQueue();
    }

    // ========== LENDING VALIDATION QUEUES (Durable Named Queues) ==========
    @Bean
    public Queue lendingValidationRequestQueue() {
        return new Queue("lending.validation.request", true);
    }

    @Bean
    public Queue lendingValidationResponseQueue() {
        return new Queue("lending.validation.response", true);
    }

    @Bean
    public Binding bookLendingRequestBinding(@Qualifier("directExchange") DirectExchange direct,
                                             @Qualifier("bookLendingRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("book.lending.requests");
    }

    @Bean
    public Binding bookLendingResponseBinding(@Qualifier("directExchange") DirectExchange direct,
                                             @Qualifier("bookLendingResponseQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("book.lending.responses");
    }

    @Bean
    public Binding readerLendingRequestBinding(@Qualifier("directExchange") DirectExchange direct,
                                             @Qualifier("readerLendingRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.lending.requests");
    }

    @Bean
    public Binding readerLendingResponseBinding(@Qualifier("directExchange") DirectExchange direct,
                                             @Qualifier("readerLendingResponseQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.lending.responses");
    }

    @Bean
    public Binding bookCreatedBinding(@Qualifier("booksExchange") DirectExchange booksExchange,
                                      @Qualifier("bookCreatedQueue") Queue bookCreatedQueue) {
        return BindingBuilder.bind(bookCreatedQueue)
                .to(booksExchange)
                .with(BookEvents.BOOK_CREATED);
    }

    @Bean
    public Binding bookUpdatedBinding(@Qualifier("booksExchange") DirectExchange booksExchange,
                                      @Qualifier("bookUpdatedQueue") Queue bookUpdatedQueue) {
        return BindingBuilder.bind(bookUpdatedQueue)
                .to(booksExchange)
                .with(BookEvents.BOOK_UPDATED);
    }

    @Bean
    public Binding bookDeletedBinding(@Qualifier("booksExchange") DirectExchange booksExchange,
                                      @Qualifier("bookDeletedQueue") Queue bookDeletedQueue) {
        return BindingBuilder.bind(bookDeletedQueue)
                .to(booksExchange)
                .with(BookEvents.BOOK_DELETED);
    }

    @Bean
    public Binding lendingCreatedBinding(@Qualifier("directExchange") DirectExchange direct,
                                         @Qualifier("lendingCreatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_CREATED);
    }

    @Bean
    public Binding lendingUpdatedBinding(@Qualifier("directExchange") DirectExchange direct,
                                         @Qualifier("lendingUpdatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_UPDATED);
    }

    @Bean
    public Binding lendingDeletedBinding(@Qualifier("directExchange") DirectExchange direct,
                                         @Qualifier("lendingDeletedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_DELETED);
    }

    @Bean
    public Binding lendingReturnedBinding(@Qualifier("directExchange") DirectExchange direct,
                                          @Qualifier("lendingReturnedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_RETURNED);
    }

    @Bean
    public Binding readerCreatedBinding(@Qualifier("directExchange") DirectExchange direct,
                                        @Qualifier("readerCreatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(ReaderEvents.READER_CREATED);
    }

    @Bean
    public Binding readerUpdatedBinding(@Qualifier("directExchange") DirectExchange direct,
                                        @Qualifier("readerUpdatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(ReaderEvents.READER_UPDATED);
    }

    @Bean
    public Binding readerDeletedBinding(@Qualifier("directExchange") DirectExchange direct,
                                        @Qualifier("readerDeletedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(ReaderEvents.READER_DELETED);
    }

    @Bean
    public Binding validateBookBinding(@Qualifier("directExchange") DirectExchange direct,
                                        @Qualifier("validateBookQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("book.validate");
    }

    @Bean
    public Binding bookValidatedBinding(@Qualifier("directExchange") DirectExchange direct,
                                        @Qualifier("bookValidatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("book.validated");
    }

    @Bean
    public Binding validateReaderBinding(@Qualifier("directExchange") DirectExchange direct,
                                         @Qualifier("validateReaderQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.validate");
    }

    @Bean
    public Binding readerValidatedBinding(@Qualifier("directExchange") DirectExchange direct,
                                          @Qualifier("readerValidatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.validated");
    }

    // ========== LENDING VALIDATION BINDINGS ==========
    @Bean
    public Binding lendingValidationRequestBinding(@Qualifier("directExchange") DirectExchange direct,
                                                     @Qualifier("lendingValidationRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("lending.validation.request");
    }

    @Bean
    public Binding lendingValidationResponseBinding(@Qualifier("directExchange") DirectExchange direct,
                                                      @Qualifier("lendingValidationResponseQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("lending.validation.response");
    }

    @Bean
    public BookEventListener bookReceiver(BookService bookService) {
        return new BookEventListener(bookService);
    }

    @Bean
    public ReaderEventListener readerReceiver(ReaderService readerService) {
        return new ReaderEventListener(readerService);
    }
}
