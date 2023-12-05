$version: "2.0"

namespace smithy.example

use aws.auth#sigv4a

@sigv4a(name: "signingname")
service InvalidService {
    version: "2020-07-02"
}
