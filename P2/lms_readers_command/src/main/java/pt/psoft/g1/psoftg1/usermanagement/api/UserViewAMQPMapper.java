package pt.psoft.g1.psoftg1.usermanagement.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

@Mapper(componentModel = "spring")
public abstract class UserViewAMQPMapper extends MapperInterface {

    @Mapping(target = "fullName", source = "name.name")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "version", expression = "java(user.getVersion().toString())")
    public abstract UserViewAMQP toUserViewAMQP(User user);

    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "version", source = "version")
    public abstract UserViewAMQP toUserViewAMQP(UserViewAMQP userView);
}
