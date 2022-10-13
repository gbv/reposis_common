package de.gbv.reposis.metrics;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MCRMetricsProvider {
    public abstract MCRJournalMetrics getMetrics(Map<String, List<String>> metadata, Set<Integer> years);
}
