#!/bin/sh

{
  echo "argv1: $1"
  echo "SMITHY_ROOT_DIR: ${SMITHY_ROOT_DIR}"
  echo "SMITHY_PLUGIN_DIR: ${SMITHY_PLUGIN_DIR}"
  echo "SMITHY_PROJECTION_NAME: ${SMITHY_PROJECTION_NAME}"
  echo "SMITHY_ARTIFACT_NAME: ${SMITHY_ARTIFACT_NAME}"
  echo "SMITHY_INCLUDES_PRELUDE: ${SMITHY_INCLUDES_PRELUDE}"
  echo "FOO_BAR: ${FOO_BAR}"
  echo "FOO_PATH: ${FOO_PATH}"
} >> output.txt

# In case of a bug, don't wait forever on stdin.
cat >> output.txt
