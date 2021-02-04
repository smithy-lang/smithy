// This file defines standard AWS configuration options that may be needed for
// testing certain behaviors. This isn't an exhaustive listing of all possible
// configuration settings. See the following for a more comprehensive overview:
// https://docs.aws.amazon.com/credref/latest/refdocs/overview.html

$version: "1.0"

metadata suppressions = [{
    id: "UnreferencedShape",
    namespace: "aws.protocoltests.config",
    reason: """
        These shapes are intended to be used to validate vendorParams in
        protocol tests, so they naturally will not be connected to a service.
        """
}]

namespace aws.protocoltests.config

structure AwsConfig {
    /// This is the time that should be set during the course of the test.
    /// This is important for things like signing where the clock time impacts
    /// the result.
    clockTime: Timestamp,
    scopedConfig: ScopedConfig,
}

/// Config settings that are scoped to different sources, such as environment
/// variables or the AWS config file.
structure ScopedConfig {
    environment: EnvironmentConfig,
    file: FileConfig,
    client: ClientConfig,
    operation: OperationConfig,
}

/// Config settings that can be set as environment variables.
structure EnvironmentConfig {
    AWS_ACCESS_KEY_ID: String,
    AWS_SECRET_ACCESS_KEY: String,
    AWS_DEFAULT_REGION: String,
    AWS_RETRY_MODE: RetryMode,
}

/// Config settings that can be set in the AWS config file.
structure FileConfig {
    access_key_id: String,
    secret_access_key: String,
    region: String,
    s3: S3Config,
    retry_mode: RetryMode,
    max_attempts: Short,
}

/// Configuration specific to S3.
structure S3Config {
    use_accelerate_endpoint: Boolean,
    use_dualstack_endpoint: Boolean,
}

/// Configuration that is set on the constructed client.
structure ClientConfig {
    access_key_id: String,
    secret_access_key: String,
    region: String,
    s3: S3Config,
    retry_config: RetryConfig,
}

/// Configuration that is set for the scope of a single operation.
structure OperationConfig {
    s3: S3Config,
}

/// Configuration specific to retries.
structure RetryConfig {
    mode: RetryMode,
    max_attempts: Short,
}

/// Controls the strategy used for retries.
@enum([
    {
        value: "legacy",
        name: "LEGACY",
    },
    {
        value: "standard",
        name: "STANDARD",
    },
    {
        value: "adaptive",
        name: "ADAPTIVE",
    }
])
string RetryMode
