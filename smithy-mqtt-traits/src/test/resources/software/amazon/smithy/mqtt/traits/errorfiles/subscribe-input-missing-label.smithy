// subscribe operations do not support input. This test
// should detect that members of the input of the operation are
// missing @smithy.mqtt#topicLabel traits.
$version: "2.0"

namespace smithy.example

use smithy.mqtt#topicLabel
use smithy.mqtt#subscribe

@subscribe("events/{foo}")
operation Foo {
    input: FooInput,
    output: FooOutput
}

@input
structure FooInput {
    @required
    @topicLabel
    foo: smithy.api#String,

    baz: smithy.api#String, // Error, missing topicLabel.
}

@output
structure FooOutput {
    messages: EventStream,
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {}
