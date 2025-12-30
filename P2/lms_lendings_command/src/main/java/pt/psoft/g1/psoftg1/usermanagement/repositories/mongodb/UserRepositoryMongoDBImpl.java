package pt.psoft.g1.psoftg1.usermanagement.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.LibrarianMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.ReaderMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.model.mongodb.UserMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.repositories.mappers.mongodb.UserMapperMongoDB;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class UserRepositoryMongoDBImpl implements UserRepository {

    private final UserRepositoryMongoDB userRepositoryMongoDB;
    private final UserMapperMongoDB userMapperMongoDB;

    @Autowired
    @Lazy
    public UserRepositoryMongoDBImpl(UserRepositoryMongoDB userRepositoryMongoDB, UserMapperMongoDB userMapperMongoDB) {
        this.userRepositoryMongoDB = userRepositoryMongoDB;
        this.userMapperMongoDB = userMapperMongoDB;
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        List<UserMongoDB> userMongoDBtoSave = new ArrayList<>();

        for(S entity : entities){
            userMongoDBtoSave.add(userMapperMongoDB.toMongoDB(entity));
        }

        for(UserMongoDB userMongoDB : userRepositoryMongoDB.saveAll(userMongoDBtoSave)){
            savedEntities.add((S) userMapperMongoDB.toDomain(userMongoDB));
        }

        return savedEntities;
    }

    @Override
    public <S extends User> S save(S entity) {
        if(entity instanceof Reader){
            ReaderMongoDB readerMongoDB = userMapperMongoDB.toMongoDB((Reader) entity);
            ReaderMongoDB savedReaderMongoDB = userRepositoryMongoDB.save(readerMongoDB);
            return (S) userMapperMongoDB.toDomain(savedReaderMongoDB);
        }

        if(entity instanceof Librarian){
            LibrarianMongoDB librarianMongoDB = userMapperMongoDB.toMongoDB((Librarian) entity);
            LibrarianMongoDB savedLibrarianMongoDB = userRepositoryMongoDB.save(librarianMongoDB);
            return (S) userMapperMongoDB.toDomain(savedLibrarianMongoDB);
        }

        if(entity instanceof User){
            UserMongoDB userMongoDB = userMapperMongoDB.toMongoDB(entity);
            UserMongoDB savedUserMongoDB = userRepositoryMongoDB.save(userMongoDB);
            return (S) userMapperMongoDB.toDomain(savedUserMongoDB);
        }

        throw new IllegalArgumentException("Unsupported entity type: " + entity.getClass().getName());
    }

    @Override
    public Optional<User> findById(String objectId) {
        if(userRepositoryMongoDB.findById(objectId).isEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(userMapperMongoDB.toDomain(userRepositoryMongoDB.findById(objectId).get()));
        }
    }

    @Override
    public User getById(String id) {
        return UserRepository.super.getById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        System.out.println("Username: " + username);
        if(userRepositoryMongoDB.findByUsername(username).isEmpty()){
            System.out.println("User not found");
            return Optional.empty();
        } else {
            System.out.println("User found");
            return Optional.of(userMapperMongoDB.toDomain(userRepositoryMongoDB.findByUsername(username).get()));
        }
    }

    @Override
    public List<User> searchUsers(Page page, SearchUsersQuery query) {
        return List.of();
    }

    @Override
    public List<User> findByNameName(String name) {
        return null;
    }

    @Override
    public List<User> findByNameNameContains(String name) {
        List<UserMongoDB> usersMongoDBFound = userRepositoryMongoDB.findByNameName(name);
        List<User> usersFound = new ArrayList<>();

        for(UserMongoDB userMongoDB : usersMongoDBFound){
            usersFound.add(userMapperMongoDB.toDomain(userMongoDB));
        }

        return usersFound;
    }

    @Override
    public void delete(User user) {

    }
}
