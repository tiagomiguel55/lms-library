package pt.psoft.g1.psoftg1.bootstrapping;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Profile("bootstrap")
@Order(1)
public class UserBootstrapper implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(UserBootstrapper.class);

    private final UserRepository userRepository;
    private final ReaderRepository readerRepository;
    private final GenreRepository genreRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${bootstrap.mode}")
    private String bootstrapMode;
    private List<String> queriesToExecute = new ArrayList<>();

    @Override
    @Transactional
    public void run(final String... args)  {
        try {
            logger.info("Starting UserBootstrapper...");
            createReaders();
            executeQueries();
            createLibrarian();
            logger.info("UserBootstrapper completed successfully.");
        } catch (Exception e) {
            logger.error("Error in UserBootstrapper: {}", e.getMessage(), e);
            // Don't rethrow - let the application continue
        }
    }

    private void createReaders() {
        //Reader1 - Manuel
        try {
            if (userRepository.findByUsername("manuel@gmail.com").isEmpty()) {
                final Reader manuel = Reader.newReader("manuel@gmail.com", "Xuba438976", "Manuel Sarapinto das Coives");
                userRepository.save(manuel);

                //String dateFormat = LocalDateTime.of(LocalDate.of(2024, 1, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String dateFormat = LocalDateTime.of(2024,1,20,0,0,0,0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, manuel.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);

                Optional<ReaderDetails> readerDetails1= readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/1");
                //System.out.println("GOT THIS ON READER DETAILS: " + readerDetails1.toString());
                Optional<Genre> g1 = genreRepository.findByName("Fantasia");
                Optional<Genre> g2 = genreRepository.findByName("Infantil");
                List<Genre> interestList = new ArrayList<>();
                if(g1.isPresent()) {
                    interestList.add(g1.get());
                }

                if(g2.isPresent()) {
                    interestList.add(g2.get());
                }

                if (readerDetails1.isEmpty()) {
                    ReaderDetails r1 = new ReaderDetails(
                            1,
                            manuel,
                            "2000-01-01",
                            "919191919",
                            true,
                            true,
                            true,
                            "readerPhotoTest.jpg",
                            interestList);
                    readerRepository.save(r1);
                    logger.info("Created reader Manuel with details.");
                } else {
                    logger.info("Reader details for Manuel already exist. Skipping...");
                }
            } else {
                logger.info("User manuel@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating Manuel: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Manuel: {}", e.getMessage(), e);
        }

        //Reader2 - João
        try {
            if (userRepository.findByUsername("joao@gmail.com").isEmpty()) {
                final Reader joao = Reader.newReader("joao@gmail.com", "Hupo183353", "João Ratao");
                userRepository.save(joao);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 3, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, joao.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                //jdbcTemplate.update("UPDATE PUBLIC.T_USER SET CREATED_AT = ? WHERE USERNAME = ?", LocalDateTime.of(LocalDate.of(2024, 1, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")), joao.getUsername());jdbcTemplate.update("UPDATE PUBLIC.T_USER SET CREATED_AT = ? WHERE USERNAME = ?", LocalDateTime.of(LocalDate.of(2024, 1, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")), joao.getUsername());


                Optional<ReaderDetails> readerDetails2 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/2");
                if (readerDetails2.isEmpty()) {
                    ReaderDetails r2 = new ReaderDetails(
                            2,
                            joao,
                            "1995-06-02",
                            "929292929",
                            true,
                            false,
                            false,
                            null,
                            null);
                    readerRepository.save(r2);
                    logger.info("Created reader João with details.");
                } else {
                    logger.info("Reader details for João already exist. Skipping...");
                }
            } else {
                logger.info("User joao@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating João: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating João: {}", e.getMessage(), e);
        }

        //Reader3 - Pedro
        try {
            if (userRepository.findByUsername("pedro@gmail.com").isEmpty()) {
                final Reader pedro = Reader.newReader("pedro@gmail.com", "Fana504843", "Pedro Das Cenas");
                userRepository.save(pedro);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 1, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, pedro.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                Optional<ReaderDetails> readerDetails3 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/3");
                if (readerDetails3.isEmpty()) {
                    ReaderDetails r3 = new ReaderDetails(
                            3,
                            pedro,
                            "2001-12-03",
                            "939393939",
                            true,
                            false,
                            true,
                            null,
                            null);
                    readerRepository.save(r3);
                    logger.info("Created reader Pedro with details.");
                } else {
                    logger.info("Reader details for Pedro already exist. Skipping...");
                }
            } else {
                logger.info("User pedro@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating Pedro: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Pedro: {}", e.getMessage(), e);
        }

        //Reader4 - Catarina
        try {
            if (userRepository.findByUsername("catarina@gmail.com").isEmpty()) {
                final Reader catarina = Reader.newReader("catarina@gmail.com", "Goxu222565", "Catarina Martins");
                userRepository.save(catarina);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 3, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, catarina.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                Optional<ReaderDetails> readerDetails4 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/4");
                if (readerDetails4.isEmpty()) {
                    ReaderDetails r4 = new ReaderDetails(
                            4,
                            catarina,
                            "2002-03-20",
                            "912345678",
                            true,
                            false,
                            true,
                            null,
                            null);
                    readerRepository.save(r4);
                    logger.info("Created reader Catarina with details.");
                } else {
                    logger.info("Reader details for Catarina already exist. Skipping...");
                }
            } else {
                logger.info("User catarina@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating Catarina: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Catarina: {}", e.getMessage(), e);
        }

        //Reader5 - Marcelo
        try {
            if (userRepository.findByUsername("marcelo@gmail.com").isEmpty()) {
                final Reader marcelo = Reader.newReader("marcelo@gmail.com", "Qoqu617878", "Marcelo Rebelo de Sousa");
                userRepository.save(marcelo);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 1, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, marcelo.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                Optional<ReaderDetails> readerDetails5 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/5");
                if (readerDetails5.isEmpty()) {
                    ReaderDetails r5 = new ReaderDetails(
                            5,
                            marcelo,
                            "2000-06-03",
                            "912355678",
                            true,
                            false,
                            true,
                            null,
                            null);
                    readerRepository.save(r5);
                    logger.info("Created reader Marcelo with details.");
                } else {
                    logger.info("Reader details for Marcelo already exist. Skipping...");
                }
            } else {
                logger.info("User marcelo@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating Marcelo: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Marcelo: {}", e.getMessage(), e);
        }

        //Reader6 - Luís
        try {
            if (userRepository.findByUsername("luis@gmail.com").isEmpty()) {
                final Reader luis = Reader.newReader("luis@gmail.com", "Xuka665916", "Luís Montenegro");
                userRepository.save(luis);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 3, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, luis.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                Optional<ReaderDetails> readerDetails6 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/6");
                if (readerDetails6.isEmpty()) {
                    ReaderDetails r6 = new ReaderDetails(
                            6,
                            luis,
                            "1999-03-03",
                            "912355678",
                            true,
                            false,
                            true,
                            null,
                            null);
                    readerRepository.save(r6);
                    logger.info("Created reader Luís with details.");
                } else {
                    logger.info("Reader details for Luís already exist. Skipping...");
                }
            } else {
                logger.info("User luis@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating Luís: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating Luís: {}", e.getMessage(), e);
        }

        //Reader7 - António
        try {
            if (userRepository.findByUsername("antonio@gmail.com").isEmpty()) {
                final Reader antonio = Reader.newReader("antonio@gmail.com", "Vucu659240", "António Costa");
                userRepository.save(antonio);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 6, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, antonio.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                Optional<ReaderDetails> readerDetails7 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/7");
                if (readerDetails7.isEmpty()) {
                    ReaderDetails r7 = new ReaderDetails(
                            7,
                            antonio,
                            "2001-03-03",
                            "912355778",
                            true,
                            false,
                            true,
                            null,
                            null);
                    readerRepository.save(r7);
                    logger.info("Created reader António with details.");
                } else {
                    logger.info("Reader details for António already exist. Skipping...");
                }
            } else {
                logger.info("User antonio@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating António: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating António: {}", e.getMessage(), e);
        }

        //Reader8 - André
        try {
            if (userRepository.findByUsername("andre@gmail.com").isEmpty()) {
                final Reader andre = Reader.newReader("andre@gmail.com", "Hupo118124", "André Ventura");
                userRepository.save(andre);
                String dateFormat = LocalDateTime.of(LocalDate.of(2024, 5, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, andre.getUsername());
                //jdbcTemplate.update(query);
                queriesToExecute.add(query);
                Optional<ReaderDetails> readerDetails8 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/8");
                if (readerDetails8.isEmpty()) {
                    ReaderDetails r8 = new ReaderDetails(
                            8,
                            andre,
                            "2001-03-03",
                            "912355888",
                            true,
                            false,
                            true,
                            null,
                            null);
                    readerRepository.save(r8);
                    logger.info("Created reader André with details.");
                } else {
                    logger.info("Reader details for André already exist. Skipping...");
                }
            } else {
                logger.info("User andre@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating André: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating André: {}", e.getMessage(), e);
        }
    }

    private void createLibrarian(){
        // Maria
        try {
            if (userRepository.findByUsername("maria@gmail.com").isEmpty()) {
                final User maria = Librarian.newLibrarian("maria@gmail.com", "Zabu370262", "Maria Roberta");
                userRepository.save(maria);
                logger.info("Created librarian Maria.");
            } else {
                logger.info("User maria@gmail.com already exists. Skipping...");
            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating librarian Maria: {}. Skipping...", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating librarian Maria: {}", e.getMessage(), e);
        }
    }

    private void executeQueries() {
        try {
            for (String query : queriesToExecute) {
                //jdbcTemplate.update(query);
                logger.debug("Query queued for execution: {}", query);
            }
            logger.info("All queries processed successfully.");
        } catch (Exception e) {
            logger.error("Error executing queries: {}", e.getMessage(), e);
        }
    }
}
