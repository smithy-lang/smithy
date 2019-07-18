metadata suppressions = [{"ids": ["UnstableFeature"], "shapes": ["aws.iotjobs#JobDocument"]}]

namespace aws.iotjobs

@protocols([{name: "aws.mqtt-json"}])
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
  messages: RejectedError
}

structure RejectedError {
  clientToken: smithy.api#String,

  @required
  code: RejectedErrorCode,

  message: smithy.api#String,
  timestamp: smithy.api#Timestamp,
  executionState: JobExecutionState,
}

@enum(
  InvalidTopic: {},
  InvalidJson: {},
  InvalidRequest: {},
  InvalidStateTransition: {},
  ResourceNotFound: {},
  VersionMismatch: {},
  InternalError: {},
  RequestThrottled: {},
  TerminalStateReached: {},
)
string RejectedErrorCode

// ------ GetPendingJobExecutions -------

@smithy.mqtt#publish("$aws/things/{thingName}/jobs/get")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-getpendingjobexecutions")
operation PublishGetPendingJobExecutions(GetPendingJobExecutionsRequest)

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/get/accepted")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-getpendingjobexecutions")
operation SubscribeToGetPendingJobExecutionsAccepted(GetPendingJobExecutionsSubscriptionRequest)
  -> GetPendingJobExecutionsSubscriptionResponse

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/get/rejected")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-getpendingjobexecutions")
operation SubscribeToGetPendingJobExecutionsRejected(GetPendingJobExecutionsSubscriptionRequest)
  -> RejectedResponse

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
  messages: GetPendingJobExecutionsResponse,
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
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-startnextpendingjobexecution")
operation PublishStartNextPendingJobExecution(StartNextPendingJobExecutionRequest)

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/start-next/accepted")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-startnextpendingjobexecution")
operation SubscribeToStartNextPendingJobExecutionAccepted(StartNextPendingJobExecutionSubscriptionRequest)
  -> StartNextPendingJobExecutionSubscriptionResponse

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/start-next/rejected")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-startnextpendingjobexecution")
operation SubscribeToStartNextPendingJobExecutionRejected(StartNextPendingJobExecutionSubscriptionRequest)
  -> RejectedResponse

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
  messages: StartNextJobExecutionResponse
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

@enum(
  QUEUED: {},
  IN_PROGRESS: {},
  TIMED_OUT: {},
  FAILED: {},
  SUCCEEDED: {},
  CANCELED: {},
  REJECTED: {},
  REMOVED: {},
)
string JobStatus


// ------- DescribeJobExecution ----------

@smithy.mqtt#publish("$aws/things/{thingName}/jobs/{jobId}/get")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-describejobexecution")
operation PublishDescribeJobExecution(DescribeJobExecutionRequest)

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/get/accepted")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-describejobexecution")
operation SubscribeToDescribeJobExecutionAccepted(DescribeJobExecutionSubscriptionRequest)
  -> DescribeJobExecutionSubscriptionResponse

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/get/rejected")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-describejobexecution")
operation SubscribeToDescribeJobExecutionRejected(DescribeJobExecutionSubscriptionRequest)
  -> RejectedResponse

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
  messages: DescribeJobExecutionResponse
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
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-updatejobexecution")
operation PublishUpdateJobExecution(UpdateJobExecutionRequest)

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/update/accepted")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-updatejobexecution")
operation SubscribeToUpdateJobExecutionAccepted(UpdateJobExecutionSubscriptionRequest)
  -> UpdateJobExecutionSubscriptionResponse

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/{jobId}/update/rejected")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-updatejobexecution")
operation SubscribeToUpdateJobExecutionRejected(UpdateJobExecutionSubscriptionRequest)
  -> RejectedResponse

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
  messages: UpdateJobExecutionResponse
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

document JobDocument


// ------- JobExecutionsChanged ----------

@smithy.mqtt#subscribe("$aws/things/{thingName}/jobs/notify")
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-jobexecutionschanged")
operation SubscribeToJobExecutionsChangedEvents(JobExecutionsChangedSubscriptionRequest)
  -> JobExecutionsChangedSubscriptionResponse

structure JobExecutionsChangedSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String,
}

structure JobExecutionsChangedSubscriptionResponse {
  messages: JobExecutionsChangedEvent,
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
@outputEventStream("messages")
@externalDocumentation("https://docs.aws.amazon.com/iot/latest/developerguide/jobs-api.html#mqtt-nextjobexecutionchanged")
operation SubscribeToNextJobExecutionChangedEvents(NextJobExecutionChangedSubscriptionRequest)
  -> NextJobExecutionChangedSubscriptionResponse

structure NextJobExecutionChangedSubscriptionRequest {
  @required
  @smithy.mqtt#topicLabel
  thingName: smithy.api#String
}

structure NextJobExecutionChangedSubscriptionResponse {
  messages: NextJobExecutionChangedEvent,
}

structure NextJobExecutionChangedEvent {
  @required
  execution: JobExecutionData,

  @required
  timestamp: smithy.api#Timestamp,
}
