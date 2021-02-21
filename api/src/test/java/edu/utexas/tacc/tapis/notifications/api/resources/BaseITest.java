package edu.utexas.tacc.tapis.notifications.api.resources;

import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Site;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.glassfish.jersey.test.JerseyTestNg;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Test(groups="integration")
public abstract class BaseITest extends JerseyTestNg.ContainerPerClassTest {

    protected TenantManager tenantManager;
    protected String user1jwt;
    protected String user2jwt;
    protected Map<String, Tenant> tenantMap = new HashMap<>();

    protected Tenant tenant;
    protected Site site;

    public BaseITest() {
        super();
        tenant = new Tenant();
        tenant.setTenantId("testTenant");
        tenant.setBaseUrl("https://test.tapis.io");
        tenantMap.put(tenant.getTenantId(), tenant);
        site = new Site();
        site.setSiteId("dev");
    }

    @BeforeTest
    public void doFlywayMigrations() {
        Flyway flyway = Flyway.configure()
            .dataSource("jdbc:postgresql://localhost:5432/test", "test", "test")
            .load();
        flyway.clean();
        flyway.migrate();
    }

    @BeforeClass
    public void setUpUsers() throws Exception {
        user1jwt = IOUtils.resourceToString("/user1jwt", StandardCharsets.UTF_8);
        user2jwt = IOUtils.resourceToString("/user2jwt", StandardCharsets.UTF_8);
    }
}
