// mqttSubscribe operations must have an event stream in its output.

namespace smithy.example

@mqttSubscribe("events")
operation Foo() -> FooOutput

structure FooOutput {}
