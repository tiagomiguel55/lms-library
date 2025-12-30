package pt.psoft.g1.psoftg1.readermanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ReaderViewAMQPMapper extends MapperInterface {

    @Mapping(target = "fullName", source = "reader.name.name")
    @Mapping(target = "username", source = "reader.username")
    @Mapping(target= "password", source = "reader.password")
    @Mapping(target = "birthDate", source = "birthDate.birthDate")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "gdpr", source = "gdprConsent")
    @Mapping(target = "marketing", source = "marketingConsent")
    @Mapping(target = "thirdParty", source = "thirdPartySharingConsent")
    @Mapping(target = "readerNumber", source = "readerNumber")
    @Mapping(target = "interestList", expression = "java(mapInterestList(reader.getInterestList()))")
    @Mapping(target = "version", expression = "java(reader.getVersion().toString())")
    public abstract ReaderViewAMQP toReaderViewAMQP(ReaderDetails reader);

    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "username", source = "username")
    @Mapping(target= "password", source = "password")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "gdpr", source = "gdpr")
    @Mapping(target = "marketing", source = "marketing")
    @Mapping(target = "thirdParty", source = "thirdParty")
    @Mapping(target = "readerNumber", source = "readerNumber")
    @Mapping(target = "interestList", source = "interestList")
    @Mapping(target = "version", source = "version")
    public abstract ReaderViewAMQP toReaderViewAMQP(ReaderSagaViewAMQP reader);

    protected List<String> mapInterestList(List<Genre> interestList) {
        List<String> stringInterestList = new ArrayList<>();

        if(interestList == null || interestList.isEmpty()) {
            return stringInterestList;
        }

        for(Genre genre : interestList) {
            stringInterestList.add(genre.getGenre());
        }

        return stringInterestList;
    }

}
