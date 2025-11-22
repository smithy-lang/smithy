

namespace test

@constraints({
  test: """
    resources.MultiPartUpload.UploadId['ABC']
  """
})
service S3 {

}