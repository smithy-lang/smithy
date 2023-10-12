$version: "2.0"

namespace ns.foo

use aws.endpoints#dualStackOnlyEndpoints

@dualStackOnlyEndpoints
service Service1 {
    version: "2021-06-29"
}
