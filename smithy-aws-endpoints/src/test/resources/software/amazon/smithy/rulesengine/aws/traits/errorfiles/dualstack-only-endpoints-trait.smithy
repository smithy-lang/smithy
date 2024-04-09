$version: "2"

namespace smithy.example

use aws.endpoints#dualStackOnlyEndpoints

@dualStackOnlyEndpoints
service MyService {
    version: "2020-04-02"
}
