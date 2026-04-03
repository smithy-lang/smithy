$version: "2.0"

namespace ns.foo

use smithy.api#streaming
use aws.api#awsChunked

@streaming
@awsChunked
blob StreamingBlob
