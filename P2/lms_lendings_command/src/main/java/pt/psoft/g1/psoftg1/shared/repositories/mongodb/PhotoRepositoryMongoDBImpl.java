package pt.psoft.g1.psoftg1.shared.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

@Profile("mongodb")
@Qualifier("mongoDbRepo")
@Component
public class PhotoRepositoryMongoDBImpl implements PhotoRepository {

    private final PhotoRepositoryMongoDB photoRepositoryMongoDB;

    @Autowired
    @Lazy
    public PhotoRepositoryMongoDBImpl(PhotoRepositoryMongoDB photoRepositoryMongoDB) {
        this.photoRepositoryMongoDB = photoRepositoryMongoDB;
    }

    @Override
    public void deleteByPhotoFile(String photoFile) {
        photoRepositoryMongoDB.deleteByPhotoFile(photoFile);
    }
}
