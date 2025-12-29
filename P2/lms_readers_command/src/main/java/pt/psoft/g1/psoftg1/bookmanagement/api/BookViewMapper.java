package pt.psoft.g1.psoftg1.bookmanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class BookViewMapper extends MapperInterface {
    @Mapping(target = "genre", source = "genre")
    @Mapping(target = "isbn", source = "isbn")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "title", source = "title")
    //@Mapping(target = "authors", expression = "java(mapAuthors(book.getAuthors()))")
    @Mapping(target = "_links", expression = "java(mapLinks(book))")
    public abstract BookView toBookView(Book book);

    public abstract List<BookView> toBookView(List<Book> bookList);



    @Named(value = "mapBookLinks")
    public Map<String, Object> mapLinks(final Book book) {
        String bookUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/books/")
                .path(book.getIsbn())
                .toUriString();

        Map<String, Object> links = new HashMap<>();
        links.put("self", bookUri);

        links.put("photo", generatePhotoUrl(book));

        return links;
    }


    protected String generatePhotoUrl(Book book) {
        String isbn = book.getIsbn();
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/books/{isbn}/photo").buildAndExpand(isbn).toUri().toString();
    }
}
