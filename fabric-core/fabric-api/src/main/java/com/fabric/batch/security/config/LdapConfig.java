package com.fabric.batch.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * LDAP Infrastructure Configuration
 *
 * Provides LdapContextSource and LdapTemplate beans when LDAP is enabled.
 * Only active in non-local profiles (local profile uses LocalSecurityConfig).
 */
@Slf4j
@Configuration
@Profile("!local")
@ConditionalOnProperty(name = "fabric.security.ldap.enabled", havingValue = "true")
public class LdapConfig {

    @Value("${fabric.security.ldap.url:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${fabric.security.ldap.base-dn:dc=fabric,dc=local}")
    private String baseDn;

    @Value("${fabric.security.ldap.manager-dn:cn=admin,dc=fabric,dc=local}")
    private String managerDn;

    @Value("${fabric.security.ldap.manager-password}")
    private String managerPassword;

    @Value("${fabric.security.ldap.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${fabric.security.ldap.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(baseDn);
        contextSource.setUserDn(managerDn);
        contextSource.setPassword(managerPassword);
        contextSource.setPooled(true);
        contextSource.setBaseEnvironmentProperties(java.util.Map.of(
            "com.sun.jndi.ldap.connect.timeout", String.valueOf(connectionTimeout),
            "com.sun.jndi.ldap.read.timeout", String.valueOf(readTimeout)
        ));
        log.info("Configured LDAP context source: url={}, baseDn={}", ldapUrl, baseDn);
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(ldapContextSource());
    }
}
