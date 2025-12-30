package pt.psoft.g1.psoftg1.lendingmanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQPMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@PropertySource({"classpath:config/library.properties"})
public class LendingServiceImpl implements LendingService{

    private final LendingRepository lendingRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final LendingViewAMQPMapper lendingViewAMQPMapper;

    @Value("${lendingDurationInDays}")
    private int lendingDurationInDays;
    @Value("${fineValuePerDayInCents}")
    private int fineValuePerDayInCents;


    @Override
    public Lending create(LendingViewAMQP lending) {

        final var b = bookRepository.findByIsbn(lending.getIsbn())
                .orElseThrow(() -> new NotFoundException("Book not found"));
        final var r = readerRepository.findByReaderNumber(lending.getReaderNumber())
                .orElseThrow(() -> new NotFoundException("Reader not found"));
        int seq = lendingRepository.getCountFromCurrentYear()+1;
        final Lending l = new Lending(b,r,seq, lendingDurationInDays, fineValuePerDayInCents );

        return lendingRepository.save(l);
    }

    @Override
    public Lending update(LendingViewAMQP lending) {

        final var l = lendingRepository.findByLendingNumber(lending.getLendingNumber())
                .orElseThrow(() -> new NotFoundException("Lending not found"));
        final var b = bookRepository.findByIsbn(lending.getIsbn())
                .orElseThrow(() -> new NotFoundException("Book not found"));
        final var r = readerRepository.findByReaderNumber(lending.getReaderNumber())
                .orElseThrow(() -> new NotFoundException("Reader not found"));



        l.applyPatch(b,r, LocalDate.parse(lending.getReturnedDate()),LocalDate.parse(lending.getLimitDate()),LocalDate.parse(lending.getReturnedDate())) ;

        return lendingRepository.save(l);

    }

    @Override
    public void delete(LendingViewAMQP lending) {
        final var l = lendingRepository.findByLendingNumber(lending.getLendingNumber())
                .orElseThrow(() -> new NotFoundException("Lending not found"));
        lendingRepository.delete(l);
    }

    @Override
    public List<LendingViewAMQP> getAllLendings() {

        List<Lending> lendings = lendingRepository.findAll();
        List<LendingViewAMQP> lendingsViewAMQP = new ArrayList<>();

        for (Lending l : lendings) {

            lendingsViewAMQP.add(lendingViewAMQPMapper.toLendingViewAMQP(l));
        }

        return lendingsViewAMQP;

    }
}
