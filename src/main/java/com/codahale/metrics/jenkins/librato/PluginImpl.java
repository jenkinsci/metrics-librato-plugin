package com.codahale.metrics.jenkins.librato;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jenkins.Metrics;
import com.librato.metrics.LibratoReporter;
import hudson.Plugin;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Cyrille Le Clerc
 */
public class PluginImpl extends Plugin {
    private static final Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    private transient Map<LibratoService, LibratoReporter> reporters;

    public PluginImpl() {
        this.reporters = new LinkedHashMap<LibratoService, LibratoReporter>();
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public synchronized void stop() throws Exception {
        if (reporters != null) {
            for (LibratoReporter r : reporters.values()) {
                r.stop();
            }
            reporters.clear();
        }
    }

    @Override
    public synchronized void postInitialize() throws Exception {
        updateReporters();
    }

    public synchronized void updateReporters() throws URISyntaxException {
        if (reporters == null) {
            reporters = new LinkedHashMap<LibratoService, LibratoReporter>();
        }
        MetricRegistry registry = Metrics.metricRegistry();
        LibratoService.DescriptorImpl descriptor =
                Jenkins.getInstance().getDescriptorByType(LibratoService.DescriptorImpl.class);
        if (descriptor == null) {
            return;
        }
        String url = JenkinsLocationConfiguration.get().getUrl();
        URI uri = url == null ? null : new URI(url);
        String hostname = uri == null ? "localhost" : uri.getHost();
        Set<LibratoService> toStop = new HashSet<LibratoService>(reporters.keySet());
        for (LibratoService s : descriptor.getServices()) {
            toStop.remove(s);
            if (reporters.containsKey(s)) continue;

            String prefix = StringUtils.isBlank(s.getPrefix()) ? "jenkins" : s.getPrefix();

            String source = StringUtils.isBlank(s.getSource()) ? hostname : s.getSource();

            //
            LibratoReporter.MetricExpansionConfig metricExpansionConfig = new LibratoReporter.MetricExpansionConfig(
                    EnumSet.of(
                            LibratoReporter.ExpandedMetric.COUNT,
                            LibratoReporter.ExpandedMetric.MEDIAN,
                            LibratoReporter.ExpandedMetric.PCT_99));

            LibratoReporter r =  LibratoReporter.builder(registry, s.getUsername(), s.getToken(), source)
                    .setFilter(MetricFilter.ALL)
                    .setPrefix(prefix)
                    .setRateUnit(TimeUnit.MINUTES)
                    .setDurationUnit(TimeUnit.SECONDS)
                    .setHttpPoster(NingJenkinsHttpPoster.newPoster(s.getUsername(), s.getToken()))
                    .setExpansionConfig(metricExpansionConfig)
                    .build();
            reporters.put(s, r);
            LOGGER.log(Level.INFO, "Starting Librato reporter with username {0} and source {1}", new Object[]{
                    s.getUsername(), s.getSource()
            });
            r.start(1, TimeUnit.MINUTES);
        }
        for (LibratoService s: toStop) {
            LibratoReporter r = reporters.get(s);
            reporters.remove(s);
            r.stop();
            LOGGER.log(Level.INFO, "Stopped Librato reporter to {0} with source {1}", new Object[]{
                    s.getUsername(), StringUtils.isBlank(s.getSource()) ? hostname : s.getSource()
            });
        }
    }
}
