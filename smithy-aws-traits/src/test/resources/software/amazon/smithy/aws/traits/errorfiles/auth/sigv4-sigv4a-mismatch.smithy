$version: "2.0"

namespace smithy.example

use aws.auth#sigv4
use aws.auth#sigv4a

@auth([sigv4a, sigv4])
@sigv4(name: "sigv4signingname")
@sigv4a(name: "sigv4asigningname")
service InvalidService {
    version: "2020-07-02"
}
