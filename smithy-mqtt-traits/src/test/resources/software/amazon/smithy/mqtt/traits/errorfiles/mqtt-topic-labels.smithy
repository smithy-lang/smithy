$version: "2.0"

namespace smithy.example

// Missing input for {foo} property.
@smithy.mqtt#publish("events1/{foo}")
operation Operation1 {}


// Missing {foo} member.
@smithy.mqtt#publish("events2/{foo}")
operation Operation2 {
    input: Operation2Input,
    output: Unit
}

@input
structure Operation2Input {
    baz: smithy.api#String,
}

// Extraneous {baz} label member.
@smithy.mqtt#publish("events3/{foo}")
operation Operation3 {
    input: Operation3Input,
    output: Unit
}

@input
structure Operation3Input {
    @required
    @smithy.mqtt#topicLabel
    foo: smithy.api#String,

    @required
    @smithy.mqtt#topicLabel
    baz: smithy.api#String,
}

// Missing topicLabel trait for {foo}
@smithy.mqtt#publish("events4/{foo}")
operation Operation4 {
    input: Operation4Input,
    output: Unit
}

@input
structure Operation4Input {
    @required
    foo: smithy.api#String,
}

// No errors.
@smithy.mqtt#publish("events5/{foo}")
operation Operation5 {
    input: Operation5Input
}

@input
structure Operation5Input {
    @required
    @smithy.mqtt#topicLabel
    foo: smithy.api#String,
}
