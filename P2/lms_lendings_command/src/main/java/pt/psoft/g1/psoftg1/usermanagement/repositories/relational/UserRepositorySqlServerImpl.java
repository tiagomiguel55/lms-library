package pt.psoft.g1.psoftg1.usermanagement.repositories.relational;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;
import pt.psoft.g1.psoftg1.readermanagement.repositories.relational.ReaderRepositorySqlServer;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.LibrarianEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.ReaderEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.UserEntity;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.repositories.mappers.UserEntityMapper;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("sqlServer")
@Qualifier("userSqlServerRepo")
@Component
public class UserRepositorySqlServerImpl implements UserRepository {

    private final UserRepositorySqlServer userRepositorySqlServer;

    private final UserEntityMapper userEntityMapper;
    private final EntityManager em;

    @Autowired
    @Lazy
    public UserRepositorySqlServerImpl(UserRepositorySqlServer userRepositorySqlServer, UserEntityMapper userEntityMapper, EntityManager em) {
        this.userRepositorySqlServer = userRepositorySqlServer;

        this.userEntityMapper = userEntityMapper;
        this.em = em;
    }
    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {

        List<S> savedEntities = new ArrayList<>();

        List<UserEntity> userEntitiesToSave = new ArrayList<>();

        for (S entity : entities) {
            userEntitiesToSave.add(userEntityMapper.toEntity(entity));
        }

        for (UserEntity userEntity : userRepositorySqlServer.saveAll(userEntitiesToSave)) {
            savedEntities.add((S) userEntityMapper.toModel(userEntity));
        }


        return savedEntities;
    }

    @Override
    public <S extends User> S save(S entity) {

        if (entity instanceof Reader) {

            ReaderEntity readerEntity = userEntityMapper.toEntity((Reader) entity);

            ReaderEntity savedEntity = userRepositorySqlServer.save(readerEntity);

            return (S) userEntityMapper.toModel(savedEntity);

        } else if (entity instanceof Librarian) {
            LibrarianEntity librarianEntity = userEntityMapper.toEntity((Librarian) entity);
            LibrarianEntity savedEntity = userRepositorySqlServer.save(librarianEntity);
            return (S) userEntityMapper.toModel(savedEntity);

        } else if (entity instanceof User) {
            UserEntity userEntity = userEntityMapper.toEntity(entity);
            UserEntity savedEntity = userRepositorySqlServer.save(userEntity);
            return (S) userEntityMapper.toModel(savedEntity);
        }

        throw new IllegalArgumentException("Unsupported entity type: " + entity.getClass().getName());
    }

    @Override
    public Optional<User> findById(String objectId) {

        if (userRepositorySqlServer.findById(Long.parseLong(objectId)).isEmpty()) {
            return Optional.empty();
        }else return Optional.of(userEntityMapper.toModel(userRepositorySqlServer.findById(Long.parseLong(objectId)).get()));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (userRepositorySqlServer.findByUsername(username).isEmpty()) {
            return Optional.empty();
        }else return Optional.of(userEntityMapper.toModel(userRepositorySqlServer.findByUsername(username).get()));
    }

    @Override
    public List<User> searchUsers(final Page page, final SearchUsersQuery query) {

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
        final Root<UserEntity> root = cq.from(UserEntity.class);
        cq.select(root);

        final List<Predicate> where = new ArrayList<>();
        if (StringUtils.hasText(query.getUsername())) {
            where.add(cb.equal(root.get("username"), query.getUsername()));
        }
        if (StringUtils.hasText(query.getFullName())) {
            where.add(cb.like(root.get("fullName"), "%" + query.getFullName() + "%"));
        }

        // search using OR
        if (!where.isEmpty()) {
            cq.where(cb.or(where.toArray(new Predicate[0])));
        }

        cq.orderBy(cb.desc(root.get("createdAt")));

        final TypedQuery<UserEntity> q = em.createQuery(cq);
        q.setFirstResult((page.getNumber() - 1) * page.getLimit());
        q.setMaxResults(page.getLimit());

        List<User> users = new ArrayList<>();

        for (UserEntity userEntity : q.getResultList()) {
            users.add(userEntityMapper.toModel(userEntity));
        }

        return users;
    }

    @Override
    public List<User> findByNameName(String name) {
        return null;
    }

    @Override
    public List<User> findByNameNameContains(String name) {
        return null;
    }

    @Override
    public void delete(User user) {

    }
}
