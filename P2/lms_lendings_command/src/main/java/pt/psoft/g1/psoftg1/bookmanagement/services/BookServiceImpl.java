package pt.psoft.g1.psoftg1.bookmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@PropertySource({"classpath:config/library.properties"})
public class BookServiceImpl implements BookService {

	private final BookRepository bookRepository;


	@Override
	public Book create(BookViewAMQP book) {
		final String isbn = book.getIsbn();
		final String description = book.getDescription();
		final String title = book.getTitle();
		final String photoURI = null;

		// Check if book already exists (could be a placeholder)
		Optional<Book> existingBook = bookRepository.findByIsbn(isbn);

		if (existingBook.isPresent()) {
			Book existing = existingBook.get();
			// Check if it's a placeholder - if so, update it with real data
			if (existing.getTitle().toString().equals("Pending Validation")) {
				System.out.println(" [BOOKS] 🔄 Updating placeholder book with real data for ISBN: " + isbn);
				return updateBookData(existing, title, description, photoURI);
			} else {
				System.out.println(" [BOOKS] ⚠️ Book already exists with real data for ISBN: " + isbn);
				throw new ConflictException("Book with ISBN " + isbn + " already exists");
			}
		}

		Book bookCreated = create(isbn, title, description, photoURI);

		return bookCreated;
	}

	private Book create( String isbn,
						 String title,
						 String description,
						 String photoURI) {

		Book newBook = new Book(isbn, title, description, photoURI);
		Book savedBook = bookRepository.save(newBook);
		System.out.println(" [BOOKS] ✅ New book created: " + title);
		return savedBook;
	}

	@Override
	public Book update(BookViewAMQP book) {
		final String isbn = book.getIsbn();
		final String description = book.getDescription();
		final String title = book.getTitle();
		final String photoURI = null;

		Book existingBook = bookRepository.findByIsbn(isbn)
				.orElseThrow(() -> new NotFoundException("Book with ISBN " + isbn + " not found"));

		System.out.println(" [BOOKS] 📝 Updating book: " + isbn);

		return updateBookData(existingBook, title, description, photoURI);
	}

	private Book updateBookData(Book book, String title, String description, String photoURI) {
		// Use reflection or create new book to update immutable fields
		// Since Book fields are private with setters, we need to create a new instance
		Book updatedBook = new Book(book.getIsbn(), title, description, photoURI);
		updatedBook.setVersion(book.getVersion());

		// Delete old and save new (workaround for immutable fields)
		bookRepository.delete(book);
		Book saved = bookRepository.save(updatedBook);

		System.out.println(" [BOOKS] ✅ Book updated successfully: " + title);
		return saved;
	}

	@Override
	public void delete(String isbn) {
		Book book = bookRepository.findByIsbn(isbn).orElseThrow();

		bookRepository.delete(book);
	}
}
