$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use aws.protocols#awsQueryCompatible

@awsJson1_0
@awsQueryCompatible
@aws.api#service(sdkId: "Json10QueryCompatCorpus", arnNamespace: "awsjson10querycompatcorpus")
@aws.auth#sigv4(name: "awsjson10querycompatcorpus")
service AwsJson10QueryCompatCorpusTests {
    operations: [
        QueryCompatErrorOp
    ]
}
