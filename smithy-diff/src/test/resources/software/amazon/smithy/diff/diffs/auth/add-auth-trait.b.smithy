$version: "2.0"

namespace ns.foo

@httpBasicAuth
@httpBearerAuth
@auth([httpBearerAuth, httpBasicAuth])
service Service1 {}
