$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_1
use smithy.protocoltests.corpus#CoreProtocolTestService
use smithy.protocoltests.corpus#DocumentProtocolTestService
use smithy.protocoltests.corpus#EventStreamProtocolTestService
use smithy.protocoltests.corpus#HttpErrorProtocolTestService
use smithy.protocoltests.corpus#MiscSerdeTraitProtocolTestService
use smithy.protocoltests.corpus#NamingProtocolTestService
use smithy.protocoltests.corpus#OptionalityProtocolTestService

@awsJson1_1
@aws.api#service(sdkId: "Json11Corpus", arnNamespace: "json11corpus")
@aws.auth#sigv4(name: "json11corpus")
@title("AWS JSON 1.1 Corpus Protocol Tests")
service AwsJson11CorpusTests with [
    CoreProtocolTestService
    OptionalityProtocolTestService
    DocumentProtocolTestService
    EventStreamProtocolTestService
    HttpErrorProtocolTestService
    NamingProtocolTestService
    MiscSerdeTraitProtocolTestService
] {}
