$version: "2.0"

namespace smithy.example

@unstableFeatures(
    VALID_SINCE_PREVIEW: { reason: "PREVIEW", since: "2026-10-08" }
    INVALID_SINCE_PREVIEW: { reason: "PREVIEW", since: "Oct 8, 2026" }
)
service MyService {
    version: "2020-01-01"
    operations: [
        ValidSinceOperation
        InvalidSinceOperation
    ]
}

@unstable(featureId: "VALID_SINCE_PREVIEW")
operation ValidSinceOperation {}

@unstable(featureId: "INVALID_SINCE_PREVIEW")
operation InvalidSinceOperation {}
