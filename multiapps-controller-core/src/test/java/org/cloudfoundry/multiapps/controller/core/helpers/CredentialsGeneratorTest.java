package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

class CredentialsGeneratorTest {

    @Test
    void testGenerate() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom randomGenerator = SecureRandom.getInstance("SHA1PRNG", "SUN");
        randomGenerator.setSeed(69);

        CredentialsGenerator credentialsGenerator = new CredentialsGenerator(randomGenerator);
        assertEquals("r&k37&tl2*D[MK7C", credentialsGenerator.next(16));
        assertEquals("dd*Qz99LI1CB49(*", credentialsGenerator.next(16));
        assertEquals("4y(m4ZMfpcY8#g7ur74K@@vXmAAJ4s2(", credentialsGenerator.next(32));
    }

}
