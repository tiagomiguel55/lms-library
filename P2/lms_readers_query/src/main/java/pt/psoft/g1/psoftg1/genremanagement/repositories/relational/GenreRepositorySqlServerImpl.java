package pt.psoft.g1.psoftg1.genremanagement.repositories.relational;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.bookmanagement.model.relational.BookEntity;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.genremanagement.repositories.mappers.GenreEntityMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.model.relational.LendingEntity;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("sqlServer")
@Qualifier("genreSqlServerRepo")
@Component
public class GenreRepositorySqlServerImpl implements GenreRepository {

    private final GenreRepositorySqlServer genreRepositorySqlServer;
    private final GenreEntityMapper genreEntityMapper;
    private final EntityManager entityManager;

    @Autowired
    @Lazy
    public GenreRepositorySqlServerImpl(GenreRepositorySqlServer genreRepositorySqlServer, GenreEntityMapper genreEntityMapper, EntityManager entityManager) {
        this.genreRepositorySqlServer = genreRepositorySqlServer;
        this.genreEntityMapper = genreEntityMapper;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Genre> findByName(String name) {
        Optional<GenreEntity> genreEntity = genreRepositorySqlServer.findByString(name);
        if (genreEntity.isEmpty()) {
            return Optional.empty();
        }else return Optional.of(genreEntityMapper.toModel(genreEntity.get()));
    }


    @Override
    public Genre save(Genre genre) {

        return genreEntityMapper.toModel(genreRepositorySqlServer.save(genreEntityMapper.toEntity(genre)));

    }
    @Override
    public void delete(Genre genre) {
        genreRepositorySqlServer.delete(genreEntityMapper.toEntity(genre));
    }

    @Override
    public List<Genre> findAll() {
        List<GenreEntity> genreEntities = genreRepositorySqlServer.findAll();
        List<Genre> genres = new ArrayList<>();
        for (GenreEntity genreEntity : genreEntities) {
            genres.add(genreEntityMapper.toModel(genreEntity));
        }
        return genres;
    }

    @Override
   public List<String> getMostLentGenres(int maxGenres) {
       //use entitymanager to create a query

       CriteriaBuilder cb = entityManager.getCriteriaBuilder();
       CriteriaQuery<Tuple> cq = cb.createTupleQuery();
       Root<LendingEntity> lendingRoot = cq.from(LendingEntity.class);
       Join<LendingEntity, BookEntity> bookJoin = lendingRoot.join("book");
       Join<BookEntity, GenreEntity> genreJoin = bookJoin.join("genre");

       Expression<Long> lendingCount = cb.count(lendingRoot);

       cq.multiselect(genreJoin.get("genre"), lendingCount);

       cq.groupBy(genreJoin.get("genre"));

       cq.orderBy(cb.desc(lendingCount));

       TypedQuery<Tuple> query = entityManager.createQuery(cq);

       query.setMaxResults(maxGenres);

       List<Tuple> results = query.getResultList();

       List<String> genres = new ArrayList<>();

       for (Tuple result : results) {
           genres.add(result.get(0, String.class));
       }

       return genres;
   }

   @Override
   public String getMostLentGenreByReader(String readerNumber){
       CriteriaBuilder cb = entityManager.getCriteriaBuilder();
       CriteriaQuery<Tuple> cq = cb.createTupleQuery();
       Root<LendingEntity> lendingRoot = cq.from(LendingEntity.class);
       Join <LendingEntity, ReaderDetailsEntity> readerJoin = lendingRoot.join("readerDetails");
       Join<LendingEntity, BookEntity> bookJoin = lendingRoot.join("book");
       Join<BookEntity, GenreEntity> genreJoin = bookJoin.join("genre");

       Expression<Long> lendingCount = cb.count(lendingRoot);

       cq.multiselect(genreJoin.get("genre"), lendingCount);

       cq.where(cb.equal(readerJoin.get("readerNumber").get("readerNumber"), readerNumber));

       cq.groupBy(genreJoin.get("genre"));

       cq.orderBy(cb.desc(lendingCount));

       TypedQuery<Tuple> query = entityManager.createQuery(cq);

       query.setMaxResults(1);

       List<Tuple> results = query.getResultList();

       if (results.isEmpty()) {
           return null;
       }

       return results.get(0).get(0, String.class);
   }

}
