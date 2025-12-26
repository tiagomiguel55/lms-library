package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.impl;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

public interface SpringDataPhotoRepository extends PhotoRepository, MongoRepository<Photo, String> {
    
    @Override
    default void deleteByPhotoFile(String photoFile) {
        // For MongoDB, we need to find and delete
        // This should be implemented if needed
    }
}
