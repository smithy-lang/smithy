$version: "0.4.0"

namespace smithy.mqtt

@trait(selector: "operation:not(-[output]->)",
       conflicts: ["smithy.mqtt#subscribe", "inputEventStream"])
@tags(["diff.error.const"])
// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
string publish

@trait(selector: "operation[trait|outputEventStream]",
       conflicts: ["smithy.mqtt#publish"])
@tags(["diff.error.const"])
// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
string subscribe

@trait(selector: "member[trait|required]:test(> :test(string, byte, short, integer, long, boolean, timestamp))")
structure topicLabel {}
