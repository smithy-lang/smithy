namespace smithy.example

// Missing input for {foo} property.
@mqttPublish("events1/{foo}")
operation Operation1()


// Missing {foo} member.
@mqttPublish("events2/{foo}")
operation Operation2(Operation2Input)

structure Operation2Input {
  baz: smithy.api#String,
}


// Extraneous {baz} label member.
@mqttPublish("events3/{foo}")
operation Operation3(Operation3Input)

structure Operation3Input {
  @required
  @mqttTopicLabel
  foo: smithy.api#String,

  @required
  @mqttTopicLabel
  baz: smithy.api#String,
}


// Missing topicLabel trait for {foo}
@mqttPublish("events4/{foo}")
operation Operation4(Operation4Input)

structure Operation4Input {
  @required
  foo: smithy.api#String,
}


// No errors.
@mqttPublish("events5/{foo}")
operation Operation5(Operation5Input)

structure Operation5Input {
  @required
  @mqttTopicLabel
  foo: smithy.api#String,
}
