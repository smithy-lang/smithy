$version: "2"

namespace smithy.example

service MyService {
  version: "foo",
  operations: [Missing1, smithy.api#String],
  resources: [Missing1, smithy.api#String],
}
