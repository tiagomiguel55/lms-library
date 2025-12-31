package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.psoft.g1.psoftg1.bookmanagement.listeners.BookEventListener;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.listeners.GenreEventListener;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;
import pt.psoft.g1.psoftg1.lendingmanagement.listeners.LendingEventListener;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQPMapper;
import pt.psoft.g1.psoftg1.readermanagement.listeners.ReaderEventListener;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.listeners.RpcBootstrapListener;
import pt.psoft.g1.psoftg1.shared.model.BookEvents;
import pt.psoft.g1.psoftg1.shared.model.GenreEvents;
import pt.psoft.g1.psoftg1.shared.model.LendingEvents;
import pt.psoft.g1.psoftg1.shared.model.ReaderEvents;
import pt.psoft.g1.psoftg1.shared.publishers.RpcBootstrapPublisher;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Definindo uma exchange
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("LMS.direct");
    }

    // Definindo filas para eventos de livros, autores e gêneros
    private static class ReceiverConfig {

        @Bean
        public Queue readerLendingRequestQueue() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue readerLendingResponseQueue() {
            return new AnonymousQueue();
        }


        @Bean
        public Queue readerServiceInstanciatedQueue() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue rpcReplyQueue() {
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
        public Queue genreCreatedQueue() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue genreUpdatedQueue() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue genreDeletedQueue() {
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
        public Queue readerCreatedQueue() {
            return new Queue("readers.reader.created", true);  // Named durable queue
        }

        @Bean
        public Queue readerUpdatedQueue() {
            return new Queue("readers.reader.updated", true);  // Named durable queue
        }

        @Bean
        public Queue readerDeletedQueue() {
            return new Queue("readers.reader.deleted", true);  // Named durable queue
        }

        // SAGA queues for Reader-User creation (DURABLE NAMED QUEUES)
        @Bean
        public Queue readerUserRequestedUserQueue() {
            return new Queue("reader.user.requested.user", true);
        }

        @Bean
        public Queue readerUserRequestedReaderQueue() {
            return new Queue("reader.user.requested.reader", true);
        }

        @Bean
        public Queue userPendingCreatedQueue() {
            return new Queue("user.pending.created", true);
        }

        @Bean
        public Queue readerPendingCreatedQueue() {
            return new Queue("reader.pending.created", true);
        }


        // Definindo bindings para as filas

        @Bean
        public Binding readerLendingRequestBinding(DirectExchange direct,
                                                   @Qualifier("readerLendingRequestQueue") Queue readerLendingRequestQueue) {
            return BindingBuilder.bind(readerLendingRequestQueue)
                    .to(direct)
                    .with("reader.lending.requests");
        }

        @Bean
        public Binding readerLendingResponseBinding(DirectExchange direct,
                                                    @Qualifier("readerLendingResponseQueue") Queue readerLendingResponseQueue) {
            return BindingBuilder.bind(readerLendingResponseQueue)
                    .to(direct)
                    .with("reader.lending.responses");
        }

        @Bean
        public Binding requestBinding(DirectExchange direct,
                                                         @Qualifier("readerServiceInstanciatedQueue") Queue readerServiceInstanciatedQueue) {
            return BindingBuilder.bind(readerServiceInstanciatedQueue)
                    .to(direct)
                    .with("rpc.bootstrap.request");
        }

        @Bean
        public Binding replyBinding(DirectExchange direct,
                                     @Qualifier("rpcReplyQueue") Queue rpcReplyQueue) {
            return BindingBuilder.bind(rpcReplyQueue)
                    .to(direct)
                    .with("rpc.bootstrap.response");
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
        public Binding genreCreatedBinding(DirectExchange direct,
                                           @Qualifier("genreCreatedQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with(GenreEvents.GENRE_CREATED);
        }


        @Bean
        public Binding genreDeletedBinding(DirectExchange direct,
                                           @Qualifier("genreDeletedQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with(GenreEvents.GENRE_DELETED);
        }

        @Bean
        public Binding genreUpdatedBinding(DirectExchange direct,
                                           @Qualifier("genreUpdatedQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with(GenreEvents.GENRE_UPDATED);
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

        // SAGA bindings for Reader-User creation
        @Bean
        public Binding readerUserRequestedUserBinding(DirectExchange direct,
                                                      @Qualifier("readerUserRequestedUserQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with("reader.user.requested.user");
        }

        @Bean
        public Binding readerUserRequestedReaderBinding(DirectExchange direct,
                                                        @Qualifier("readerUserRequestedReaderQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with("reader.user.requested.reader");
        }

        @Bean
        public Binding userPendingCreatedBinding(DirectExchange direct,
                                                 @Qualifier("userPendingCreatedQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with("user.pending.created");
        }

        @Bean
        public Binding readerPendingCreatedBinding(DirectExchange direct,
                                                   @Qualifier("readerPendingCreatedQueue") Queue queue) {
            return BindingBuilder.bind(queue)
                    .to(direct)
                    .with("reader.pending.created");
        }

        @Bean

        public RpcBootstrapListener rpcBootstrapListener(BookService bookService, GenreService genreService, LendingService lendingService, ReaderService readerService, RpcBootstrapPublisher rpcBootstrapPublisher) {
            return new RpcBootstrapListener(readerService, bookService, genreService, lendingService, rpcBootstrapPublisher);
        }

        @Bean
        public BookEventListener bookReceiver(BookService bookService) {
            return new BookEventListener(bookService);
        }


        @Bean
        public GenreEventListener genreReceiver(GenreService genreService) {
            return new GenreEventListener(genreService);
        }


        @Bean
        public LendingEventListener lendingReceiver(LendingService lendingService) {
            return new LendingEventListener(lendingService);
        }

        @Bean
        public ReaderEventListener readerReceiver(ReaderService readerService, ReaderEventPublisher readerEventPublisher , ReaderViewAMQPMapper readerViewAMQPMapper) {
            return new ReaderEventListener(readerService, readerEventPublisher, readerViewAMQPMapper);
        }
    }

}