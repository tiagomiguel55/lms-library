package pt.psoft.g1.psoftg1.unitTests.readermanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.publishers.ReaderEventPublisher;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.readermanagement.services.*;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.model.Photo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class ReaderServiceImplTest {

    @Mock
    private ReaderRepository readerRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private ReaderMapper readerMapper;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ForbiddenNameRepository forbiddenNameRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private ReaderEventPublisher readerEventPublisher;

    @InjectMocks
    private ReaderServiceImpl readerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);


    }

    @Test
    void testCreateReader_Success() {
        CreateReaderRequest request = mock(CreateReaderRequest.class);
        List<String> interestList = new ArrayList<>();
        interestList.add("Fiction");
        when(request.getUsername()).thenReturn("testuser");
        when(request.getFullName()).thenReturn("Test User");
        when(request.getInterestList()).thenReturn(interestList);
        when(request.getPhoto()).thenReturn(null);
        String photoURI = null;

        Genre mockGenre = mock(Genre.class);
        when(mockGenre.getGenre()).thenReturn("Fiction");

        ReaderServiceImpl ReaderServiceSpy = spy(readerService);

        Reader mockReader = mock(Reader.class);
        ReaderDetails mockReaderDetails = mock(ReaderDetails.class);
        when(userRepo.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(forbiddenNameRepository.findByForbiddenNameIsContained(any())).thenReturn(Collections.emptyList());
        when(readerMapper.createReader(request)).thenReturn(mockReader);
        when(readerRepo.getCountFromCurrentYear()).thenReturn(0);
        when(readerMapper.createReaderDetails(anyInt(), eq(mockReader), eq(request), eq(photoURI), any())).thenReturn(mockReaderDetails);
        when(readerRepo.save(any(ReaderDetails.class))).thenReturn(mockReaderDetails);
        Optional<Genre> optionalGenre = Optional.of(mockGenre);
        System.out.println("Optional Genre: " + optionalGenre);
        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(mockGenre));


        ReaderDetails result = readerService.create(request, photoURI);

        assertNotNull(result);
        //verify(userRepo).save(mockReader);
        verify(readerRepo).save(mockReaderDetails);

    }

    @Test
    void testCreateReader_UsernameConflict() {
        CreateReaderRequest request = mock(CreateReaderRequest.class);
        when(request.getUsername()).thenReturn("testuser");

        when(userRepo.findByUsername(request.getUsername())).thenReturn(Optional.of(mock(Reader.class)));

        ConflictException exception = assertThrows(ConflictException.class, () -> readerService.create(request, null));
        assertEquals("Username already exists!", exception.getMessage());
    }





    @Test
    void testUpdateReader_Success() {
        String id = "readerId";
        UpdateReaderRequest request = mock(UpdateReaderRequest.class);
        List<String> interestList = new ArrayList<>();
        interestList.add("Fiction");
        when(request.getUsername()).thenReturn("testuser");
        when(request.getFullName()).thenReturn("Test User");
        when(request.getInterestList()).thenReturn(interestList);
        when(request.getPhoto()).thenReturn(null);
        String photoURI = null;

        Genre mockGenre = mock(Genre.class);
        when(mockGenre.getGenre()).thenReturn("Fiction");

        ReaderServiceImpl ReaderServiceSpy = spy(readerService);

        Reader mockReader = mock(Reader.class);
        ReaderDetails mockReaderDetails = mock(ReaderDetails.class);


        when(userRepo.findByUsername(any(String.class))).thenReturn(Optional.empty());
        when(forbiddenNameRepository.findByForbiddenNameIsContained(any())).thenReturn(Collections.emptyList());
        when(readerRepo.findByUserId(id)).thenReturn(Optional.of(mockReaderDetails));
        when(mockReaderDetails.getReader()).thenReturn(mockReader);
        when(readerRepo.getCountFromCurrentYear()).thenReturn(0);
        when(readerRepo.save(any(ReaderDetails.class))).thenReturn(mockReaderDetails);
        Optional<Genre> optionalGenre = Optional.of(mockGenre);
        System.out.println("Optional Genre: " + optionalGenre);
        when(genreRepository.findByName("Fiction")).thenReturn(Optional.of(mockGenre));

        ReaderDetails result = readerService.update(id, request, 1L, photoURI);

        assertNotNull(result);
        verify(readerRepo).save(mockReaderDetails);
    }

    @Test
    void testUpdateReader_NotFound() {
        String id = "readerId";
        UpdateReaderRequest request = new UpdateReaderRequest();
        request.setInterestList(List.of("Fiction"));

        when(readerRepo.findByUserId(id)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> readerService.update(id, request, 1L, null));
        assertEquals("Cannot find reader", exception.getMessage());
    }

    @Test
    void testFindByReaderNumber_Success() {
        String readerNumber = "123";
        ReaderDetails mockReaderDetails = mock(ReaderDetails.class);

        when(readerRepo.findByReaderNumber(readerNumber)).thenReturn(Optional.of(mockReaderDetails));

        Optional<ReaderDetails> result = readerService.findByReaderNumber(readerNumber);

        assertTrue(result.isPresent());
        assertEquals(mockReaderDetails, result.get());
    }

    @Test
    void testFindByReaderNumber_NotFound() {
        String readerNumber = "123";

        when(readerRepo.findByReaderNumber(readerNumber)).thenReturn(Optional.empty());

        Optional<ReaderDetails> result = readerService.findByReaderNumber(readerNumber);

        assertFalse(result.isPresent());
    }

    @Test
    void testSearchReaders_NoResults() {
        Page page = mock(Page.class);
        SearchReadersQuery query = mock(SearchReadersQuery.class);

        when(readerRepo.searchReaderDetails(page, query)).thenReturn(Collections.emptyList());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> readerService.searchReaders(page, query));
        assertEquals("No results match the search query", exception.getMessage());
    }

    @Test
    void testSearchReaders_Success() {
        Page page = mock(Page.class);
        SearchReadersQuery query = mock(SearchReadersQuery.class);
        List<ReaderDetails> mockReaders = new ArrayList<>();
        mockReaders.add(mock(ReaderDetails.class));

        when(readerRepo.searchReaderDetails(page, query)).thenReturn(mockReaders);

        List<ReaderDetails> result = readerService.searchReaders(page, query);

        assertEquals(mockReaders, result);
    }

    @Test
    void testFindByPhoneNumber_Success() {
        // Arrange
        String phoneNumber = "123456789";
        ReaderDetails mockReader = mock(ReaderDetails.class);
        when(readerRepo.findByPhoneNumber(phoneNumber)).thenReturn(List.of(mockReader));

        // Act
        List<ReaderDetails> result = readerService.findByPhoneNumber(phoneNumber);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(mockReader, result.get(0));
        verify(readerRepo).findByPhoneNumber(phoneNumber);
    }

    @Test
    void testFindByUsername_Success() {
        // Arrange
        String username = "testuser";
        ReaderDetails mockReader = mock(ReaderDetails.class);
        when(readerRepo.findByUsername(username)).thenReturn(Optional.of(mockReader));

        // Act
        Optional<ReaderDetails> result = readerService.findByUsername(username);

        // Assert
        assertTrue(result.isPresent());
        assertSame(mockReader, result.get());
        verify(readerRepo).findByUsername(username);
    }

    @Test
    void testFindByUsername_NotFound() {
        // Arrange
        String username = "nonexistentuser";
        when(readerRepo.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        Optional<ReaderDetails> result = readerService.findByUsername(username);

        // Assert
        assertFalse(result.isPresent());
        verify(readerRepo).findByUsername(username);
    }

    @Test
    void testFindAll_Success() {
        // Arrange
        ReaderDetails mockReader = mock(ReaderDetails.class);
        when(readerRepo.findAll()).thenReturn(Collections.singletonList(mockReader));

        // Act
        Iterable<ReaderDetails> result = readerService.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.iterator().hasNext());
        assertSame(mockReader, result.iterator().next());
        verify(readerRepo).findAll();
    }

    @Test
    void testRemoveReaderPhoto_Success() {
        // Arrange
        String readerNumber = "12345";
        long desiredVersion = 1L;

        // Mock de ReaderDetails
        ReaderDetails mockReaderDetails = mock(ReaderDetails.class);
        Photo mockPhoto = mock(Photo.class); // Supondo que você tenha uma classe Photo
        String mockPhotoFile = "mockPhotoFile.jpg";

        when(readerRepo.findByReaderNumber(readerNumber)).thenReturn(Optional.of(mockReaderDetails));
        when(mockReaderDetails.getPhoto()).thenReturn(mockPhoto);
        when(mockPhoto.getPhotoFile()).thenReturn(mockPhotoFile);
        when(readerRepo.save(mockReaderDetails)).thenReturn(mockReaderDetails);

        // Act
        Optional<ReaderDetails> result = readerService.removeReaderPhoto(readerNumber, desiredVersion);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(mockReaderDetails, result.get());

        verify(mockReaderDetails).removePhoto(desiredVersion); // Verifica se removePhoto foi chamado
        verify(readerRepo).save(mockReaderDetails); // Verifica se o repositório foi chamado para salvar o leitor atualizado
        verify(photoRepository).deleteByPhotoFile(mockPhotoFile); // Verifica se o arquivo da foto foi excluído
    }

    @Test
    void testRemoveReaderPhoto_ReaderNotFound() {
        // Arrange
        String readerNumber = "nonexistent";

        when(readerRepo.findByReaderNumber(readerNumber)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            readerService.removeReaderPhoto(readerNumber, 1L);
        });

        verify(readerRepo).findByReaderNumber(readerNumber); // Verifica se a busca pelo leitor foi feita
        verify(photoRepository, never()).deleteByPhotoFile(anyString()); // Verifica se deleteByPhotoFile não foi chamado
    }
}
