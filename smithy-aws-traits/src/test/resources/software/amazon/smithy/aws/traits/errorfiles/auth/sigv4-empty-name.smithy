$version: "2.0"

namespace smithy.example

use aws.auth#sigv4

@sigv4(name: "")
service InvalidService {
    version: "2020-07-02"
}
