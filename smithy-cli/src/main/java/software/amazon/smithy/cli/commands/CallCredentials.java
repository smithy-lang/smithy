/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import software.amazon.smithy.java.auth.api.identity.IdentityResolver;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.credentials.chain.CredentialChain;
import software.amazon.smithy.java.context.Context;

/** Selects real or dry-run AWS credentials without coupling auth policy to the call orchestrator. */
final class CallCredentials {
    private CallCredentials() {}

    static IdentityResolver<AwsCredentialsIdentity> resolve(boolean dryRun) {
        if (dryRun) {
            return new StaticResolver(
                    AwsCredentialsIdentity.create("AKIDRYRUNEXAMPLE", "dryrun-placeholder-secret-key"));
        }
        return CredentialChain.create(AwsCredentialsIdentity.class);
    }

    private static final class StaticResolver implements IdentityResolver<AwsCredentialsIdentity> {
        private final AwsCredentialsIdentity identity;

        StaticResolver(AwsCredentialsIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public IdentityResult<AwsCredentialsIdentity> resolveIdentity(Context requestProperties) {
            return IdentityResult.of(identity);
        }
    }
}
