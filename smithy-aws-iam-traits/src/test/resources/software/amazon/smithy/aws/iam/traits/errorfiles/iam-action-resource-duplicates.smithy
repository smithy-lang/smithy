$version: "2.0"

namespace smithy.example

use aws.iam#iamAction

@iamAction(
    resources: {
        required: {
            "bar": {
                conditionKeys: ["foo:asdf"]
            }
            "bap": {
                conditionKeys: ["foo:zxcv"]
            }
        }
        optional: {
            "baz": {
                conditionKeys: ["foo:qwer"]
            }
            "bap": {
                conditionKeys: ["foo:zxcv"]
            }
        }
    }
)
operation Foo {}
