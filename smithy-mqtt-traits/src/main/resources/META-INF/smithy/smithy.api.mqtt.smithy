$version: "2.0"

namespace smithy.mqtt

@trait(selector: "service")
@protocolDefinition
structure mqttJson {}

@trait(
    selector: "operation :not(-[output]-> * > member)",
    conflicts: ["smithy.mqtt#subscribe"],
    breakingChanges: [{change: "any"}]
)
@traitValidators(
    "MqttPublishInput.NoEventStreams": {
        selector: "-[input]-> structure > member :test(> union [trait|streaming])"
        message: "The input of `smithy.mqtt#publish` operations cannot contain event streams"
    }
    "MqttUnsupportedErrors": {
        selector: ":test(-[error]->)"
        message: "Operations marked with the `smithy.mqtt#publish` trait do not support errors"
        severity: "DANGER"
    }
)
// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
string publish

@trait(
    selector: "operation :test(-[output]->)"
    conflicts: ["smithy.mqtt#publish"]
    breakingChanges: [{change: "any"}]
)
@traitValidators(
    "MqttSubscribeInput.MissingTopicLabel": {
        selector: "-[input]-> structure > member :not([trait|smithy.mqtt#topicLabel])"
        message: """
            All input members of an operation marked with the `smithy.mqtt#subscribe` trait must be \
            marked with the `smithy.mqtt#topicLabel` trait."""
    }
    "MqttUnsupportedErrors": {
        selector: ":test(-[error]->)"
        message: "Operations marked with the `smithy.mqtt#subscribe` trait do not support errors"
        severity: "DANGER"
    }
    "MqttSubscribeOutput.MissingEventStream": {
        selector: ":test(-[output]-> :not(> member > union [trait|streaming]))"
        message: "Subscribe operations must define an output event stream"
    }
    "MqttSubscribeOutput.EventHeaderNotSupported": {
        selector: "-[output]-> structure > member > union [trait|streaming] > member > structure > member [trait|eventHeader]"
        message: "The `@eventHeader` trait is not supported with subscribe operations."
        severity: "DANGER"
    }
    "MqttSubscribeOutput.NoInitialEvents": {
        selector: ":test(-[output]-> structure > member :not(> union [trait|streaming]))"
        message: "Operations marked with `smithy.mqtt#subscribe` do not support initial responses in their output streams."
    }
)
// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
string subscribe

@trait(selector: "member[trait|required] :test(> :test(string, byte, short, integer, long, boolean, timestamp))")
structure topicLabel {}
