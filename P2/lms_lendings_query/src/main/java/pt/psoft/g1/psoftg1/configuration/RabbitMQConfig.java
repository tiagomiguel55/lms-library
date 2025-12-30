package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.psoft.g1.psoftg1.bookmanagement.listeners.BookEventListener;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.lendingmanagement.listeners.LendingEventListener;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
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
    public Queue validateReaderQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding bookLendingRequestBinding(DirectExchange direct,
                                             @Qualifier("bookLendingRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("book.lending.requests");
    }

    @Bean
    public Binding bookLendingResponseBinding(DirectExchange direct,
                                             @Qualifier("bookLendingResponseQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("book.lending.responses");
    }

    @Bean
    public Binding readerLendingRequestBinding(DirectExchange direct,
                                             @Qualifier("readerLendingRequestQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.lending.requests");
    }

    @Bean
    public Binding readerLendingResponseBinding(DirectExchange direct,
                                             @Qualifier("readerLendingResponseQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.lending.responses");
    }
    @Bean
    public Binding bookCreatedBinding(DirectExchange direct,
                                      @Qualifier("bookCreatedQueue") Queue bookCreatedQueue) {
        return BindingBuilder.bind(bookCreatedQueue)
                .to(direct)
                .with(BookEvents.BOOK_CREATED);
    }

    @Bean
    public Binding bookUpdatedBinding(DirectExchange direct,
                                      @Qualifier("bookUpdatedQueue") Queue bookUpdatedQueue) {
        return BindingBuilder.bind(bookUpdatedQueue)
                .to(direct)
                .with(BookEvents.BOOK_UPDATED);
    }

    @Bean
    public Binding bookDeletedBinding(DirectExchange direct,
                                      @Qualifier("bookDeletedQueue") Queue bookDeletedQueue) {
        return BindingBuilder.bind(bookDeletedQueue)
                .to(direct)
                .with(BookEvents.BOOK_DELETED);
    }

    @Bean
    public Binding lendingCreatedBinding(DirectExchange direct,
                                         @Qualifier("lendingCreatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_CREATED);
    }

    @Bean
    public Binding lendingUpdatedBinding(DirectExchange direct,
                                         @Qualifier("lendingUpdatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_UPDATED);
    }

    @Bean
    public Binding lendingDeletedBinding(DirectExchange direct,
                                         @Qualifier("lendingDeletedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_DELETED);
    }

    @Bean
    public Binding lendingReturnedBinding(DirectExchange direct,
                                          @Qualifier("lendingReturnedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(LendingEvents.LENDING_RETURNED);
    }

    @Bean
    public Binding readerCreatedBinding(DirectExchange direct,
                                        @Qualifier("readerCreatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(ReaderEvents.READER_CREATED);
    }

    @Bean
    public Binding readerUpdatedBinding(DirectExchange direct,
                                        @Qualifier("readerUpdatedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(ReaderEvents.READER_UPDATED);
    }

    @Bean
    public Binding readerDeletedBinding(DirectExchange direct,
                                        @Qualifier("readerDeletedQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with(ReaderEvents.READER_DELETED);
    }

    @Bean
    public Binding validateReaderBinding(DirectExchange direct,
                                         @Qualifier("validateReaderQueue") Queue queue) {
        return BindingBuilder.bind(queue)
                .to(direct)
                .with("reader.validate");
    }

    @Bean
    public BookEventListener bookReceiver(BookService bookService) {
        return new BookEventListener(bookService);
    }

    @Bean
    public LendingEventListener lendingReceiver(LendingService lendingService) {
        return new LendingEventListener(lendingService);
    }

    @Bean
    public ReaderEventListener readerReceiver(ReaderService readerService) {
        return new ReaderEventListener(readerService);
    }
}
