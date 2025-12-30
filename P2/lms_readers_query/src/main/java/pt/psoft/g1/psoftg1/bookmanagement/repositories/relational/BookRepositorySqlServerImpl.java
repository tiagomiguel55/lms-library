package pt.psoft.g1.psoftg1.bookmanagement.repositories.relational;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.BookEntity;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.mappers.BookEntityMapper;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;
import pt.psoft.g1.psoftg1.genremanagement.repositories.relational.GenreRepositorySqlServer;
import pt.psoft.g1.psoftg1.lendingmanagement.model.relational.LendingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("sqlServer")
@Qualifier("bookSqlServerRepo")
@Component
public class BookRepositorySqlServerImpl implements BookRepository {

    private final BookRepositorySqlServer bookRepositorySqlServer;
    private final BookEntityMapper bookEntityMapper;

    private final GenreRepositorySqlServer genreRepository;
    private final EntityManager em;

    @Autowired
    @Lazy
    public BookRepositorySqlServerImpl(BookRepositorySqlServer bookRepositorySqlServer, BookEntityMapper bookEntityMapper, GenreRepositorySqlServer genreRepository, EntityManager em) {
        this.bookRepositorySqlServer = bookRepositorySqlServer;
        this.bookEntityMapper = bookEntityMapper;
        this.genreRepository = genreRepository;
        this.em = em;
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        Optional<BookEntity> bookEntity = bookRepositorySqlServer.findByIsbn(isbn);
        return bookEntity.map(bookEntityMapper::toModel);
    }


    @Override
    public Book save(Book book) {

        BookEntity bookEntity = bookEntityMapper.toEntity(book);

        if (bookEntity.getGenre() != null) {
            // Verifica se o gênero já existe no banco de dados pelo nome
            GenreEntity existingGenre = genreRepository.findByString(bookEntity.getGenre().getGenre()).get();
            if (existingGenre == null) {
                // Se o gênero não existe, salva o novo gênero
                existingGenre = genreRepository.save(bookEntity.getGenre());
                bookEntity.setGenre(existingGenre); // Atualiza o gênero da BookEntity com o gênero persistido
            }
            bookEntity.setGenre(existingGenre); // Atualiza o gênero da BookEntity com o gênero persistido
        }

        // Atualiza a lista de autores da BookEntity com os autores persistidos


        BookEntity savedEntity = bookRepositorySqlServer.save(bookEntity);

        return bookEntityMapper.toModel(savedEntity);

    }

    @Override
    public void delete(Book book) {

    }

    @Override
    public List<Book> findAll() {
        List<BookEntity> bookEntities = bookRepositorySqlServer.findAll();
        List<Book> books = new ArrayList<>();
        for (BookEntity bookEntity : bookEntities) {
            books.add(bookEntityMapper.toModel(bookEntity));
        }
        return books;
    }


    @Override
    public List<Book> findMostLentBooksByGenre(int maxBooks, String genre) {
        // Criação do CriteriaBuilder e CriteriaQuery
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        // Definição do Root para a entidade Lending
        Root<LendingEntity> lendingRoot = cq.from(LendingEntity.class);

        // Definição dos joins para acessar Book e Genre
        Join<LendingEntity, BookEntity> bookJoin = lendingRoot.join("book");
        Join<BookEntity, GenreEntity> genreJoin = bookJoin.join("genre");

        // Filtragem pelo gênero especificado
        cq.where(cb.equal(genreJoin.get("genre"), genre));

        // Contagem de ocorrências de empréstimos por livro
        Expression<Long> lendingCount = cb.count(lendingRoot);

        // Seleciona o livro e sua contagem de empréstimos
        cq.multiselect(bookJoin, lendingCount);

        // Agrupa pela entidade Book para contar cada livro individualmente
        cq.groupBy(bookJoin);

        // Ordena pela contagem de empréstimos em ordem decrescente
        cq.orderBy(cb.desc(lendingCount));

        // Criação e execução da consulta
        TypedQuery<Tuple> query = em.createQuery(cq);
        query.setMaxResults(maxBooks);

        // Processa os resultados e retorna uma lista de livros
        List<Tuple> results = query.getResultList();
        List<BookEntity> mostLentBooks = new ArrayList<>();

        for (Tuple result : results) {
            mostLentBooks.add(result.get(0, BookEntity.class));
        }

        List<Book> mostLentBooksModel = new ArrayList<>();

        for (BookEntity bookEntity : mostLentBooks) {
            mostLentBooksModel.add(bookEntityMapper.toModel(bookEntity));
        }

        return mostLentBooksModel;
    }


}
