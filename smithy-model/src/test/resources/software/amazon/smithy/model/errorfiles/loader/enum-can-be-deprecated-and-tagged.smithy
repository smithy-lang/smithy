$version: "2.0"

namespace smithy.example

// This example is taken from the specification to ensure that
// enums can be defined as specified.
@enum([
    {
        value: "t2.nano",
        name: "T2_NANO",
        documentation: """
            T2 instances are Burstable Performance
            Instances that provide a baseline level of CPU
            performance with the ability to burst above the
            baseline.""",
        tags: ["ebsOnly"]
    },
    {
        value: "t2.micro",
        name: "T2_MICRO",
        documentation: """
            T2 instances are Burstable Performance
            Instances that provide a baseline level of CPU
            performance with the ability to burst above the
            baseline.""",
        tags: ["ebsOnly"]
    },
    {
        value: "m256.mega",
        name: "M256_MEGA",
        deprecated: true
    }
])
string MyString
