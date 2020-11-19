package edu.utexas.tacc.tapis.notifications.lib.config;

import java.util.Map;

public class RuntimeSettings {

    private static final Map<String, String> settings = System.getenv();

    static class BaseConfig implements IRuntimeConfig {

        protected String siteId = settings.getOrDefault("TAPIS_SITE_ID", "tacc");
        protected String dbHost = settings.getOrDefault("DB_HOST", "localhost");
        protected String dbName = settings.getOrDefault("DB_NAME", "dev");
        protected String dbUsername = settings.getOrDefault("DB_USERNAME", "dev");
        protected String dbPassword = settings.getOrDefault("DB_PASSWORD", "dev");
        protected String dbPort = settings.getOrDefault("DB_PORT", "5432");
        protected String rabbitMQUsername = settings.getOrDefault("RABBITMQ_USERNAME", "dev");
        protected String rabbitMQVHost = settings.getOrDefault("RABBITMQ_VHOST", "dev");
        protected String rabbitmqPassword = settings.getOrDefault("RABBITMQ_PASSWORD", "dev");
        protected String servicePassword = settings.getOrDefault("SERVICE_PASSWORD", "dev");
        protected String tokensServiceURL = settings.getOrDefault("TOKENS_SERVICE_URL", "https://dev.develop.tapis.io");
        protected String tenantsServiceURL = settings.getOrDefault("TENANTS_SERVICE_URL", "https://dev.develop.tapis.io");

        public String getDbHost() {
            return dbHost;
        }

        public String getDbName() {
            return dbName;
        }

        public String getDbUsername() {
            return dbUsername;
        }

        public String getDbPassword() {
            return dbPassword;
        }

        public String getDbPort() {
            return dbPort;
        }

        public String getRabbitMQUsername() {
            return rabbitMQUsername;
        }

        public String getRabbitMQVHost() {
            return rabbitMQVHost;
        }

        public String getRabbitmqPassword() {
            return rabbitmqPassword;
        }

        public String getServicePassword() { return servicePassword; }

        public String getTokensServiceURL() { return tokensServiceURL; }

        public String getTenantsServiceURL() { return tenantsServiceURL; }

        public String getSiteId() { return siteId; }

    }


    private static class TestConfig extends BaseConfig {
        protected String dbHost = settings.getOrDefault("DB_HOST", "localhost");
        protected String dbName = "test";
        protected String dbUsername = "test";
        protected String dbPassword = "test";
        protected String dbPort = "5432";

        @Override
        public String getDbName() { return dbName; }

        @Override
        public String getDbHost() {
            return dbHost;
        }

        @Override
        public String getDbUsername() {
            return dbUsername;
        }

        @Override
        public String getDbPassword() {
            return dbPassword;
        }

        @Override
        public String getDbPort() {
            return dbPort;
        }
    }

    public static IRuntimeConfig get() {
        if (settings.getOrDefault("APP_ENV", "dev").equalsIgnoreCase("dev")) {
            return new BaseConfig();
        } else if (settings.getOrDefault("APP_ENV", "dev").equalsIgnoreCase("test")) {
            return new TestConfig();
        } else {
            //TODO:
            return new BaseConfig();
        }
    }

}

