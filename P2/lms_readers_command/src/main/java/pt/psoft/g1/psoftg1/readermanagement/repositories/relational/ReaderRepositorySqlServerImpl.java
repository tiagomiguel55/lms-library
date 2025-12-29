package pt.psoft.g1.psoftg1.readermanagement.repositories.relational;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;
import pt.psoft.g1.psoftg1.genremanagement.repositories.mappers.GenreEntityMapper;
import pt.psoft.g1.psoftg1.genremanagement.repositories.relational.GenreRepositorySqlServer;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.mappers.ReaderEntityMapper;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.ReaderEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.UserEntity;
import pt.psoft.g1.psoftg1.usermanagement.repositories.mappers.UserEntityMapper;
import pt.psoft.g1.psoftg1.usermanagement.repositories.relational.UserRepositorySqlServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Profile("sqlServer")
@Qualifier("readerSqlServerRepo")
@Component
public class ReaderRepositorySqlServerImpl implements ReaderRepository {

    private final ReaderRepositorySqlServer readerRepostorySqlServer;

    private final UserRepositorySqlServer userRepositorySqlServer;

    private final UserEntityMapper userEntityMapper;
    private final ReaderEntityMapper readerEntityMapper;

    @PersistenceContext
    private final EntityManager em;
    private final GenreEntityMapper genreEntityMapper;

    private final GenreRepositorySqlServer genreRepostorySqlServer;

    @Autowired
    @Lazy
    public ReaderRepositorySqlServerImpl(ReaderRepositorySqlServer readerRepostorySqlServer, UserRepositorySqlServer userRepositorySqlServer, UserEntityMapper userEntityMapper, ReaderEntityMapper readerEntityMapper, EntityManager entityManager, GenreEntityMapper genreEntityMapper, GenreRepositorySqlServer genreRepostorySqlServer) {
        this.readerRepostorySqlServer = readerRepostorySqlServer;
        this.userRepositorySqlServer = userRepositorySqlServer;
        this.userEntityMapper = userEntityMapper;
        this.readerEntityMapper = readerEntityMapper;
        this.em = entityManager;
        this.genreEntityMapper = genreEntityMapper;
        this.genreRepostorySqlServer = genreRepostorySqlServer;
    }


    @Override
    public Optional<ReaderDetails> findByReaderNumber(String readerNumber) {
        if (readerRepostorySqlServer.findByReaderNumber(readerNumber).isEmpty()) {
            return Optional.empty();
        }else {
            ReaderDetailsEntity readerDetailsEntity = readerRepostorySqlServer.findByReaderNumber(readerNumber).get();
            //System.out.println(readerDetailsEntity.getReader());
            return Optional.of(readerEntityMapper.toModel(readerDetailsEntity));
        }
    }

    @Override
    public List<ReaderDetails> findByPhoneNumber(String phoneNumber) {
        List<ReaderDetails> readerDetails = new ArrayList<>();

        for (ReaderDetailsEntity readerDetail : readerRepostorySqlServer.findByPhoneNumber(phoneNumber)) {
            readerDetails.add(readerEntityMapper.toModel(readerDetail));
        }
        return readerDetails;
    }

    @Override
    public Optional<ReaderDetails> findByUsername(String username) {
        return Optional.of(readerEntityMapper.toModel(readerRepostorySqlServer.findByUsername(username).get()));
    }

    @Override
    public Optional<ReaderDetails> findByUserId(String userId) {
        return Optional.of(readerEntityMapper.toModel(readerRepostorySqlServer.findByUserId(userId).get()));
    }

    @Override
    public int getCountFromCurrentYear() {
        return readerRepostorySqlServer.getCountFromCurrentYear();
    }

