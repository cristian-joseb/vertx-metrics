package org.example;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.impl.VertxMetricsFactoryImpl;

import java.util.Set;
import java.util.regex.Pattern;

public class ServerMetrics {

  private final PrometheusMeterRegistry registry =
    new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry,
      Clock.SYSTEM);

  private final VertxMetricsFactory vertxMetricsFactory = new VertxMetricsFactoryImpl();

  public MetricsOptions initializeMetricsOptions() {
    setupMetricsHistogramBuckets();
    removeUselessMetrics();

    final MetricsOptions metricsOptions = new MicrometerMetricsOptions()
      .setPrometheusOptions(
        new VertxPrometheusOptions()
          .setEnabled(true)
          .setPublishQuantiles(false)
      )
      .setLabels(Set.of(
        Label.REMOTE,
        Label.HTTP_CODE,
        Label.HTTP_PATH,
        Label.CLASS_NAME,
        Label.EB_ADDRESS,
        Label.EB_FAILURE,
        Label.POOL_TYPE,
        Label.POOL_NAME
      ))
      .setMicrometerRegistry(registry)
      .setJvmMetricsEnabled(true)
      .setFactory(vertxMetricsFactory)
      .setEnabled(true);
    return metricsOptions;
  }

  private void removeUselessMetrics() {
    final Pattern simpleFunctionPattern = Pattern.compile(".*\\.v[0-9]+");

    registry.config().meterFilter(
      new MeterFilter() {
        @Override
        public MeterFilterReply accept(final Meter.Id id) {
          MeterFilterReply result = MeterFilterReply.NEUTRAL;

          String metricName = id.getName();
          if (metricName.startsWith("vertx.http.server.responseTime")
            && !id.getTag("code").equals("200")) {
            result = MeterFilterReply.DENY;
          } else if (
            "vertx.http.server.requests".equals(metricName)
              || "vertx.http.server.requestCount".equals(metricName)
              || "vertx.http.client.requestCount".equals(metricName)) {
            String path = id.getTag("path");
            result = MeterFilterReply.DENY;
            if (path != null && !simpleFunctionPattern.matcher(path).matches()) {
              result = MeterFilterReply.DENY;
            }
          }

          return result;
        }
      });
  }

  private void setupMetricsHistogramBuckets() {
    final long milliseconds = 1000 * 1000L;
    final long bytes = 1L;

    registry.config().meterFilter(
      new MeterFilter() {
        @Override
        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
          DistributionStatisticConfig.Builder statisticConfigBuilder = DistributionStatisticConfig.builder()
            .percentilesHistogram(false);

          if (id.getName().contains("bytes")) {
            statisticConfigBuilder.sla(
              50 * bytes,
              250 * bytes,
              1000 * bytes,
              4000 * bytes,
              16000 * bytes,
              64000 * bytes,
              256000 * bytes
            );
          } else {
            statisticConfigBuilder.sla(
              2 * milliseconds,
              5 * milliseconds,
              10 * milliseconds,
              20 * milliseconds,
              30 * milliseconds,
              60 * milliseconds,
              120 * milliseconds,
              250 * milliseconds,
              500 * milliseconds,
              1000 * milliseconds,
              2000 * milliseconds,
              4000 * milliseconds
            );
          }
          return statisticConfigBuilder.build().merge(config);
        }
      });
  }
}
