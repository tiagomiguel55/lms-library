package pt.psoft.g1.psoftg1.readermanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

@Mapper(componentModel = "spring")
public abstract class ReaderViewAMQPMapper extends MapperInterface {

    @Mapping(target = "fullName", source = "reader.name.name")
    @Mapping(target = "username", source = "reader.username")
    @Mapping(target= "password", source = "reader.password")
    @Mapping(target = "readerNumber", source = "readerNumber")
    @Mapping(target = "version", expression = "java(reader.getVersion().toString())")
    public abstract ReaderViewAMQP toReaderViewAMQP(ReaderDetails reader);



}
