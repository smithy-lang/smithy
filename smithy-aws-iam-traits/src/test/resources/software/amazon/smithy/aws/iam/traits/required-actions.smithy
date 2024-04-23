$version: "1.0"

namespace smithy.example

@aws.iam#requiredActions(["iam:PassRole", "ec2:RunInstances"])
operation MyOperation {}
