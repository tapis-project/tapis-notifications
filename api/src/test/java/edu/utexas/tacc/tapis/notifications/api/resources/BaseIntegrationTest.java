package edu.utexas.tacc.tapis.notifications.api.resources;

import org.flywaydb.core.Flyway;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test(groups = {"integration"})
public class BaseIntegrationTest {

    @BeforeTest
    public void doFlywayMigrations() {
        Flyway flyway = Flyway.configure()
            .dataSource("jdbc:postgresql://localhost:5432/test", "test", "test")
            .load();
        flyway.clean();
        flyway.migrate();
    }

}
