package com.mycompany.jsf.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void verifiesLegacyPasswordsAndMigratableBcryptHashes() {
        char[] password = "correct horse battery".toCharArray();
        try {
            assertTrue(PasswordHasher.verify(password, "correct horse battery"));
            assertFalse(PasswordHasher.verify(password, "different password"));

            String hash = PasswordHasher.hash(password);
            assertTrue(PasswordHasher.isBcryptHash(hash));
            assertTrue(PasswordHasher.verify(password, hash));
            assertFalse(PasswordHasher.verify("wrong password".toCharArray(), hash));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void bootstrapSqlHashesPasswordAndEscapesUserSuppliedValues() {
        char[] password = "initial admin password".toCharArray();
        try {
            String sql = PasswordHashGenerator.createInsertSql(
                    "Admin O'Neil", "Admin@Example.local", password);

            assertTrue(sql.contains("'Admin O''Neil'"));
            assertTrue(sql.contains("'admin@example.local'"));
            assertTrue(sql.contains("'ADMIN', TRUE, CURRENT_TIMESTAMP"));
            assertFalse(sql.contains(new String(password)));

            int hashStart = sql.indexOf("'$2a$") + 1;
            int hashEnd = sql.indexOf('\'', hashStart);
            assertTrue(PasswordHasher.verify(password, sql.substring(hashStart, hashEnd)));
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
