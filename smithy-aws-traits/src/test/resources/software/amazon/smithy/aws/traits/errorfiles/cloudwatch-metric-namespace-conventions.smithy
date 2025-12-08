$version: "2.0"

namespace ns.foo

use aws.api#service

@service(sdkId: "Valid", cloudWatchNamespace: "AWS/Valid")
service ValidService {
    version: "2020-07-02"
}

@service(sdkId: "Not Pascal", cloudWatchNamespace: "AWS/notPascal")
service NotPascalService {
    version: "2020-07-02"
}

@service(sdkId: "No Prefix", cloudWatchNamespace: "NoPrefix")
service NoPrefixService {
    version: "2020-07-02"
}

@service(sdkId: "Both", cloudWatchNamespace: "both")
service BothService {
    version: "2020-07-02"
}
