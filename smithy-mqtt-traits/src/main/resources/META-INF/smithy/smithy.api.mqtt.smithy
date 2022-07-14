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
// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
string publish

@trait(
    selector: "operation:test(-[output]-> structure > member > union[trait|streaming])",
    conflicts: ["smithy.mqtt#publish"],
    breakingChanges: [{change: "any"}]
)
// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
string subscribe

@trait(selector: "member[trait|required] :test(> :test(string, byte, short, integer, long, boolean, timestamp))")
structure topicLabel {}
