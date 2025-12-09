package pt.psoft.g1.psoftg1.authormanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class AuthorViewAMQPMapper extends MapperInterface {

    @Mapping(target = "authorNumber", source = "authorNumber")
    @Mapping(target = "name", expression = "java(author.getName())")
    @Mapping(target = "bio", expression = "java(author.getBio())")
    @Mapping(target = "photoURI", expression = "java(mapPhotoURI(author))")
    @Mapping(target = "version", source = "version")
    public abstract AuthorViewAMQP toAuthorViewAMQP(Author author);

    public abstract List<AuthorViewAMQP> toAuthorViewAMQP(List<Author> authorList);

    protected String mapPhotoURI(Author author) {
        return author.getPhoto() != null ? author.getPhoto().getPhotoFile() : null;
    }
}
