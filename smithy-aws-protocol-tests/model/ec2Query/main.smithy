// EC2 serialization is very similar to aws.query serialization.
//
// The differences for input are:
//
// 1. EC2 does not support the xmlFlattened trait on input. All lists are flattened.
// 2. EC2 does not utilize input maps.
// 3. EC2 uses a trait called aws.protocols#ec2QueryName that is used when serializing
//    query string input parameters. If this trait is found, then it is used.
//    This trait has no effect on output serialization.
// 4. EC2 input parameters not marked with aws.protocols#ec2QueryName fall back to
//    using xmlName. If xmlName is set, then the first letter is uppercased
//    and serialized in the query.
// 5. Because lists are always considered flattened, the member name of a list,
//    the xmlName on a list, and ec2QueryName on a list have no effect on how
//    a list is serialized.
//
// The differences for output are:
//
// 1. Unlike aws.query, there's no result wrapper.
// 2. EC2 does not utilize output maps.
// 3. requestId is a child of the root node. It's not nested in some
//    ResponseMetadata element like aws.query.
//
// EC2 errors have an additional level of nesting. See xml-errors.smithy
// for details.

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.api#service
use aws.auth#sigv4
use aws.protocols#ec2Query
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// An EC2 query service that sends query requests and XML responses.
@service(sdkId: "EC2 Protocol")
@suppress(["SigV4Traits"])
@sigv4(name: "ec2query")
@ec2Query
@xmlNamespace(uri: "https://example.com/")
@title("Sample Ec2 Protocol Service")
service AwsEc2 {
    version: "2020-01-08",
    operations: [
        // Basic input and output tests
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,

        // Input tests
        SimpleInputParams,
        QueryTimestamps,
        NestedStructures,
        QueryLists,
        QueryIdempotencyTokenAutoFill,

        // Output tests
        XmlEmptyBlobs,

        // Output XML list tests
        XmlLists,
        XmlEmptyLists,

        // Output XML structure tests
        SimpleScalarXmlProperties,
        XmlBlobs,
        XmlTimestamps,
        XmlEnums,
        XmlIntEnums,
        RecursiveXmlShapes,
        RecursiveXmlShapes,
        IgnoresWrappingXmlName,
        XmlNamespaces,

        // Output error tests
        GreetingWithErrors,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,

        // custom endpoints with paths
        HostWithPathOperation,

        // client-only timestamp parsing tests
        DatetimeOffsets,
        FractionalSeconds,

        // requestCompression trait tests
        PutWithContentEncoding
    ]
}
