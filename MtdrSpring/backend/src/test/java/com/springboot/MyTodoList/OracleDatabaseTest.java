package com.springboot.MyTodoList;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.MountableFile;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.service.SprintService;

@SpringBootTest
@Testcontainers
public class OracleDatabaseTest {
    static String image = "gvenzl/oracle-free:23.6-slim-faststart";

    @Container
    static OracleContainer oracleContainer = new OracleContainer(image)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withUsername("testuser")
            .withPassword(("testpwd"));

    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracleContainer::getJdbcUrl);
        registry.add("spring.datasource.username", oracleContainer::getUsername);
        registry.add("spring.datasource.password", oracleContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.Oracle12cDialect");
    }

    @BeforeAll
    public static void setUp() throws Exception {
        // Load the init.sql script from the classpath. Any file may be used.
        MountableFile sqlFile = MountableFile.forClasspathResource("schema.sql");
        oracleContainer.copyFileToContainer(sqlFile, "/tmp/schema.sql");
        // Run the init.sql script as sysdba on the database container.
        oracleContainer.execInContainer("bash", "-c", "echo exit | sqlplus testuser/testpwd @/tmp/schema.sql");

    }

    @Autowired
    DataSource dataSource;

    @Autowired
    private SprintService sprintService;

    @Test
    void testCreateAndFindSprint() {
        Sprint sprint = new Sprint();
        sprint.setName("Sprint Test");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(14));
        sprint.setStatus("PLANNED");
        sprint.setCreatedBy(1);
        sprint.setCreationTs(OffsetDateTime.now());

        Sprint savedSprint = sprintService.createSprint(sprint);
        assertThat(savedSprint.getId()).isGreaterThan(0);

        List<Sprint> allSprints = sprintService.findAll();
        assertThat(allSprints).isNotEmpty();
        assertThat(allSprints).extracting("name").contains("Sprint Test");
    }

    @Test
    void testUpdateSprintStatus() {
        // Primero creamos un sprint
        Sprint sprint = new Sprint();
        sprint.setName("Sprint Status Test");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(7));
        sprint.setStatus("PLANNED");
        sprint.setCreatedBy(2);
        sprint.setCreationTs(OffsetDateTime.now());

        Sprint savedSprint = sprintService.createSprint(sprint);

        // Actualizamos estado a ACTIVE
        Sprint updatedSprint = sprintService.updateStatus(savedSprint.getId(), "ACTIVE");

        assertThat(updatedSprint).isNotNull();
        assertThat(updatedSprint.getStatus()).isEqualTo("ACTIVE");
    }
    
   
}