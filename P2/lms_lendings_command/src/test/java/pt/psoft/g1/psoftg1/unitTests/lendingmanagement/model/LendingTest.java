package pt.psoft.g1.psoftg1.unitTests.lendingmanagement.model;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@PropertySource({"classpath:config/library.properties"})
class LendingTest {



    private static Book bookdouble;
    private static Reader readerDouble;

    private static ReaderDetails readerDetailsDouble;
    @Value("${lendingDurationInDays}")
    private int lendingDurationInDays;
    @Value("${fineValuePerDayInCents}")
    private int fineValuePerDayInCents;

    @BeforeAll
    public static void setup() {


        bookdouble = mock(Book.class);
        when(bookdouble.getTitle()).thenReturn(mock(Title.class));
        when(bookdouble.getTitle().toString()).thenReturn("O Inspetor Max");

        readerDouble = mock(Reader.class);
        when(readerDouble.getUsername()).thenReturn("manuel@gmail.com");

        readerDetailsDouble = mock(ReaderDetails.class);
        when(readerDetailsDouble.getReader()).thenReturn(readerDouble);
    }

    @Test
    void ensureBookNotNull() {
        assertThrows(IllegalArgumentException.class, () -> new Lending(null, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents));
    }

    @Test
    void ensureReaderNotNull() {
        assertThrows(IllegalArgumentException.class, () -> new Lending(bookdouble, null, 1, lendingDurationInDays, fineValuePerDayInCents));
    }

    @Test
    void ensureValidReaderNumber() {
        assertThrows(IllegalArgumentException.class, () -> new Lending(bookdouble, readerDetailsDouble, -1, lendingDurationInDays, fineValuePerDayInCents));
    }

    @Test
    void ensureReaderNotNullInBuilder() {
        assertThrows(IllegalArgumentException.class, () -> new Lending(0l,bookdouble, null, mock(LendingNumber.class), LocalDate.now(), LocalDate.now(), null ,fineValuePerDayInCents,null, false,false,null,0l,""));
    }

    @Test
    void ensureReaderNotNullInBootstrap() {
        assertThrows(IllegalArgumentException.class, () -> Lending.newBootstrappingLending(bookdouble, null, 2021, 1, LocalDate.now(), null, lendingDurationInDays, fineValuePerDayInCents));
    }


    @Test
    void testSetReturned() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setReturned(0, null);
        assertEquals(LocalDate.now(), lending.getReturnedDate());
    }

    @Test
    void testBootstrapLending() {
        Lending lending = Lending.newBootstrappingLending(bookdouble, readerDetailsDouble, 2021, 1, LocalDate.now(), null, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now(), lending.getStartDate());
        assertEquals(LocalDate.now().plusDays(lendingDurationInDays), lending.getLimitDate());
    }

    @Test
    void testLendingBuilder(){
        Lending lending = new Lending(0l,bookdouble, readerDetailsDouble, mock(LendingNumber.class), LocalDate.now(), LocalDate.now(), null ,fineValuePerDayInCents,null,true,true,"VALIDATED",0l,"");
        assertNotNull(lending);
        assertEquals(bookdouble, lending.getBook());
        assertEquals(readerDetailsDouble, lending.getReaderDetails());
        assertEquals(fineValuePerDayInCents, lending.getFineValuePerDayInCents());
    }


    @Test
    void testSetGenId() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setGenId("1");
        assertEquals("1", lending.getGenId());
    }

    @Test
    void testSetBook() {
        Book book = mock(Book.class);
        when(book.getTitle()).thenReturn(mock(Title.class));
        when(book.getTitle().toString()).thenReturn("O Inspetor Max");
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setBook(book);
        assertEquals(book, lending.getBook());
        assertEquals("O Inspetor Max", lending.getTitle());
    }

    @Test
    void testSetReturnedWhenAlreadyReturnedThrowsException() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setReturned(0, null);
        assertThrows(IllegalArgumentException.class, () -> lending.setReturned(1, "Another return attempt"));
    }

    @Test
    void testSetReturnedWithStaleVersionThrowsException() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertThrows(StaleObjectStateException.class, () -> lending.setReturned(999, null));
    }

    @Test
    void testGetDaysDelayed() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(0, lending.getDaysDelayed());
    }

    @Test
    void testGetFineValueInCentsBeforeDueDate() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(Optional.empty(), lending.getFineValueInCents());
    }

    @Test
    void testGetFineValueInCentsAfterDueDate() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setReturned(0, null);
        lending.getDaysUntilReturn();
        assertFalse(lending.getFineValueInCents().isPresent());
    }

    @Test
    void testGetDaysUntilReturn() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(Optional.of(lendingDurationInDays), lending.getDaysUntilReturn());
    }

    @Test
    void testGetDaysOverDueBeforeReturn() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(Optional.empty(), lending.getDaysOverdue());
    }

    @Test
    void testGetTitle() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals("O Inspetor Max", lending.getTitle());
    }

    @Test
    void testGetLendingNumber() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now().getYear() + "/1", lending.getLendingNumber());
    }

    @Test
    void testGetBook() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(bookdouble, lending.getBook());
    }

    @Test
    void testGetReaderDetails() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(readerDetailsDouble, lending.getReaderDetails());
    }

    @Test
    void testGetStartDate() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now(), lending.getStartDate());
    }

    @Test
    void testGetLimitDate() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertEquals(LocalDate.now().plusDays(lendingDurationInDays), lending.getLimitDate());
    }

    @Test
    void testGetReturnedDateBeforeReturn() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        assertNull(lending.getReturnedDate());
    }

    @Test
    void testGetReturnedDateAfterReturn() {
        Lending lending = new Lending(bookdouble, readerDetailsDouble, 1, lendingDurationInDays, fineValuePerDayInCents);
        lending.setReturned(0, "Returned with commentary");
        assertEquals(LocalDate.now(), lending.getReturnedDate());
    }

}
