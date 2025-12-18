package pt.psoft.g1.psoftg1.readermanagement.repositories.relational;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.model.relational.ReaderDetailsEntity;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.mappers.ReaderEntityMapper;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.ReaderEntity;
import pt.psoft.g1.psoftg1.usermanagement.model.relational.UserEntity;
import pt.psoft.g1.psoftg1.usermanagement.repositories.mappers.UserEntityMapper;
import pt.psoft.g1.psoftg1.usermanagement.repositories.relational.UserRepositorySqlServer;

import java.util.Optional;

@Profile("sqlServer")
@Qualifier("readerSqlServerRepo")
@Component
public class ReaderRepositorySqlServerImpl implements ReaderRepository {

    private final ReaderRepositorySqlServer readerRepostorySqlServer;

    private final UserRepositorySqlServer userRepositorySqlServer;

    private final UserEntityMapper userEntityMapper;
    private final ReaderEntityMapper readerEntityMapper;



    @Autowired
    @Lazy
    public ReaderRepositorySqlServerImpl(ReaderRepositorySqlServer readerRepostorySqlServer, UserRepositorySqlServer userRepositorySqlServer, UserEntityMapper userEntityMapper, ReaderEntityMapper readerEntityMapper) {
        this.readerRepostorySqlServer = readerRepostorySqlServer;
        this.userRepositorySqlServer = userRepositorySqlServer;
        this.userEntityMapper = userEntityMapper;
        this.readerEntityMapper = readerEntityMapper;
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
    public Optional<ReaderDetails> findByUsername(String username) {
        return readerRepostorySqlServer.findByUsername(username).map(readerEntityMapper::toModel);
    }


    @Override
    public ReaderDetails save(ReaderDetails readerDetails) {
        ReaderDetailsEntity readerDetailsEntity = readerEntityMapper.toEntity(readerDetails);
        Optional<UserEntity> user = userRepositorySqlServer.findByUsername(readerDetails.getReader().getUsername());
        if (user.isPresent()) {
            readerDetailsEntity.setReader(userEntityMapper.toReader(user.get()));

        }else {
            ReaderEntity userSaved = userRepositorySqlServer.save(userEntityMapper.toEntity(readerDetails.getReader()));
            readerDetailsEntity.setReader(userSaved);
        }


        return readerEntityMapper.toModel(readerRepostorySqlServer.save(readerDetailsEntity));
    }

    @Override
    public void delete(ReaderDetails rd) {
        readerRepostorySqlServer.delete(readerEntityMapper.toEntity(rd));
    }


}
