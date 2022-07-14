$version: "2.0"

namespace aws.iotjobs

use smithy.mqtt#mqttJson

@mqttJson
service IotJobs {
  version: "2018-08-14",
  operations: [
    PublishGetPendingJobExecutions,
    SubscribeToGetPendingJobExecutionsAccepted,
    SubscribeToGetPendingJobExecutionsRejected,

    PublishStartNextPendingJobExecution,
    SubscribeToStartNextPendingJobExecutionAccepted,
    SubscribeToStartNextPendingJobExecutionRejected,

    PublishDescribeJobExecution,
    SubscribeToDescribeJobExecutionAccepted,
    SubscribeToDescribeJobExecutionRejected,

    PublishUpdateJobExecution,
    SubscribeToUpdateJobExecutionAccepted,
    SubscribeToUpdateJobExecutionRejected,

    SubscribeToJobExecutionsChangedEvents,
    SubscribeToNextJobExecutionChangedEvents,
  ],
}

// ------ Service-wide error -------

structure RejectedResponse {
  messages: RejectedErrorStream
}

@streaming
union RejectedErrorStream {
    singleton: RejectedError
}

structure RejectedError {
  clientToken: smithy.api#String,

  @required
  code: RejectedErrorCode,

  message: smithy.api#String,
  timestamp: smithy.api#Timestamp,
  executionState: JobExecutionState,
}

enum RejectedErrorCode {
    @enumValue("InvalidTopic")
    INVALID_TOPIC

    @enumValue("InvalidJson")
    INVALID_JSON

    @enumValue("InvalidRequest")
    INVALID_REQUEST

    @enumValue("InvalidStateTransition")
    INVALID_STATE_TRANSITION

    @enumValue("ResourceNotFound")
    RESOURCE_NOT_FOUND

    @enumValue("VersionMismatch")
    VERSION_MISMATCH

    @enumValue("InternalError")
    INTERNAL_ERROR

    @enumValue("RequestThrottled")
    REQUEST_THROTTLED

    @enumValue("TerminalStateReached")
    TERMINAL_STATE_REACHED
}

// ------ GetPendingJobExecutions -------

@smithy.mqtt#publish("$aws/things/{thingName}/jobs/get")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-getpendingjobexecutions")
operation PublishGetPendingJobExecutions {
    input: GetPendingJobExecutionsRequest
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/get/accepted")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-getpendingjobexecutions")
operation SubscribeToGetPendingJobExecutionsAccepted {
    input: GetPendingJobExecutionsSubscriptionRequest,
    output: GetPendingJobExecutionsSubscriptionResponse
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/get/rejected")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-getpendingjobexecutions")
operation SubscribeToGetPendingJobExecutionsRejected {
    input: GetPendingJobExecutionsSubscriptionRequest,
    output: RejectedResponse,
}

structure GetPendingJobExecutionsRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,

  clientToken: smithy.api#String,
}

structure GetPendingJobExecutionsSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,
}

structure GetPendingJobExecutionsSubscriptionResponse {
  messages: GetPendingJobExecutionsResponseStream,
}

@streaming
union GetPendingJobExecutionsResponseStream {
    singleton: GetPendingJobExecutionsResponse
}

structure GetPendingJobExecutionsResponse {
  inProgressJobs: JobExecutionSummaryList,
  queuedJobs: JobExecutionSummaryList,
  timestamp: smithy.api#Timestamp,

  clientToken: smithy.api#String,
}

list JobExecutionSummaryList {
  member: JobExecutionSummary
}

structure JobExecutionSummary {
  jobId: smithy.api#String,
  executionNumber: smithy.api#Long,
  versionNumber: smithy.api#Integer,
  lastUpdatedAt: smithy.api#Timestamp,
  queuedAt: smithy.api#Timestamp,
  startedAt: smithy.api#Timestamp,
}


// ------- StartNextPendingJobExecution ----------

@smithy.mqtt#publish("$aws/things/{thingName}/jobs/start-next")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-startnextpendingjobexecution")
operation PublishStartNextPendingJobExecution {
    input: StartNextPendingJobExecutionRequest
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/start-next/accepted")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-startnextpendingjobexecution")
operation SubscribeToStartNextPendingJobExecutionAccepted {
    input: StartNextPendingJobExecutionSubscriptionRequest,
    output: StartNextPendingJobExecutionSubscriptionResponse
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/start-next/rejected")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-startnextpendingjobexecution")
operation SubscribeToStartNextPendingJobExecutionRejected {
    input: StartNextPendingJobExecutionSubscriptionRequest,
    output: RejectedResponse
}

structure StartNextPendingJobExecutionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,

  clientToken: smithy.api#String,

  stepTimeoutInMinutes: smithy.api#Long,
  statusDetails: StatusDetails,
}

map StatusDetails {
  key: smithy.api#String,
  value: smithy.api#String
}

structure StartNextPendingJobExecutionSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,
}

structure StartNextPendingJobExecutionSubscriptionResponse {
  messages: StartNextJobExecutionResponseStream
}

@streaming
union StartNextJobExecutionResponseStream {
    singleton: StartNextJobExecutionResponse
}

structure StartNextJobExecutionResponse {
  clientToken: smithy.api#String,

  execution: JobExecutionData,
  timestamp: smithy.api#Timestamp,
}

