package pt.psoft.g1.psoftg1.bootstrapping;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    private final UserRepository userRepository;
    private final ReaderRepository readerRepository;
    private final JdbcTemplate jdbcTemplate;
    private List<String> queriesToExecute = new ArrayList<>();

    @Override
    @Transactional
    public void run(final String... args)  {
        createReaders();
        createLibrarian();
        executeQueries();
    }

    private void createReaders() {
        //Reader1 - Manuel
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




            if (readerDetails1.isEmpty()) {
                ReaderDetails r1 = new ReaderDetails(
                        1,
                        manuel);
                readerRepository.save(r1);

            }
        }

        //Reader2 - João
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
                        joao);
                readerRepository.save(r2);
            }
        }

        //Reader3 - Pedro
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
                        pedro);
                readerRepository.save(r3);
            }
        }

        //Reader4 - Catarina
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
                        catarina);
                readerRepository.save(r4);
            }
        }

        //Reader5 - Marcelo
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
                        marcelo);
                readerRepository.save(r5);
            }
        }

        //Reader6 - Luís
        if (userRepository.findByUsername("luis@gmail.com").isEmpty()) {
            final Reader luis = Reader.newReader("luis@gmail.com", "Xuka665916", "Luís Montenegro");
            userRepository.save(luis);
            String dateFormat = LocalDateTime.of(LocalDate.of(2024, 3, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
            String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, luis.getUsername());
            //jdbcTemplate.update(query);
            queriesToExecute.add(query);
            Optional<ReaderDetails> readerDetails5 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/6");
            if (readerDetails5.isEmpty()) {
                ReaderDetails r6 = new ReaderDetails(
                        6,
                        luis);
                readerRepository.save(r6);
            }
        }

        //Reader7 - António
        if (userRepository.findByUsername("antonio@gmail.com").isEmpty()) {
            final Reader antonio = Reader.newReader("antonio@gmail.com", "Vucu659240", "António Costa");
            userRepository.save(antonio);
            String dateFormat = LocalDateTime.of(LocalDate.of(2024, 6, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
            String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, antonio.getUsername());
            //jdbcTemplate.update(query);
            queriesToExecute.add(query);
            Optional<ReaderDetails> readerDetails5 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/7");
            if (readerDetails5.isEmpty()) {
                ReaderDetails r7 = new ReaderDetails(
                        7,
                        antonio);
                readerRepository.save(r7);
            }
        }

        //Reader8 - André
        if (userRepository.findByUsername("andre@gmail.com").isEmpty()) {
            final Reader andre = Reader.newReader("andre@gmail.com", "Hupo118124", "André Ventura");
            userRepository.save(andre);
            String dateFormat = LocalDateTime.of(LocalDate.of(2024, 5, 20), LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
            String query = String.format("UPDATE DEFAULT.T_USER SET CREATED_AT = '%s' WHERE USERNAME = '%s'", dateFormat, andre.getUsername());
            //jdbcTemplate.update(query);
            queriesToExecute.add(query);
            Optional<ReaderDetails> readerDetails5 = readerRepository.findByReaderNumber(LocalDate.now().getYear() + "/8");
            if (readerDetails5.isEmpty()) {
                ReaderDetails r5 = new ReaderDetails(
                        8,
                        andre);
                readerRepository.save(r5);
            }
        }
    }

    private void createLibrarian(){
        // Maria
        if (userRepository.findByUsername("maria@gmail.com").isEmpty()) {
                final User maria = Librarian.newLibrarian("maria@gmail.com", "Zabu370262", "Maria Roberta");
            userRepository.save(maria);
        }
    }

    private void executeQueries() {
        for (String query : queriesToExecute) {
            //jdbcTemplate.update(query);
        }
    }
}
