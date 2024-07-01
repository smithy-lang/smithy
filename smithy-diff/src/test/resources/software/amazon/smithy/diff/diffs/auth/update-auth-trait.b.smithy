$version: "2.0"

namespace ns.foo

@httpBasicAuth
@httpBearerAuth
@auth([httpBasicAuth, httpBearerAuth])
service Service1 {}

@httpBasicAuth
@httpBearerAuth
@auth([httpBearerAuth, httpBasicAuth])
service Service2 {}

@httpBasicAuth
@httpBearerAuth
@auth([httpBearerAuth])
service Service3 {}

@httpBasicAuth
@httpBearerAuth
@auth([httpBasicAuth, httpBearerAuth])
service Service4 {}
