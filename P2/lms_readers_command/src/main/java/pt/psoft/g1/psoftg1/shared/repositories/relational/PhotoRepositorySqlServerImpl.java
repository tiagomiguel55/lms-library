package pt.psoft.g1.psoftg1.shared.repositories.relational;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

@Profile("sqlServer")
@Qualifier("photoSqlServerRepo")
@Component
public class PhotoRepositorySqlServerImpl implements PhotoRepository {

    private final PhotoRepositorySqlServer photoRepositorySqlServer;

    @Autowired
    @Lazy
    public PhotoRepositorySqlServerImpl(PhotoRepositorySqlServer photoRepositorySqlServer) {
        this.photoRepositorySqlServer = photoRepositorySqlServer;
    }
    @Override
    public void deleteByPhotoFile(String photoFile) {
        photoRepositorySqlServer.deleteByPhotoFile(photoFile);
    }
}
