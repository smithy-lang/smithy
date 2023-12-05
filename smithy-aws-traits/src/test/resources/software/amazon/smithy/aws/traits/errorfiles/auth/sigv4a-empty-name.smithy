$version: "2.0"

namespace smithy.example

use aws.auth#sigv4
use aws.auth#sigv4a

@auth([sigv4a, sigv4])
@sigv4(name: "signingname")
@sigv4a(name: "")
service InvalidService {
    version: "2020-07-02"
}
