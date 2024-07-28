package org.bstats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RelocateCheckTest {

    /**
     * If the {@code bstats.relocatecheck} is not set to {@code false}, calling the constructor should throw
     * an exception (because the class was not relocated for the test).
     */
    @Test
    public void shouldEnforceRelocation() {
        System.getProperties().remove("bstats.relocatecheck");
        assertThrows(IllegalStateException.class, this::createDummyMetricsBase);
        System.getProperties().setProperty("bstats.relocatecheck", "false");
    }

    @Test
    public void shouldRespectDisabledRelocateCheck() {
        // The bstats.relocatecheck system property is set for all tests by Gradle
        assertDoesNotThrow(this::createDummyMetricsBase);
    }

    private MetricsBase createDummyMetricsBase() {
        return new MetricsBase("", "", -1, false, null, null, null, null, null, null, true, true, true, false);
    }

}
