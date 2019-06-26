$version: "0.1.0"

namespace smithy.mqtt

trait publish {
  shape: TopicString,
  selector: "operation:not(-[output]->)",
  conflicts: ["smithy.mqtt#subscribe", "inputEventStream"],
  tags: ["diff.error.const"]
}

trait subscribe {
  shape: TopicString,
  selector: "operation[trait|outputEventStream]",
  conflicts: ["smithy.mqtt#publish"],
  tags: ["diff.error.const"]
}

// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
@private
string TopicString

trait topicLabel {
  selector: "member[trait|required]:test(> :test(string, byte, short, integer, long, boolean, timestamp))",
}
