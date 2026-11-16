package com.mycompany.jsf.security;

import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public final class EcheanJaasConfiguration extends Configuration {

    public static final String APPLICATION_NAME = "EcheanLogin";
    private static final String DATA_SOURCE_JNDI_NAME =
            "java:jboss/datasources/echeanDS";

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!APPLICATION_NAME.equals(name)) {
            return null;
        }
        return new AppConfigurationEntry[]{
            new AppConfigurationEntry(
                    DatabaseLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    Map.of("dataSourceJndiName", DATA_SOURCE_JNDI_NAME))
        };
    }
}
