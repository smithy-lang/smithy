// This file defines standard AWS configuration options that may be needed for
// testing certain behaviors. This isn't an exhaustive listing of all possible
// configuration settings. See the following for a more comprehensive overview:
// https://docs.aws.amazon.com/credref/latest/refdocs/overview.html

$version: "2.0"

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
    configFile: ConfigFile,
    credentialsFile: ConfigFile,
    client: ClientConfig,
    operation: OperationConfig,
}

/// Config settings that can be set as environment variables.
structure EnvironmentConfig {
    AWS_ACCESS_KEY_ID: String,
    AWS_SECRET_ACCESS_KEY: String,
    AWS_DEFAULT_REGION: String,
    AWS_RETRY_MODE: RetryMode,
    AWS_SESSION_TOKEN: String,
    AWS_PROFILE: String,
}

/// A shape representing a parsed config file, which is a map of profile names
/// to configuration sets.
map ConfigFile {
    /// The top level key in a config file is the "profile", which is a string.
    /// If a profile is not explicitly set, then implementations should check the
    /// profile named "default".
    key: String,
    /// The value is a collection of settings.
    value: FileConfigSettings,
}

/// Config settings that can be set in the AWS config / credentials file as
/// part of a profile.
structure FileConfigSettings {
    aws_access_key_id: String,
    aws_secret_access_key: String,
    aws_session_token: String,
    region: String,
    s3: S3Config,
    retry_mode: RetryMode,
    max_attempts: Short,
}

/// Configuration specific to S3.
structure S3Config {
    addressing_style: S3AddressingStyle,
    use_accelerate_endpoint: Boolean,
    use_dualstack_endpoint: Boolean,
}

/// Configuration that is set on the constructed client.
structure ClientConfig {
    aws_access_key_id: String,
    aws_secret_access_key: String,
    aws_session_token: String,
    region: String,
    s3: S3Config,
    retry_config: RetryConfig,
    aws_profile: String,
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

/// Controls the S3 addressing bucket style.
enum S3AddressingStyle {
    AUTO = "auto"
    PATH = "path"
    VIRTUAL = "virtual"
}

/// Controls the strategy used for retries.
enum RetryMode {
    LEGACY = "legacy"
    STANDARD = "standard"
    ADAPTIVE = "adaptive"
}
