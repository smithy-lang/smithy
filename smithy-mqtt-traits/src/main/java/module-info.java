import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.mqtt.traits.MqttModelDiscovery;
import software.amazon.smithy.mqtt.traits.PublishTrait;
import software.amazon.smithy.mqtt.traits.SubscribeTrait;
import software.amazon.smithy.mqtt.traits.TopicLabelTrait;
import software.amazon.smithy.mqtt.traits.validators.MqttSubscribeInputValidator;
import software.amazon.smithy.mqtt.traits.validators.MqttSubscribeOutputValidator;
import software.amazon.smithy.mqtt.traits.validators.MqttTopicConflictValidator;
import software.amazon.smithy.mqtt.traits.validators.MqttTopicLabelValidator;
import software.amazon.smithy.mqtt.traits.validators.MqttUnsupportedErrorsValidator;

module software.amazon.smithy.mqtt.traits {
    requires java.logging;
    requires software.amazon.smithy.model;

    exports software.amazon.smithy.mqtt.traits;

    uses ModelDiscovery;
    uses TraitService;
    uses Validator;

    // This is required in order for the MQTT model to be discovered
    // via ModelAssembler#discoverModels.
    provides ModelDiscovery with MqttModelDiscovery;

    // Register typed values for MQTT traits.
    provides TraitService with
            PublishTrait,
            SubscribeTrait,
            TopicLabelTrait;

    // Register MQTT validators. MQTT requires more custom validation than
    // just simple selectors.
    provides Validator with
            MqttSubscribeInputValidator,
            MqttSubscribeOutputValidator,
            MqttTopicConflictValidator,
            MqttTopicLabelValidator,
            MqttUnsupportedErrorsValidator;
}
