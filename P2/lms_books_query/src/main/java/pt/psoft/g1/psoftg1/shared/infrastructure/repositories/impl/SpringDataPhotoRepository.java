package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.impl;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

public interface SpringDataPhotoRepository extends PhotoRepository, MongoRepository<Photo, String> {
    @Override
    void deleteByPhotoFile(String photoFile);
}
