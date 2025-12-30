package pt.psoft.g1.psoftg1.lendingmanagement.api;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

@Mapper(componentModel = "spring")
public abstract class LendingViewAMQPMapper extends MapperInterface {

    @Mapping(source = "lendingNumber", target = "lendingNumber")
    @Mapping(source = "genId", target = "genId")
    @Mapping(target = "isbn", expression = "java(lending.getBook().getIsbn())")
    @Mapping(target = "readerNumber", expression = "java(lending.getReaderDetails().getReaderNumber())")
    @Mapping(target = "startDate", expression = "java(lending.getStartDate().toString())")
    @Mapping(target = "limitDate", expression = "java(lending.getLimitDate().toString())")
    @Mapping(target = "returnedDate", expression = "java(lending.getReturnedDate() != null ? lending.getReturnedDate().toString() : null)")
    @Mapping(expression = "java(Long.toString(lending.getVersion()))", target = "version")
    @Mapping(target = "daysUntilReturn", expression = "java(lending.getDaysUntilReturn().orElse(0))")
    @Mapping(target = "daysOverdue", expression = "java(lending.getDaysOverdue().orElse(0))")
    @Mapping(target = "fineValueInCents", expression = "java(lending.getFineValueInCents().orElse(0))")
    public abstract LendingViewAMQP toLendingViewAMQP(Lending lending);

}
