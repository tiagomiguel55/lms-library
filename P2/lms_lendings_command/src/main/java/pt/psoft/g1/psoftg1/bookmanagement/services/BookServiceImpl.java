package pt.psoft.g1.psoftg1.bookmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

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



		Book bookCreated = create(isbn, title, description, photoURI);

		return bookCreated;
	}

	private Book create( String isbn,
						 String title,
						 String description,
						 String photoURI) {

		if (bookRepository.findByIsbn(isbn).isPresent()) {
			throw new ConflictException("Book with ISBN " + isbn + " already exists");
		}

		Book newBook = new Book(isbn, title, description, photoURI);

		Book savedBook = bookRepository.save(newBook);

		return savedBook;
	}

	@Override
	public Book update(BookViewAMQP book) {
		return null;
	}

	@Override
	public void delete(String isbn) {
		Book book = bookRepository.findByIsbn(isbn).orElseThrow();

		bookRepository.delete(book);
	}
}
