package pt.psoft.g1.psoftg1.authormanagement.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;
import pt.psoft.g1.psoftg1.shared.model.Photo;

@Mapper(componentModel = "spring")
public abstract class AuthorMapper extends MapperInterface {
    @Mapping(target = "photo", source = "photoURI", qualifiedByName = "stringToPhoto")
    public abstract Author create(CreateAuthorRequest request);

    public abstract void update(UpdateAuthorRequest request, @MappingTarget Author author);

    @Named("stringToPhoto")
    protected Photo stringToPhoto(String photoURI) {
        if (photoURI == null || photoURI.isBlank()) {
            return null;
        }
        return new Photo(photoURI);
    }
}
