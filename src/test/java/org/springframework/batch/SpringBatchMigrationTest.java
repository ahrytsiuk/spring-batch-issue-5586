package org.springframework.batch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbDataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@Testcontainers(disabledWithoutDocker = true)
public class SpringBatchMigrationTest {

    @Container
    public MariaDBContainer mariadb = new MariaDBContainer(DockerImageName.parse("mariadb:11.8.2"));

    @Test
    void migrationWithSpringBatchScript_FAIL() throws Exception {
        try (var resolver = new MavenArtifactResolver()) {
            var initSchema = new ByteArrayResource(resolver.getResourceContent(
                    "org.springframework.batch:spring-batch-core:5.2.2",
                    "org/springframework/batch/core/schema-mysql.sql"
            ));
            var migration = new ByteArrayResource(resolver.getResourceContent(
                    "org.springframework.batch:spring-batch-core:6.0.5",
                    "org/springframework/batch/core/migration/6.0/migration-mariadb.sql"
            ));

            MariaDbDataSource datasource = createDataSource();
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
            databasePopulator.addScript(initSchema);
            databasePopulator.addScript(migration);
            Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
        }
    }

    @Test
    void migrationWithCustomScript_PASS() throws Exception {
        try (var resolver = new MavenArtifactResolver()) {
            var initSchema = new ByteArrayResource(resolver.getResourceContent(
                    "org.springframework.batch:spring-batch-core:5.2.2",
                    "org/springframework/batch/core/schema-mysql.sql"
            ));
            var migration = new ByteArrayResource("""
                    RENAME TABLE BATCH_JOB_SEQ TO BATCH_JOB_INSTANCE_SEQ;
                    """.getBytes(StandardCharsets.UTF_8)
            );

            MariaDbDataSource datasource = createDataSource();
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
            databasePopulator.addScript(initSchema);
            databasePopulator.addScript(migration);
            Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
        }
    }

    private MariaDbDataSource createDataSource() throws SQLException {
        MariaDbDataSource datasource = new MariaDbDataSource();
        datasource.setUrl(mariadb.getJdbcUrl());
        datasource.setUser(mariadb.getUsername());
        datasource.setPassword(mariadb.getPassword());
        return datasource;
    }
}