structure JobExecutionData {
  jobId: smithy.api#String,
  thingName: smithy.api#String,
  jobDocument: JobDocument,
  status: JobStatus,
  statusDetails: StatusDetails,
  queuedAt: smithy.api#Timestamp,
  startedAt: smithy.api#Timestamp,
  lastUpdatedAt: smithy.api#Timestamp,
  versionNumber: smithy.api#Integer,
  executionNumber: smithy.api#Long,
}

enum JobStatus {
    QUEUED
    IN_PROGRESS
    TIMED_OUT
    FAILED
    SUCCEEDED
    CANCELED
    REJECTED
    REMOVED
}


// ------- DescribeJobExecution ----------

@smithy.mqtt#publish("$aws/things/{thingName}/jobs/{jobId}/get")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-describejobexecution")
operation PublishDescribeJobExecution {
    input: DescribeJobExecutionRequest
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/get/accepted")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-describejobexecution")
operation SubscribeToDescribeJobExecutionAccepted {
    input: DescribeJobExecutionSubscriptionRequest,
    output: DescribeJobExecutionSubscriptionResponse
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/get/rejected")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-describejobexecution")
operation SubscribeToDescribeJobExecutionRejected {
    input: DescribeJobExecutionSubscriptionRequest,
    output: RejectedResponse
}

structure DescribeJobExecutionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,

  @required
  @smithy.mqtt#topicLabel
  jobId: smithy.api#String,

  clientToken: smithy.api#String,

  executionNumber: smithy.api#Long,
  includeJobDocument: smithy.api#Boolean,
}

structure DescribeJobExecutionSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,

  @required
  @smithy.mqtt#topicLabel
  jobId: smithy.api#String,
}

structure DescribeJobExecutionSubscriptionResponse {
  messages: DescribeJobExecutionResponseStream
}

@streaming
union DescribeJobExecutionResponseStream {
    singleton: DescribeJobExecutionResponse
}

structure DescribeJobExecutionResponse {
  clientToken: smithy.api#String,

  @required
  execution: JobExecutionData,

  @required
  timestamp: smithy.api#Timestamp,
}


// ------- UpdateJobExecution ----------

@smithy.mqtt#publish("$aws/things/{thingName}/jobs/{jobId}/update")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-updatejobexecution")
operation PublishUpdateJobExecution {
    input: UpdateJobExecutionRequest
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/update/accepted")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-updatejobexecution")
operation SubscribeToUpdateJobExecutionAccepted {
    input: UpdateJobExecutionSubscriptionRequest,
    output: UpdateJobExecutionSubscriptionResponse
}

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/update/rejected")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-updatejobexecution")
operation SubscribeToUpdateJobExecutionRejected {
    input: UpdateJobExecutionSubscriptionRequest,
    output: RejectedResponse
}

structure UpdateJobExecutionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,

  @required
  @smithy.mqtt#topicLabel
  jobId: smithy.api#String,

  @required
  status: JobStatus,

  clientToken: smithy.api#String,

  statusDetails: StatusDetails,
  expectedVersion: smithy.api#Integer,
  executionNumber: smithy.api#Long,
  includeJobExecutionState: smithy.api#Boolean,
  includeJobDocument: smithy.api#Boolean,
}

structure UpdateJobExecutionSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,

  @required
  @smithy.mqtt#topicLabel
  jobId: smithy.api#String,
}

structure UpdateJobExecutionSubscriptionResponse {
  messages: UpdateJobExecutionResponseStream
}

@streaming
union UpdateJobExecutionResponseStream {
    singleton: UpdateJobExecutionResponse
}

structure UpdateJobExecutionResponse {
  clientToken: smithy.api#String,

  @required
  executionState: JobExecutionState,

  @required
  jobDocument: JobDocument,

  @required
  timestamp: smithy.api#Timestamp,
}

structure JobExecutionState {
  status: JobStatus,
  statusDetails: StatusDetails,
  versionNumber: smithy.api#Integer,
}

@suppress(["UnstableFeature"])
document JobDocument


// ------- JobExecutionsChanged ----------

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/notify")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-jobexecutionschanged")
operation SubscribeToJobExecutionsChangedEvents {
    input: JobExecutionsChangedSubscriptionRequest,
    output: JobExecutionsChangedSubscriptionResponse
}

structure JobExecutionsChangedSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,
}

structure JobExecutionsChangedSubscriptionResponse {
  messages: JobExecutionsChangedEventStream,
}

@streaming
union JobExecutionsChangedEventStream {
    singleton: JobExecutionsChangedEvent
}

structure JobExecutionsChangedEvent {
  @required
  jobs: JobExecutionsChangedJobs,

  @required
  timestamp: smithy.api#Timestamp,
}

map JobExecutionsChangedJobs {
  key: JobStatus,
  value: JobExecutionSummaryList
}


// ------- NextJobExecutionChanged ----------

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/notify-next")
@externalDocumentation("API Reference": "https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-nextjobexecutionchanged")
operation SubscribeToNextJobExecutionChangedEvents {
    input: NextJobExecutionChangedSubscriptionRequest,
    output: NextJobExecutionChangedSubscriptionResponse
}

structure NextJobExecutionChangedSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String
}

structure NextJobExecutionChangedSubscriptionResponse {
  messages: NextJobExecutionChangedEventStream,
}

@streaming
union NextJobExecutionChangedEventStream {
    singleton: NextJobExecutionChangedEvent
}

structure NextJobExecutionChangedEvent {
  @required
  execution: JobExecutionData,

  @required
  timestamp: smithy.api#Timestamp,
}
