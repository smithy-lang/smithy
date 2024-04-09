$version: "2.0"
namespace smithy.example

@aws.iam#actionName("overridingActionName")
operation Echo {}

operation GetResource2 {
    input: GetResource2Input
}

structure GetResource2Input {
    id1: String,

    @required
    id2: String
}
