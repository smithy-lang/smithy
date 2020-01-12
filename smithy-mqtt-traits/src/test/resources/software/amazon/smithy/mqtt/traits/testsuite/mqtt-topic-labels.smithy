namespace smithy.example

// Missing input for {foo} property.
@smithy.mqtt#publish("events1/{foo}")
operation Operation1 {}


// Missing {foo} member.
@smithy.mqtt#publish("events2/{foo}")
operation Operation2 {
    input: Operation2Input
}

structure Operation2Input {
  baz: smithy.api#String,
}


// Extraneous {baz} label member.
@smithy.mqtt#publish("events3/{foo}")
operation Operation3 {
    input: Operation3Input
}

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
    input: Operation4Input
}

structure Operation4Input {
  @required
  foo: smithy.api#String,
}


// No errors.
@smithy.mqtt#publish("events5/{foo}")
operation Operation5 {
    input: Operation5Input
}

structure Operation5Input {
  @required
  @smithy.mqtt#topicLabel
  foo: smithy.api#String,
}
