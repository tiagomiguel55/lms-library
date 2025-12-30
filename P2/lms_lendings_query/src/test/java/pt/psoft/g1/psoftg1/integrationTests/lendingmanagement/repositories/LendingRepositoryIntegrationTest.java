package pt.psoft.g1.psoftg1.integrationTests.lendingmanagement.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class LendingRepositoryIntegrationTest {

    @Autowired
    private LendingRepository lendingRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;


    private Lending lending;
    private ReaderDetails readerDetails;
    private Reader reader;
    private Book book;


    @BeforeEach
    public void setUp() {

        book = new Book("9782826012092",
                "O Inspetor Max",
                "conhecido pastor-alemão que trabalha para a Judiciária, vai ser fundamental para resolver um importante caso de uma rede de malfeitores que quer colocar uma bomba num megaconcerto de uma ilustre cantora",
                null);
        bookRepository.save(book);

        reader = Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives");
        userRepository.save(reader);

        readerDetails = new ReaderDetails(1,
                reader);
        readerRepository.save(readerDetails);

        // Create and save the lending
        lending = Lending.newBootstrappingLending(book,
                readerDetails,
                LocalDate.now().getYear(),
                999,
                LocalDate.of(LocalDate.now().getYear(), 1,1),
                LocalDate.of(LocalDate.now().getYear(), 1,11),
                15,
                300);
        lendingRepository.save(lending);
    }

    @AfterEach
    public void tearDown(){
        //lendingRepository.delete(lending);
        readerRepository.delete(readerDetails);
        userRepository.delete(reader);
        bookRepository.delete(book);
    }

    @Test
    public void testSave() {
        Lending newLending = new Lending(lending.getBook(), lending.getReaderDetails(), 2, 14, 50);
        Lending savedLending = lendingRepository.save(newLending);
        assertThat(savedLending).isNotNull();
        assertThat(savedLending.getLendingNumber()).isEqualTo(newLending.getLendingNumber());
        //lendingRepository.delete(savedLending);
    }

    @Test
    public void testFindByLendingNumber() {
        String ln = lending.getLendingNumber();
        Optional<Lending> found = lendingRepository.findByLendingNumber(ln);
        assertThat(found).isPresent();
        assertThat(found.get().getLendingNumber()).isEqualTo(ln);
    }



    @Test
    public void testGetCountFromCurrentYear() {
        int count = lendingRepository.getCountFromCurrentYear();
        assertThat(count).isEqualTo(1);
        var lending2 = Lending.newBootstrappingLending(book,
                readerDetails,
                LocalDate.now().getYear(),
                998,
                LocalDate.of(LocalDate.now().getYear(), 5,31),
                null,
                15,
                300);
        lendingRepository.save(lending2);
        count = lendingRepository.getCountFromCurrentYear();
        assertThat(count).isEqualTo(2);
    }




}
