$version: "1.0"

metadata suppressions = [
    {
        id: "UnreferencedShape",
        namespace: "aws.protocoltests.json",
    },
    {
        id: "DeprecatedTrait",
        namespace: "*",
        reason: """
                Some of the AWS protocols make use of deprecated traits, and some are
                themselves deprecated traits. As this package is intended to test those
                protocols, the warnings should be suppressed.""",
    },
]
