$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.api#service
use aws.protocols#restJson1

/// A REST JSON service that sends JSON requests and responses with validation applied
@service(sdkId: "Rest Json Validation Protocol")
@restJson1
service RestJsonValidation {
    version: "2021-08-19",
    operations: [
        MalformedEnum,
        MalformedLength,
        MalformedLengthOverride,
        MalformedLengthQueryString,
        MalformedPattern,
        MalformedPatternOverride,
        MalformedRange,
        MalformedRangeOverride,
        MalformedRequired,
        MalformedUniqueItems,
        RecursiveStructures,
        SensitiveValidation
    ]
}
