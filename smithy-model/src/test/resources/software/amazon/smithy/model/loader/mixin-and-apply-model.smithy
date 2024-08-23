$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace smithy.example

@http(uri: "/machines", method: "POST")
operation CreateMachine {
    output := {
        Machine: MachineData
    }
}

@mixin
structure MachineDataMixin {
    machineId: String
}

structure MachineData with [MachineDataMixin] {
    manufacturer: String
}

apply MachineData$machineId @required
