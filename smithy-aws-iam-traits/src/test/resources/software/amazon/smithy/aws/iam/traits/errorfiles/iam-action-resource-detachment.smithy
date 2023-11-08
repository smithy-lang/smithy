$version: "2.0"

namespace smithy.example

use aws.iam#iamAction

resource Monitor {
    resources: [HealthEvent]
}

resource HealthEvent {
    read: GetHealthEvent
}

@iamAction(
    resources: {
        required: {
            "HealthEvent": {}
        }
    }
)
@readonly
operation GetHealthEvent {}
