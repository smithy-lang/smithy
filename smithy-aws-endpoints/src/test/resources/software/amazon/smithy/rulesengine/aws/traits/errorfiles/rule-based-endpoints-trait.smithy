$version: "2"

namespace smithy.example

use aws.endpoints#rulesBasedEndpoints

@rulesBasedEndpoints
service MyService {
    version: "2020-04-02"
}
