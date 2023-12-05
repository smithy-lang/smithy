$version: "2.0"

namespace smithy.example

use aws.api#service
use aws.auth#sigv4
use aws.auth#sigv4a

@service(sdkId: "servicename")
@auth([sigv4a, sigv4])
@sigv4(name: "invalidservice")
@sigv4a(name: "signingname")
service InvalidService {
    version: "2020-07-02"
}
