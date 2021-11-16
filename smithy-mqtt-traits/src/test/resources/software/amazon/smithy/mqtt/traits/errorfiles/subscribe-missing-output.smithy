// Subscribe operations must define output structures that contain an event stream.

namespace smithy.example

@smithy.mqtt#subscribe("events")
@suppress(["OperationMissingInput", "OperationMissingOutput"])
operation Foo {}
