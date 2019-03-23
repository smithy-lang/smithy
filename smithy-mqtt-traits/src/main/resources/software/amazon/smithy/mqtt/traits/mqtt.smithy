$version:1.0
namespace smithy.api

trait mqttPublish {
  shape: MqttTopicString,
  selector: "operation:not(-[output]->)",
  conflicts: [mqttSubscribe, inputEventStream],
  tags: [diff.error.const]
}

trait mqttSubscribe {
  shape: MqttTopicString,
  selector: "operation[trait|outputEventStream]",
  conflicts: [mqttPublish],
  tags: [diff.error.const]
}

// Matches one or more characters that are not "#" or "+".
@pattern("^[^#+]+$")
@private
string MqttTopicString

trait mqttTopicLabel {
  selector: "member[trait|required]:test(> :test(string, byte, short, integer, long, boolean, timestamp))",
}
