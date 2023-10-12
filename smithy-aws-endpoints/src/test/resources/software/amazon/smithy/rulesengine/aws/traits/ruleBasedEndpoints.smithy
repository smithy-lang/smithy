$version: "2.0"

namespace ns.foo

use aws.endpoints#rulesBasedEndpoints

@rulesBasedEndpoints
service Service1 {
    version: "2021-06-29"
}
