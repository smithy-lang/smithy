// Subscribe operations must define output structures.

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {}
