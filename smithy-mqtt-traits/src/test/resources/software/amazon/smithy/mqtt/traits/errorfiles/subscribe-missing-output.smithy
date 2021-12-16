// Subscribe operations must define output structures that contain an event stream.
$version: "2.0"

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {}
