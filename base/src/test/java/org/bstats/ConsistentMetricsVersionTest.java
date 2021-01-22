package org.bstats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsistentMetricsVersionTest {

    /**
     * Makes sure that the version constant {@link MetricsBase#METRICS_VERSION} is consistent
     * with the version in the {@code gradle.properties} file.
     */
    @Test
    public void testConsistentMetricsVersion() {
        // The environment variable is set by the Gradle script
        String versionInGradleProperties = System.getProperty("metrics-version");
        assertNotNull(versionInGradleProperties, "Failed to find metrics version");
        assertEquals(versionInGradleProperties, MetricsBase.METRICS_VERSION);
    }

}