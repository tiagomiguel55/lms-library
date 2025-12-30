package pt.psoft.g1.psoftg1.bookmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQPMapper;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@PropertySource({"classpath:config/library.properties"})
public class BookServiceImpl implements BookService {


	private final BookRepository bookRepository;
	private final GenreRepository genreRepository;
	private final PhotoRepository photoRepository;
	private final BookViewAMQPMapper bookViewAMQPMapper;

	@Value("${suggestionsLimitPerGenre}")
	private long suggestionsLimitPerGenre;




	@Override
	public Book create(BookViewAMQP bookViewAMQP) {

		final String isbn = bookViewAMQP.getIsbn();
		final String description = bookViewAMQP.getDescription();
		final String title = bookViewAMQP.getTitle();
		final String photoURI = null;
		final String genre = bookViewAMQP.getGenre();


		Book bookCreated = create(isbn, title, description, photoURI, genre);

		return bookCreated;
	}

	private Book create( String isbn,
						 String title,
						 String description,
						 String photoURI,
						 String genreName) {

		if (bookRepository.findByIsbn(isbn).isPresent()) {
			throw new ConflictException("Book with ISBN " + isbn + " already exists");
		}


		final Genre genre = genreRepository.findByName(String.valueOf(genreName))
				.orElseThrow(() -> new NotFoundException("Genre not found"));

		Book newBook = new Book(isbn, title, description, genre, photoURI);

		Book savedBook = bookRepository.save(newBook);

		return savedBook;
	}


	@Override
	public Book update(BookViewAMQP bookViewAMQP) {

		final String version = bookViewAMQP.getVersion();
		final String isbn = bookViewAMQP.getIsbn();
		final String description = bookViewAMQP.getDescription();
		final String title = bookViewAMQP.getTitle();
		final String photoURI = null;
		final String genre = bookViewAMQP.getGenre();

		var book = findByIsbn(isbn);

		Book bookUpdated = update(book, version, title, description, photoURI, genre);

		return bookUpdated;
	}

	@Override
	public void delete(BookViewAMQP bookViewAMQP) {

	}

	private Book update( Book book,
						 String currentVersion,
						 String title,
						 String description,
						 String photoURI,
						 String genreId) {

		Genre genreObj = null;
		if (genreId != null) {
			Optional<Genre> genre = genreRepository.findByName(genreId);
			if (genre.isEmpty()) {
				throw new NotFoundException("Genre not found");
			}
			genreObj = genre.get();
		}

		book.applyPatch(Long.parseLong(currentVersion), title, description, photoURI, genreObj);

		Book updatedBook = bookRepository.save(book);

		return updatedBook;
	}

	@Override
	public Book findByIsbn(String isbn) {
		return this.bookRepository.findByIsbn(isbn)
				.orElseThrow(() -> new NotFoundException(Book.class, isbn));
	}

	@Override
	public List<BookViewAMQP> getAllBooks() {
		List<Book> books = bookRepository.findAll();

		List<BookViewAMQP> bookViewAMQPS = new ArrayList<>();

		for (Book book : books) {
			bookViewAMQPS.add(bookViewAMQPMapper.toBookViewAMQP(book));
		}

		return bookViewAMQPS;
	}
}
