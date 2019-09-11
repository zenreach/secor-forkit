package com.pinterest.secor.monitoring;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import io.prometheus.client.hotspot.DefaultExports;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrometheusMetricsCollector implements MetricCollector {
    static Map<String, Counter> counters = new ConcurrentHashMap<>();
    static Map<String, Summary> summaries = new ConcurrentHashMap<>();
    static Map<String, Gauge> gauges = new ConcurrentHashMap<>();

    static {
        DefaultExports.initialize();
    }

    @Override
    public void increment(String label, String topic) {
        synchronized (counters) {
            if (!counters.containsKey(label))
                counters.put(label, Counter.build().name(label).help(label).labelNames("topic").register());
            Counter c = counters.get(label);
            c.labels(topic).inc();
        }
    }

    @Override
    public void increment(String label, int delta, String topic) {
        synchronized (counters) {
            if (!counters.containsKey(label))
                counters.put(label, Counter.build().name(label).help(label).labelNames("topic").register());
            Counter c = counters.get(label);
            c.labels(topic).inc(delta);
        }
    }

    @Override
    public void metric(String label, double value, String topic) {
        synchronized (summaries) {
            if (!summaries.containsKey(label))
                summaries.put(label, Summary.build().name(label).help(label).labelNames("topic").register());
            Summary s = summaries.get(label);
            s.labels(topic).observe(value);
        }
    }

    @Override
    public void gauge(String label, double value, String topic) {
        synchronized (gauges) {
            if (!gauges.containsKey(label))
                gauges.put(label, Gauge.build().name(label).help(label).labelNames("topic").register());
            Gauge g = gauges.get(label);
            g.labels(topic).inc(value);
        }
    }
}
