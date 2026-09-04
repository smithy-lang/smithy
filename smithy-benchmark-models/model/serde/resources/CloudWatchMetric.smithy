$version: "2"

namespace smithy.benchmark.serde

resource CloudWatchMetric {
    operations: [
        PutMetricData
        GetMetricData
    ]
}
