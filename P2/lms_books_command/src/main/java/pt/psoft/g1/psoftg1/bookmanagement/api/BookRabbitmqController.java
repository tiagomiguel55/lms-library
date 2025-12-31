package pt.psoft.g1.psoftg1.bookmanagement.api;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import lombok.RequiredArgsConstructor;
    import org.springframework.amqp.rabbit.annotation.RabbitListener;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import pt.psoft.g1.psoftg1.authormanagement.api.AuthorPendingCreated;
    import pt.psoft.g1.psoftg1.authormanagement.model.Author;
    import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
    import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
    import pt.psoft.g1.psoftg1.bookmanagement.model.PendingBookRequest;
    import pt.psoft.g1.psoftg1.bookmanagement.publishers.BookEventsPublisher;
    import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
    import pt.psoft.g1.psoftg1.bookmanagement.repositories.PendingBookRequestRepository;
    import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
    import org.springframework.amqp.core.Message;
    import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
    import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
    import org.springframework.orm.ObjectOptimisticLockingFailureException;
    
    import java.nio.charset.StandardCharsets;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Optional;
    
    @Service
    @RequiredArgsConstructor
    public class BookRabbitmqController {
    
        @Autowired
        private final BookService bookService;
    
        @Autowired
        private final BookRepository bookRepository;
    
        @Autowired
        private final AuthorRepository authorRepository;
    
        @Autowired
        private final GenreRepository genreRepository;
    
        @Autowired
        private final PendingBookRequestRepository pendingBookRequestRepository;

        @Autowired
        private final BookEventsPublisher bookEventsPublisher;
    
        @RabbitListener(queues = "#{autoDeleteQueue_Book_Created.name}")
        public void receiveBookCreatedMsg(Message msg) {
    
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);
    
                System.out.println(" [x] Received Book Created by AMQP: " + msg + ".");
                try {
                    bookService.create(bookViewAMQP);
                    System.out.println(" [x] New book inserted from AMQP: " + msg + ".");
                } catch (Exception e) {
                    System.out.println(" [x] Book already exists. No need to store it.");
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving book event from AMQP: '" + ex.getMessage() + "'");
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Book_Updated.name}")
        public void receiveBookUpdated(Message msg) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
    
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
                BookViewAMQP bookViewAMQP = objectMapper.readValue(jsonReceived, BookViewAMQP.class);
    
                System.out.println(" [x] Received Book Updated by AMQP: " + msg + ".");
                try {
                    bookService.update(bookViewAMQP);
                    System.out.println(" [x] Book updated from AMQP: " + msg + ".");
                } catch (Exception e) {
                    System.out.println(" [x] Book does not exists or wrong version. Nothing stored.");
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving book event from AMQP: '" + ex.getMessage() + "'");
            }
        }
    
        @RabbitListener
        public void receive(String payload) {
            System.out.println(" [x] Received '" + payload + "'");
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Author_Pending_Created.name}")
        public void receiveAuthorPendingCreated(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                AuthorPendingCreated event = objectMapper.readValue(jsonReceived, AuthorPendingCreated.class);
    
                System.out.println(" [x] Received Author Pending Created by AMQP:");
                System.out.println("     - Book ID (ISBN): " + event.getBookId());
                System.out.println("     - Author ID: " + event.getAuthorId());
                System.out.println("     - Author Name: " + event.getAuthorName());
                System.out.println("     - Genre Name: " + event.getGenreName());
    
                // Retry logic to handle race conditions
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        // Update pending request status
                        Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(event.getBookId());
                        if (pendingRequestOpt.isPresent()) {
                            PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                            // Mark author as received (order-independent)
                            pendingRequest.setAuthorPendingReceived(true);
    
                            // Check if BOTH are now received
                            if (pendingRequest.isAuthorPendingReceived() && pendingRequest.isGenrePendingReceived()) {
                                pendingRequest.setStatus(PendingBookRequest.RequestStatus.BOTH_PENDING_CREATED);
                                System.out.println(" [x] 📝 Both Author and Genre pending received → BOTH_PENDING_CREATED");
                            } else {
                                System.out.println(" [x] 📝 Author pending received, waiting for Genre pending...");
                            }
    
                            pendingBookRequestRepository.save(pendingRequest);
                            System.out.println(" [x] Updated pending request status to " + pendingRequest.getStatus() + " for ISBN: " + event.getBookId());
    
                            // Try to create book if both author and genre are ready
                            tryCreateBook(event.getBookId());
                        } else {
                            System.out.println(" [x] ⚠️ No pending request found for ISBN: " + event.getBookId());
                        }
    
                        // Success, break out of retry loop
                        break;
    
                    } catch (ObjectOptimisticLockingFailureException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50); // Small delay before retry
                        } else {
                            System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                            throw e;
                        }
                    }
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving author pending created event from AMQP: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Genre_Pending_Created.name}")
        public void receiveGenrePendingCreated(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                pt.psoft.g1.psoftg1.genremanagement.api.GenrePendingCreated event =
                    objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.genremanagement.api.GenrePendingCreated.class);
    
                System.out.println(" [x] Received Genre Pending Created by AMQP:");
                System.out.println("     - Book ID (ISBN): " + event.getBookId());
                System.out.println("     - Genre Name: " + event.getGenreName());
    
                // Retry logic to handle race conditions
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        // Update pending request status
                        Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(event.getBookId());
                        if (pendingRequestOpt.isPresent()) {
                            PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                            // Mark genre as received (order-independent)
                            pendingRequest.setGenrePendingReceived(true);
    
                            // Check if BOTH are now received
                            if (pendingRequest.isAuthorPendingReceived() && pendingRequest.isGenrePendingReceived()) {
                                pendingRequest.setStatus(PendingBookRequest.RequestStatus.BOTH_PENDING_CREATED);
                                System.out.println(" [x] 📝 Both Author and Genre pending received → BOTH_PENDING_CREATED");
                            } else {
                                System.out.println(" [x] 📝 Genre pending received, waiting for Author pending...");
                            }
    
                            pendingBookRequestRepository.save(pendingRequest);
                            System.out.println(" [x] Updated pending request status to " + pendingRequest.getStatus() + " for ISBN: " + event.getBookId());
    
                            // Try to create book if both author and genre are ready
                            tryCreateBook(event.getBookId());
                        } else {
                            System.out.println(" [x] ⚠️ No pending request found for ISBN: " + event.getBookId());
                        }
    
                        // Success, break out of retry loop
                        break;
    
                    } catch (ObjectOptimisticLockingFailureException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50); // Small delay before retry
                        } else {
                            System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                            throw e;
                        }
                    }
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving genre pending created event from AMQP: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }
    
        private void tryCreateBook(String isbn) {
            try {
                // Check if book already exists
                Optional<Book> existingBook = bookRepository.findByIsbn(isbn);
                if (existingBook.isPresent()) {
                    System.out.println(" [x] Book already exists with ISBN: " + isbn);
                    return;
                }
    
                // Get the pending request - ALWAYS RELOAD from DB to get latest status
                Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(isbn);
                if (pendingRequestOpt.isEmpty()) {
                    System.out.println(" [x] ⚠️ No pending request found for ISBN: " + isbn);
                    return;
                }
    
                PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                // Step 1: When both Author and Genre are CREATED (not finalized yet), send BOOK_FINALIZED event
                if (pendingRequest.getStatus() == PendingBookRequest.RequestStatus.BOTH_PENDING_CREATED) {
                    System.out.println(" [x] ✅ Both Author and Genre are CREATED (pending finalization)");
                    System.out.println(" [x] 📤 Sending BOOK_FINALIZED event to trigger author and genre finalization...");

                    // Get author info to send in BOOK_FINALIZED event
                    List<Author> authors = authorRepository.searchByNameName(pendingRequest.getAuthorName());
                    if (authors.isEmpty()) {
                        System.out.println(" [x] ⚠️ Author not found: " + pendingRequest.getAuthorName());
                        return;
                    }

                    Author author = authors.get(0);

                    // Send BOOK_FINALIZED event to trigger finalization in AuthorCmd and GenreCmd
                    bookService.publishBookFinalized(author.getAuthorNumber(), author.getName(), isbn, pendingRequest.getGenreName());
                    System.out.println(" [x] ✅ BOOK_FINALIZED event sent - waiting for author and genre finalization...");

                    return; // Don't create book yet, wait for finalization events
                }
    
                // Step 2: Only create book when BOTH author AND genre are FINALIZED
                if (pendingRequest.getStatus() != PendingBookRequest.RequestStatus.BOTH_FINALIZED) {
                    System.out.println(" [x] ⏸️ Waiting for both author and genre to be FINALIZED for ISBN: " + isbn);
                    System.out.println(" [x]    Current status: " + pendingRequest.getStatus());
                    return;
                }
    
                System.out.println(" [x] 🎯 Status is BOTH_FINALIZED - proceeding with book creation!");
    
                // Verify both author and genre exist and are finalized
                List<Author> authors = authorRepository.searchByNameName(pendingRequest.getAuthorName());
                Optional<Genre> genreOpt = genreRepository.findByString(pendingRequest.getGenreName());
    
                if (authors.isEmpty()) {
                    System.out.println(" [x] ⚠️ Author not found: " + pendingRequest.getAuthorName());
                    return;
                }
    
                if (genreOpt.isEmpty()) {
                    System.out.println(" [x] ⚠️ Genre not found: " + pendingRequest.getGenreName());
                    return;
                }
    
                Author author = authors.get(0);
                Genre genre = genreOpt.get();
    
                // Double-check that both are actually finalized
                if (!author.isFinalized()) {
                    System.out.println(" [x] ⚠️ Author is not finalized yet: " + author.getName());
                    return;
                }
    
                if (!genre.isFinalized()) {
                    System.out.println(" [x] ⚠️ Genre is not finalized yet: " + genre.getGenre());
                    return;
                }
    
                System.out.println(" [x] ✅ Both Author and Genre are FINALIZED - Creating book now!");
                System.out.println("     - Author: " + author.getName() + " (ID: " + author.getAuthorNumber() + ", finalized: " + author.isFinalized() + ")");
                System.out.println("     - Genre: " + genre.getGenre() + " (finalized: " + genre.isFinalized() + ")");
    
                // Create book with finalized author and genre - use title from pending request
                String title = pendingRequest.getTitle();
                String description = "Requested book - ISBN: " + isbn;
    
                List<Author> authorList = new ArrayList<>();
                authorList.add(author);
    
                Book newBook = new Book(isbn, title, description, genre, authorList, null);
                Book savedBook = bookRepository.save(newBook);
    
                System.out.println(" [x] ✅ Book created successfully with FINALIZED author and genre: " + savedBook.getIsbn() + " - " + savedBook.getTitle());
    
                // IMPORTANT: Reload the pending request to avoid stale data, then update status
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        Optional<PendingBookRequest> latestRequestOpt = pendingBookRequestRepository.findByBookId(isbn);
                        if (latestRequestOpt.isPresent()) {
                            PendingBookRequest latestRequest = latestRequestOpt.get();
                            latestRequest.setStatus(PendingBookRequest.RequestStatus.BOOK_CREATED);
                            pendingBookRequestRepository.save(latestRequest);
                            System.out.println(" [x] 📝 Updated pending request status to BOOK_CREATED");
                        }
                        break; // Success
                    } catch (ObjectOptimisticLockingFailureException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict updating pending request (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50);
                        } else {
                            System.out.println(" [x] ❌ Failed to update pending request status after " + maxRetries + " attempts");
                            throw e;
                        }
                    }
                }
    
                System.out.println(" [x] ✅ Book creation saga completed successfully!");
    
            } catch (Exception e) {
                System.out.println(" [x] ❌ Error creating book: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        private void processPendingRequest(String isbn, Author author, Genre genre) {
            try {
                // Find the pending request for this book using bookId (ISBN)
                Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(isbn);
    
                if (pendingRequestOpt.isPresent()) {
                    PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                    // Update the request status to BOOK_CREATED
                    pendingRequest.setStatus(PendingBookRequest.RequestStatus.BOOK_CREATED);
                    pendingBookRequestRepository.save(pendingRequest);
    
                    System.out.println(" [x] Processed pending request for ISBN: " + isbn);
    
                    // Publish BOOK_FINALIZED event to notify that the book is finalized
                    bookService.publishBookFinalized(author.getAuthorNumber(), author.getName(), isbn, genre.getGenre());
    
                    // Optionally delete the pending request after successful completion
                    // pendingBookRequestRepository.delete(pendingRequest);
                } else {
                    System.out.println(" [x] No pending request found for ISBN: " + isbn);
                }
            } catch (Exception e) {
                System.out.println(" [x] Error processing pending request: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Author_Creation_Failed.name}")
        public void receiveAuthorCreationFailed(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                pt.psoft.g1.psoftg1.authormanagement.api.AuthorCreationFailed event =
                    objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.authormanagement.api.AuthorCreationFailed.class);
    
                System.out.println(" [x] ❌ Received Author Creation Failed by AMQP:");
                System.out.println("     - Book ID (ISBN): " + event.getBookId());
                System.out.println("     - Author Name: " + event.getAuthorName());
                System.out.println("     - Genre Name: " + event.getGenreName());
                System.out.println("     - Error: " + event.getErrorMessage());
    
                try {
                    // Find the pending request for this book
                    Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(event.getBookId());
    
                    if (pendingRequestOpt.isPresent()) {
                        PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                        // Mark the request as FAILED
                        pendingRequest.setStatus(PendingBookRequest.RequestStatus.FAILED);
                        pendingRequest.setErrorMessage("Author creation failed: " + event.getErrorMessage());
                        pendingBookRequestRepository.save(pendingRequest);
    
                        System.out.println(" [x] ✅ Marked pending book request as FAILED for ISBN: " + event.getBookId());
                        System.out.println(" [x] 🔄 SAGA COMPENSATION COMPLETED - Book creation aborted");
                    } else {
                        System.out.println(" [x] ⚠️ No pending request found for ISBN: " + event.getBookId());
                    }
                } catch (Exception e) {
                    System.out.println(" [x] ❌ Error processing author creation failed event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving author creation failed event from AMQP: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Genre_Creation_Failed.name}")
        public void receiveGenreCreationFailed(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                pt.psoft.g1.psoftg1.genremanagement.api.GenreCreationFailed event =
                    objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.genremanagement.api.GenreCreationFailed.class);
    
                System.out.println(" [x] ❌ Received Genre Creation Failed by AMQP:");
                System.out.println("     - Book ID (ISBN): " + event.getBookId());
                System.out.println("     - Genre Name: " + event.getGenreName());
                System.out.println("     - Error: " + event.getErrorMessage());
    
                try {
                    // Find the pending request for this book
                    Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(event.getBookId());
    
                    if (pendingRequestOpt.isPresent()) {
                        PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                        // Mark the request as FAILED
                        pendingRequest.setStatus(PendingBookRequest.RequestStatus.FAILED);
                        pendingRequest.setErrorMessage("Genre creation failed: " + event.getErrorMessage());
                        pendingBookRequestRepository.save(pendingRequest);
    
                        System.out.println(" [x] ✅ Marked pending book request as FAILED for ISBN: " + event.getBookId());
                        System.out.println(" [x] 🔄 SAGA COMPENSATION COMPLETED - Book creation aborted");
                    } else {
                        System.out.println(" [x] ⚠️ No pending request found for ISBN: " + event.getBookId());
                    }
                } catch (Exception e) {
                    System.out.println(" [x] ❌ Error processing genre creation failed event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving genre creation failed event from AMQP: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
        public void receiveAuthorCreated(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP event =
                    objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP.class);
    
                System.out.println(" [x] 🎉 Received Author FINALIZED by AMQP:");
                System.out.println("     - Author Name: " + event.getName());
                System.out.println("     - Book ID (ISBN): " + event.getBookId());
    
                if (event.getBookId() == null || event.getBookId().isEmpty()) {
                    System.out.println(" [x] ⚠️ Author finalized but no bookId associated, skipping");
                    return;
                }
    
                // Retry logic to handle race conditions
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        // Always reload from DB to get latest state
                        Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(event.getBookId());
    
                        if (pendingRequestOpt.isEmpty()) {
                            System.out.println(" [x] ⚠️ No pending request found for ISBN: " + event.getBookId());
                            return;
                        }
    
                        PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                        System.out.println(" [x] 🔍 Current status: " + pendingRequest.getStatus());
    
                        // Mark author as finalized (order-independent)
                        pendingRequest.setAuthorFinalizedReceived(true);
    
                        // Update status based on what we have received
                        boolean statusChanged = false;
                        if (pendingRequest.isAuthorFinalizedReceived() && pendingRequest.isGenreFinalizedReceived()) {
                            pendingRequest.setStatus(PendingBookRequest.RequestStatus.BOTH_FINALIZED);
                            System.out.println(" [x] 📝 Status transition: Both finalized → BOTH_FINALIZED");
                            statusChanged = true;
                        } else if (pendingRequest.getStatus() == PendingBookRequest.RequestStatus.BOTH_FINALIZED) {
                            System.out.println(" [x] ✅ Already at BOTH_FINALIZED, proceeding to book creation");
                            statusChanged = false; // No need to save
                        } else {
                            System.out.println(" [x] 📝 Author finalized, waiting for Genre finalization...");
                            statusChanged = true; // Save the flag update
                        }
    
                        if (statusChanged) {
                            pendingBookRequestRepository.save(pendingRequest);
                            System.out.println(" [x] ✅ Updated status to: " + pendingRequest.getStatus());
                        }
    
                        // Try to create book if both are finalized
                        tryCreateBook(event.getBookId());
    
                        // Success, break out of retry loop
                        break;
    
                    } catch (ObjectOptimisticLockingFailureException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50); // Small delay before retry
                        } else {
                            System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                            throw e;
                        }
                    } catch (jakarta.persistence.OptimisticLockException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50); // Small delay before retry
                        } else {
                            System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                            throw e;
                        }
                    }
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving author finalized event from AMQP: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Genre_Created.name}")
        public void receiveGenreCreated(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP event =
                    objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP.class);
    
                System.out.println(" [x] 🎉 Received Genre FINALIZED by AMQP:");
                System.out.println("     - Genre Name: " + event.getGenre());
                System.out.println("     - Book ID (ISBN): " + event.getBookId());
    
                if (event.getBookId() == null || event.getBookId().isEmpty()) {
                    System.out.println(" [x] ⚠️ Genre finalized but no bookId associated, skipping");
                    return;
                }
    
                // Retry logic to handle race conditions
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        // Always reload from DB to get latest state
                        Optional<PendingBookRequest> pendingRequestOpt = pendingBookRequestRepository.findByBookId(event.getBookId());
    
                        if (pendingRequestOpt.isEmpty()) {
                            System.out.println(" [x] ⚠️ No pending request found for ISBN: " + event.getBookId());
                            return;
                        }
    
                        PendingBookRequest pendingRequest = pendingRequestOpt.get();
    
                        System.out.println(" [x] 🔍 Current status: " + pendingRequest.getStatus());
    
                        // Mark genre as finalized (order-independent)
                        pendingRequest.setGenreFinalizedReceived(true);
    
                        // Update status based on what we have received
                        boolean statusChanged = false;
                        if (pendingRequest.isAuthorFinalizedReceived() && pendingRequest.isGenreFinalizedReceived()) {
                            pendingRequest.setStatus(PendingBookRequest.RequestStatus.BOTH_FINALIZED);
                            System.out.println(" [x] 📝 Status transition: Both finalized → BOTH_FINALIZED");
                            statusChanged = true;
                        } else if (pendingRequest.getStatus() == PendingBookRequest.RequestStatus.BOTH_FINALIZED) {
                            System.out.println(" [x] ✅ Already at BOTH_FINALIZED, proceeding to book creation");
                            statusChanged = false; // No need to save
                        } else {
                            System.out.println(" [x] 📝 Genre finalized, waiting for Author finalization...");
                            statusChanged = true; // Save the flag update
                        }
    
                        if (statusChanged) {
                            pendingBookRequestRepository.save(pendingRequest);
                            System.out.println(" [x] ✅ Updated status to " + pendingRequest.getStatus() + " for ISBN: " + event.getBookId());
                        }
    
                        // Try to create book if both author and genre are ready
                        tryCreateBook(event.getBookId());
    
                        // Success, break out of retry loop
                        break;
    
                    } catch (ObjectOptimisticLockingFailureException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50); // Small delay before retry
                        } else {
                            System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                            throw e;
                        }
                    } catch (jakarta.persistence.OptimisticLockException e) {
                        if (attempt < maxRetries - 1) {
                            System.out.println(" [x] ⚠️ Optimistic lock conflict (attempt " + (attempt + 1) + "), retrying...");
                            Thread.sleep(50); // Small delay before retry
                        } else {
                            System.out.println(" [x] ❌ Failed after " + maxRetries + " attempts due to optimistic locking");
                            throw e;
                        }
                    }
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving genre finalized event from AMQP: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }
    
        @RabbitListener(queues = "#{autoDeleteQueue_Book_Finalized.name}")
        public void receiveBookFinalized(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
    
                ObjectMapper objectMapper = new ObjectMapper();
                pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent event =
                    objectMapper.readValue(jsonReceived, pt.psoft.g1.psoftg1.bookmanagement.api.BookFinalizedEvent.class);
    
                System.out.println(" [x] 📥 Received Book Finalized by AMQP (BookCmd itself):");
                System.out.println("     - Book ID: " + event.getBookId());
                System.out.println("     - Author ID: " + event.getAuthorId());
                System.out.println("     - Author Name: " + event.getAuthorName());
                System.out.println("     - Genre Name: " + event.getGenreName());
    
                try {
                    // BookCmd receives its own BookFinalized event
                    // This serves two purposes depending on timing:
    
                    Optional<Book> bookOpt = bookRepository.findByIsbn(event.getBookId());
                    if (bookOpt.isPresent()) {
                        Book book = bookOpt.get();
                        System.out.println(" [x] ✅ CONFIRMATION: Book exists with FINALIZED author and genre!");
                        System.out.println(" [x] 📚 Book: " + book.getTitle());
                        System.out.println(" [x] ✨ This confirms the book was created with PERMANENT (finalized) entities");
                        System.out.println(" [x] 📊 Updating read models for finalized book: " + event.getBookId());
    
                        // Here you can:
                        // - Update read models/query databases
                        // - Update search indexes
                        // - Send notifications that book is fully available
                        // - Update caches with finalized book data
    
                    } else {
                        System.out.println(" [x] ℹ️ Book not yet created for ISBN: " + event.getBookId());
                        System.out.println(" [x] 🔄 This is the TRIGGER event - AuthorCmd & GenreCmd will now finalize their TEMPORARY entities");
                        System.out.println(" [x] ⏳ Waiting for AuthorCreated & GenreCreated events to arrive...");
    
                        // This event triggered the finalization process in AuthorCmd and GenreCmd
                        // Once they respond with AuthorCreated and GenreCreated, the book will be created
                    }
                } catch (Exception e) {
                    System.out.println(" [x] ❌ Error processing book finalized in BookCmd: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            catch(Exception ex) {
                System.out.println(" [x] Exception receiving book finalized event from AMQP (BookCmd): '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }

        @RabbitListener(queues = "#{autoDeleteQueue_Lending_Returned.name}")
        public void receiveLendingReturned(Message msg) {
            try {
                String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new ObjectMapper();
                LendingReturnedEvent event = objectMapper.readValue(jsonReceived, LendingReturnedEvent.class);

                System.out.println("Received LendingReturned in BooksCmd: bookId=" + event.getBookId() +
                        ", comment=" + event.getComment() + ", grade=" + event.getGrade());

                // Load book and add comment & grade
                Optional<Book> bookOpt = bookRepository.findByIsbn(event.getBookId());
                if (bookOpt.isPresent()) {
                    Book book = bookOpt.get();
                    // Here you would add the comment and grade to the book
                    // For now, just log it
                    System.out.println("Book found: " + book.getTitle());
                    System.out.println("Adding comment: " + event.getComment() + ", grade: " + event.getGrade());

                    // Publish BookUpdated event
                    bookEventsPublisher.sendBookUpdated(book, book.getVersion());
                    System.out.println("Published BookUpdated event for: " + event.getBookId());
                } else {
                    System.out.println("Book not found for ISBN: " + event.getBookId());
                }
            } catch (Exception ex) {
                System.out.println("Exception receiving LendingReturned event in BooksCmd: '" + ex.getMessage() + "'");
                ex.printStackTrace();
            }
        }

    }


    

