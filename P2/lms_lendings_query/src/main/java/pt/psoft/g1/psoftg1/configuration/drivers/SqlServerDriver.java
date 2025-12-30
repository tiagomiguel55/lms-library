package pt.psoft.g1.psoftg1.configuration.drivers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@Profile("sqlserver1")
public class SqlServerDriver {

    @Value("${spring.data.SqlServer.url}")
    private String url;

    @Value("${spring.data.SqlServer.username}")
    private String username;

    @Value("${spring.data.SqlServer.password}")
    private String password;

    @Value("${spring.data.SqlServer.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource sqlServerDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
