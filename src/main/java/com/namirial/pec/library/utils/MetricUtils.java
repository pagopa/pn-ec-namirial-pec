package com.namirial.pec.library.utils;

import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.util.Collections;

public class MetricUtils {

    private MetricUtils() { throw new IllegalStateException("MetricUtils is a utility class"); }

    public static PutMetricDataRequest createMetricDataRequest(String metricName, Dimension dimension, String namespace, double value) {
        MetricDatum.Builder builder = MetricDatum.builder().metricName(metricName).value(value).unit(StandardUnit.COUNT).timestamp(Instant.now());
        if (dimension != null) builder.dimensions(dimension);
        return PutMetricDataRequest.builder().namespace(namespace).metricData(Collections.singletonList(builder.build())).build();
    }

    public static PutMetricDataRequest createMetricDataRequest(String metricName, String namespace, double value) {
        return createMetricDataRequest(metricName, null, namespace, value);
    }

}
