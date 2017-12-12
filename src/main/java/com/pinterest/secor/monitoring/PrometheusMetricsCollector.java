package com.pinterest.secor.monitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import io.prometheus.client.hotspot.DefaultExports;

import java.util.HashMap;
import java.util.Map;

public class PrometheusMetricsCollector implements MetricCollector {
    static final Map<String, Counter> counters = new HashMap<String, Counter>();
    static final Map<String, Summary> summaries = new HashMap<String, Summary>();
    static final Map<String, Gauge> gauges = new HashMap<String, Gauge>();

    public PrometheusMetricsCollector() {
        DefaultExports.initialize();
    }

    @Override
    public void increment(String label, String topic) {
        if (!counters.containsKey(label))
            counters.put(label, Counter.build().name(label).labelNames("topic").register());
        Counter c = counters.get(label);
        c.inc();
    }

    @Override
    public void increment(String label, int delta, String topic) {
        if (!counters.containsKey(label))
            counters.put(label, Counter.build().name(label).labelNames("topic").register());
        Counter c = counters.get(label);
        c.inc(delta);
    }

    @Override
    public void metric(String label, double value, String topic) {
        if (!summaries.containsKey(label))
            summaries.put(label, Summary.build().name(label).labelNames("topic").register());
        Summary s = summaries.get(label);
        s.observe(value);
    }

    @Override
    public void gauge(String label, double value, String topic) {
        if (!gauges.containsKey(label))
            gauges.put(label, Gauge.build().name(label).labelNames("topic").register());
        Gauge g = gauges.get(label);
        g.inc(value);
    }
}
