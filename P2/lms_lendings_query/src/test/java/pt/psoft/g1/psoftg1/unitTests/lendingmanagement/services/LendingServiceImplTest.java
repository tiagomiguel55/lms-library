package pt.psoft.g1.psoftg1.unitTests.lendingmanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingServiceImpl;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SearchLendingQuery;
import pt.psoft.g1.psoftg1.lendingmanagement.services.SetLendingReturnedRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.services.generator.ApplicationContextProvider;
import pt.psoft.g1.psoftg1.shared.services.generator.IdGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class LendingServiceImplTest {

    @Mock
    private LendingRepository lendingRepository;
    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ApplicationContextProvider applicationContextProvider;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private FineRepository fineRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private LendingServiceImpl lendingService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        when(applicationContext.getBean(IdGenerator.class)).thenReturn(idGenerator);

        applicationContextProvider.setApplicationContext(applicationContext);

    }

    @Test
    void testListByReaderNumberAndIsbn_WithEmptyReturned() {
        // Arrange
        String readerNumber = "R123";
        String isbn = "ISBN123";
        List<Lending> lendings = List.of(mock(Lending.class), mock(Lending.class));
        when(lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.listByReaderNumberAndIsbn(readerNumber, isbn, Optional.empty());

        // Assert
        assertEquals(lendings.size(), result.size());
        assertEquals(lendings, result);
    }

    @Test
    void testListByReaderNumberAndIsbn_WithReturnedTrue() {
        // Arrange
        String readerNumber = "R123";
        String isbn = "ISBN123";

        Lending returnedLending = mock(Lending.class);
        when(returnedLending.getReturnedDate()).thenReturn(LocalDate.now()); // simulate returned

        Lending notReturnedLending = mock(Lending.class);
        when(notReturnedLending.getReturnedDate()).thenReturn(null); // simulate not returned

        List<Lending> lendings = new ArrayList<>();
        lendings.add(returnedLending);
        lendings.add(notReturnedLending);
        when(lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.listByReaderNumberAndIsbn(readerNumber, isbn, Optional.of(true));

        // Assert
        assertEquals(1, result.size());
        assertEquals(returnedLending, result.get(0));
    }

    @Test
    void testListByReaderNumberAndIsbn_WithReturnedFalse() {
        // Arrange
        String readerNumber = "R123";
        String isbn = "ISBN123";

        Lending returnedLending = mock(Lending.class);
        when(returnedLending.getReturnedDate()).thenReturn(LocalDate.now()); // simulate returned

        Lending notReturnedLending = mock(Lending.class);
        when(notReturnedLending.getReturnedDate()).thenReturn(null); // simulate not returned

        List<Lending> lendings = new ArrayList<>();
        lendings.add(returnedLending);
        lendings.add(notReturnedLending);
        when(lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.listByReaderNumberAndIsbn(readerNumber, isbn, Optional.of(false));

        // Assert
        assertEquals(1, result.size());
        assertEquals(notReturnedLending, result.get(0));
    }

    @Test
    void testListByReaderNumberAndIsbn_NoMatchingResults() {
        // Arrange
        String readerNumber = "R123";
        String isbn = "ISBN123";

        Lending returnedLending = mock(Lending.class);
        when(returnedLending.getReturnedDate()).thenReturn(LocalDate.now()); // simulate returned

        Lending notReturnedLending = mock(Lending.class);
        when(notReturnedLending.getReturnedDate()).thenReturn(null); // simulate not returned

        List<Lending> lendings = new ArrayList<>();
        lendings.add(returnedLending);
        lendings.add(notReturnedLending);
        when(lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.listByReaderNumberAndIsbn(readerNumber, isbn, Optional.of(false));

        // Assert
        assertTrue(result.stream().noneMatch(l -> l.getReturnedDate() != null));
    }

    @Test
    void testFindByLendingNumber_Success() {
        String lendingNumber = "LN123";
        Lending lending = mock(Lending.class);
        when(lending.getLendingNumber()).thenReturn(lendingNumber);

        when(lendingRepository.findByLendingNumber(lendingNumber)).thenReturn(Optional.of(lending));

        Optional<Lending> result = lendingService.findByLendingNumber(lendingNumber);

        assertTrue(result.isPresent());
        assertEquals(lendingNumber, result.get().getLendingNumber());
    }

    @Test
    void testFindByLendingNumber_NotFound() {
        String lendingNumber = "LN123";

        when(lendingRepository.findByLendingNumber(lendingNumber)).thenReturn(Optional.empty());

        Optional<Lending> result = lendingService.findByLendingNumber(lendingNumber);

        assertFalse(result.isPresent());
    }

    @Test
    void testCreateLending_ThrowsLendingForbiddenException_WhenUserHasOutstandingBooks() {
        String readerNumber = "R123";
        String isbn = "ISBN123";
        CreateLendingRequest request = mock(CreateLendingRequest.class);
        when(request.getReaderNumber()).thenReturn(readerNumber);
        when(request.getIsbn()).thenReturn(isbn);

        Book book = mock(Book.class);
        ReaderDetails reader = mock(ReaderDetails.class);


        Lending existingLending = mock(Lending.class);
        when(existingLending.getDaysDelayed()).thenReturn(0);  // simulate no delay
        when(existingLending.getReturnedDate()).thenReturn(null);// simulate not returned yet
        when(existingLending.getBook()).thenReturn(book);
        when(existingLending.getBook().getIsbn()).thenReturn(isbn);
        when(existingLending.getReaderDetails()).thenReturn(reader);
        when(existingLending.getReaderDetails().getReaderNumber()).thenReturn(readerNumber);

        when(lendingRepository.listOutstandingByReaderNumber(readerNumber)).thenReturn(List.of(existingLending, existingLending, existingLending));


        LendingForbiddenException exception = assertThrows(LendingForbiddenException.class, () ->
                lendingService.create(request));

        assertEquals("Reader has three books outstanding already", exception.getMessage());
    }

    @Test
    void testCreateLending_ThrowsNotFoundException_WhenBookNotFound() {
        String readerNumber = "R123";
        String isbn = "ISBN123";
        CreateLendingRequest request = mock(CreateLendingRequest.class);
        when(request.getReaderNumber()).thenReturn(readerNumber);
        when(request.getIsbn()).thenReturn(isbn);

        when(lendingRepository.listOutstandingByReaderNumber(readerNumber)).thenReturn(List.of());
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.empty());


        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                lendingService.create(request));

        assertEquals("Book not found", exception.getMessage());
    }

    @Test
    void testCreateLending_ThrowsNotFoundException_WhenReaderNotFound() {
        String readerNumber = "R123";
        String isbn = "ISBN123";
        CreateLendingRequest request = mock(CreateLendingRequest.class);
        when(request.getReaderNumber()).thenReturn(readerNumber);
        when(request.getIsbn()).thenReturn(isbn);

        Book book = mock(Book.class);
        when(book.getIsbn()).thenReturn(isbn);

        when(lendingRepository.listOutstandingByReaderNumber(readerNumber)).thenReturn(List.of());
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(book));
        when(readerRepository.findByReaderNumber(readerNumber)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                lendingService.create(request));

        assertEquals("Reader not found", exception.getMessage());

    }




    @Test
    void testSetReturned_SavesFine_WhenLendingIsDelayed() {
        String lendingNumber = "LN123";
        SetLendingReturnedRequest request = mock(SetLendingReturnedRequest.class);
        when(request.getCommentary()).thenReturn("Commentary");
        Lending lending = mock(Lending.class);
        when(lending.getLendingNumber()).thenReturn(lendingNumber);
        when(lending.getDaysDelayed()).thenReturn(1);  // simulate delay
        when(lending.getReturnedDate()).thenReturn(null);  // simulate not returned yet

        when(lendingRepository.findByLendingNumber(lendingNumber)).thenReturn(Optional.of(lending));

        lendingService.setReturned(lendingNumber, request, 1);

        verify(fineRepository, times(1)).save(any(Fine.class));
        verify(lendingRepository, times(1)).save(lending);
    }

    @Test
    void testGetAverageDuration() {
        when(lendingRepository.getAverageDuration()).thenReturn(15.7);

        Double avgDuration = lendingService.getAverageDuration();

        assertEquals(15.7, avgDuration);
    }

    @Test
    void testGetOverdue() {
        Page page = mock(Page.class);
        when(page.getNumber()).thenReturn(1);
        when(page.getLimit()).thenReturn(10);
        List<Lending> overdueLendings = List.of(mock(Lending.class), mock(Lending.class));

        when(lendingRepository.getOverdue(page)).thenReturn(overdueLendings);

        List<Lending> result = lendingService.getOverdue(page);

        assertEquals(overdueLendings.size(), result.size());
        verify(lendingRepository, times(1)).getOverdue(page);
    }

    @Test
    void testSearchLendings_WithValidPageAndQuery() {
        // Arrange
        Page page = mock(Page.class);
        when(page.getNumber()).thenReturn(1);
        when(page.getLimit()).thenReturn(10);
        SearchLendingQuery query = mock(SearchLendingQuery.class);
        when(query.getReaderNumber()).thenReturn("R123");
        when(query.getIsbn()).thenReturn("ISBN123");
        when(query.getReturned()).thenReturn(true);
        when(query.getStartDate()).thenReturn("2023-01-01");
        when(query.getEndDate()).thenReturn("2023-02-01");
        List<Lending> lendings = List.of(mock(Lending.class), mock(Lending.class));

        LocalDate startDate = LocalDate.parse("2023-01-01");
        LocalDate endDate = LocalDate.parse("2023-02-01");

        when(lendingRepository.searchLendings(page, "R123", "ISBN123", true, startDate, endDate)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.searchLendings(page, query);

        // Assert
        assertEquals(lendings.size(), result.size());
        verify(lendingRepository, times(1)).searchLendings(page, "R123", "ISBN123", true, startDate, endDate);
    }

    @Test
    void testSearchLendings_WithNullQuery_UsesDefaultValues() {
        // Arrange
        Page page = mock(Page.class);
        when(page.getNumber()).thenReturn(1);
        when(page.getLimit()).thenReturn(10);
        SearchLendingQuery query = mock(SearchLendingQuery.class);
        when(query.getReaderNumber()).thenReturn("R123");
        when(query.getIsbn()).thenReturn("ISBN123");
        when(query.getReturned()).thenReturn(true);
        when(query.getStartDate()).thenReturn("2023-01-01");
        when(query.getEndDate()).thenReturn("2023-02-01");
        List<Lending> lendings = List.of(mock(Lending.class));

        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = null;

        when(lendingRepository.searchLendings(page, "", "", null, startDate, endDate)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.searchLendings(page, null);

        // Assert
        assertEquals(lendings.size(), result.size());
        verify(lendingRepository, times(1)).searchLendings(page, "", "", null, startDate, endDate);
    }

    @Test
    void testSearchLendings_InvalidDateFormat_ThrowsException() {
        // Arrange
        Page page = mock(Page.class);
        when(page.getNumber()).thenReturn(1);
        when(page.getLimit()).thenReturn(10);
        SearchLendingQuery query = mock(SearchLendingQuery.class);
        when(query.getReaderNumber()).thenReturn("R123");
        when(query.getIsbn()).thenReturn("ISBN123");
        when(query.getReturned()).thenReturn(true);
        when(query.getStartDate()).thenReturn("invalid-date");
        when(query.getEndDate()).thenReturn("2023-02-01");
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lendingService.searchLendings(page, query));

        assertEquals("Expected format is YYYY-MM-DD", exception.getMessage());
    }

    @Test
    void testSearchLendings_WithSpecificDateRange() {
        // Arrange
        Page page = mock(Page.class);
        when(page.getNumber()).thenReturn(1);
        when(page.getLimit()).thenReturn(10);
        SearchLendingQuery query = mock(SearchLendingQuery.class);
        when(query.getReaderNumber()).thenReturn("R123");
        when(query.getIsbn()).thenReturn("ISBN123");
        when(query.getReturned()).thenReturn(true);
        when(query.getStartDate()).thenReturn("2023-01-01");
        when(query.getEndDate()).thenReturn("2023-01-31");
        List<Lending> lendings = List.of(mock(Lending.class));

        LocalDate startDate = LocalDate.parse("2023-01-01");
        LocalDate endDate = LocalDate.parse("2023-01-31");

        when(lendingRepository.searchLendings(page, "R123", "ISBN123", true, startDate, endDate)).thenReturn(lendings);

        // Act
        List<Lending> result = lendingService.searchLendings(page, query);

        // Assert
        assertEquals(lendings.size(), result.size());
        verify(lendingRepository, times(1)).searchLendings(page, "R123", "ISBN123", true, startDate, endDate);
    }
}