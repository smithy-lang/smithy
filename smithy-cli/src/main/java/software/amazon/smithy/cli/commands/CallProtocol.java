/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.Locale;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.client.rpcv2json.RpcV2JsonProtocol;
import software.amazon.smithy.model.shapes.ShapeId;

/** Protocol override parsing shared by command execution and help. */
final class CallProtocol {
    static final String SUPPORTED = "rest-json, aws-json, rest-xml, rpc-v2-cbor, or rpc-v2-json";

    private CallProtocol() {}

    static ClientProtocol<?, ?> create(String protocol, ShapeId serviceId) {
        switch (protocol.toLowerCase(Locale.ROOT).replace("_", "-")) {
            case "rest-json":
            case "restjson1":
            case "aws.protocols#restjson1":
                return new RestJsonClientProtocol(serviceId);
            case "aws-json":
            case "awsjson":
            case "awsjson1":
                return new AwsJson1Protocol(serviceId);
            case "rest-xml":
            case "restxml":
            case "aws.protocols#restxml":
                return new RestXmlClientProtocol(serviceId);
            case "rpc-v2-cbor":
            case "rpcv2cbor":
            case "smithy.protocols#rpcv2cbor":
                return new RpcV2CborProtocol(serviceId);
            case "rpc-v2-json":
            case "rpcv2json":
            case "smithy.protocols#rpcv2json":
                return new RpcV2JsonProtocol(serviceId);
            default:
                throw new CliError("Unknown --protocol '" + protocol + "'. Supported: " + SUPPORTED + ".");
        }
    }
}
