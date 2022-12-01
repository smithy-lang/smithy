$version: "1.0"

namespace ns.test

use smithy.test#test

@test()
service Weather {
    version: "2022-05-24",
    operations: [GetCurrentTime]
}

@readonly
operation GetCurrentTime {
  input: GetCurrentTimeInput,
  output: GetCurrentTimeOutput
}

@input
structure GetCurrentTimeInput {}

@output
structure GetCurrentTimeOutput {
    @required
    time: Timestamp,
}