    @Override
    @Transactional
    public ReaderDetails save(ReaderDetails readerDetails) {
        ReaderDetailsEntity readerDetailsEntity = readerEntityMapper.toEntity(readerDetails);

        if (readerDetails.getInterestList() != null) {
            List<GenreEntity> updatedInterestList = new ArrayList<>();

            for (GenreEntity genre : readerDetailsEntity.getInterestList()) {
                // Verifica se o gênero já existe no banco
                Optional<GenreEntity> genreEntityOpt = genreRepostorySqlServer.findByString(genre.getGenre());
                if (genreEntityOpt.isPresent()) {
                    updatedInterestList.add(genreEntityOpt.get());
                } else {
                    // Salva somente se não existir
                    GenreEntity genreEntitySaved = genreRepostorySqlServer.save(genre);
                    updatedInterestList.add(genreEntitySaved);
                }
            }

            // Atualiza a lista com entidades gerenciadas
            readerDetailsEntity.setInterestList(updatedInterestList);
        }

        Optional<UserEntity> user = userRepositorySqlServer.findByUsername(readerDetails.getReader().getUsername());
        if (user.isPresent()) {
            readerDetailsEntity.setReader(userEntityMapper.toReader(user.get()));
            System.out.println("User found: " + user.get().getUsername());
        }else {
            ReaderEntity userSaved = userRepositorySqlServer.save(userEntityMapper.toEntity(readerDetails.getReader()));
            readerDetailsEntity.setReader(userSaved);
        }

        return readerEntityMapper.toModel(readerRepostorySqlServer.save(readerDetailsEntity));
    }

    @Override
    public ReaderDetails update(ReaderDetails readerDetails) {
        ReaderDetailsEntity readerDetailsEntity = readerEntityMapper.toEntity(readerDetails);

        return readerEntityMapper.toModel(readerRepostorySqlServer.save(readerDetailsEntity));
    }

    @Override
    public Iterable<ReaderDetails> findAll() {
        List<ReaderDetails> readerDetails = new ArrayList<>();



        List<ReaderDetailsEntity> readerDetailsEntities = readerRepostorySqlServer.findAll();



        if (readerDetailsEntities.isEmpty()) {
            return readerDetails;
        }

        for (ReaderDetailsEntity readerDetail : readerDetailsEntities ) {

            readerDetails.add(readerEntityMapper.toModel(readerDetail));
        }

        return readerDetails;
    }

    @Override
    public void delete(ReaderDetails readerDetails) {

    }

    @Override
    public List<ReaderDetails> searchReaderDetails(final pt.psoft.g1.psoftg1.shared.services.Page page, final SearchReadersQuery query) {

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<ReaderDetailsEntity> cq = cb.createQuery(ReaderDetailsEntity.class);
        final Root<ReaderDetailsEntity> readerDetailsRoot = cq.from(ReaderDetailsEntity.class);
        Join<ReaderDetailsEntity, User> userJoin = readerDetailsRoot.join("reader");

        cq.select(readerDetailsRoot);

        final List<Predicate> where = new ArrayList<>();
        if (StringUtils.hasText(query.getName())) { //'contains' type search
            where.add(cb.like(userJoin.get("name").get("name"), "%" + query.getName() + "%"));
            cq.orderBy(cb.asc(userJoin.get("name")));
        }
        if (StringUtils.hasText(query.getEmail())) { //'exatct' type search
            where.add(cb.equal(userJoin.get("username"), query.getEmail()));
            cq.orderBy(cb.asc(userJoin.get("username")));

        }
        if (StringUtils.hasText(query.getPhoneNumber())) { //'exatct' type search
            where.add(cb.equal(readerDetailsRoot.get("phoneNumber").get("phoneNumber"), query.getPhoneNumber()));
            cq.orderBy(cb.asc(readerDetailsRoot.get("phoneNumber").get("phoneNumber")));
        }

        // search using OR
        if (!where.isEmpty()) {
            cq.where(cb.or(where.toArray(new Predicate[0])));
        }


        final TypedQuery<ReaderDetailsEntity> q = em.createQuery(cq);
        q.setFirstResult((page.getNumber() - 1) * page.getLimit());
        q.setMaxResults(page.getLimit());

        List<ReaderDetails> readerDetails = new ArrayList<>();

        for (ReaderDetailsEntity readerDetail : q.getResultList()) {
            readerDetails.add(readerEntityMapper.toModel(readerDetail));
        }

        return readerDetails;
    }
}
