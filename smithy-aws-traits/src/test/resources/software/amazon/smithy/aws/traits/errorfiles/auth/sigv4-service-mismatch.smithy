$version: "2.0"

namespace smithy.example

use aws.api#service
use aws.auth#sigv4

@service(sdkId: "servicename")
@sigv4(name: "signingname")
service InvalidService {
    version: "2020-07-02"
}
