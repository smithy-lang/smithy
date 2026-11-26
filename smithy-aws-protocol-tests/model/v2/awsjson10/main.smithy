$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use smithy.protocoltests.corpus#CoreProtocolTestService
use smithy.protocoltests.corpus#DocumentProtocolTestService
use smithy.protocoltests.corpus#EventStreamProtocolTestService
use smithy.protocoltests.corpus#HttpErrorProtocolTestService
use smithy.protocoltests.corpus#MiscSerdeTraitProtocolTestService
use smithy.protocoltests.corpus#NamingProtocolTestService
use smithy.protocoltests.corpus#OptionalityProtocolTestService

@awsJson1_0
@aws.api#service(sdkId: "Json10Corpus", arnNamespace: "json10corpus")
@aws.auth#sigv4(name: "json10corpus")
@title("AWS JSON 1.0 Corpus Protocol Tests")
service AwsJson10CorpusTests with [
    CoreProtocolTestService
    OptionalityProtocolTestService
    DocumentProtocolTestService
    EventStreamProtocolTestService
    HttpErrorProtocolTestService
    NamingProtocolTestService
    MiscSerdeTraitProtocolTestService
] {}
