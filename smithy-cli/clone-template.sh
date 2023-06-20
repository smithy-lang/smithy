#!/bin/bash

#REPO=ssh://git.amazon.com/pkg/Smithy-Examples
#TEMPLATE_PATH=templates/common-shapes

REPO=$1
TEMPLATE=$2
TEMPLATE_PATH="templates/$TEMPLATE"
RELATIVE_PREFIX=../

# Check whether current directory is a Git repo
if git rev-parse --is-inside-work-tree >& /dev/null; then
  printf '%s\n' "Cannot clone template into an existing Git repository" >&2
  exit 1
fi

rm -rf Smithy-Examples
git clone --filter=blob:none --no-checkout --depth 1 --sparse $REPO
cd Smithy-Examples
git sparse-checkout set --no-cone $TEMPLATE_PATH
git checkout

SYMLINK_PATH=$(readlink $TEMPLATE_PATH)
if [ -z ${SYMLINK_PATH+x} ]; then
  echo "no symlink found. continuing";
else
  echo "symlink found: '$SYMLINK_PATH'";
  TEMPLATE_PATH=${SYMLINK_PATH#$RELATIVE_PREFIX}
  git sparse-checkout set --no-cone $TEMPLATE_PATH
  git checkout
fi

rm -rf "../$TEMPLATE"
mv $TEMPLATE_PATH "../$TEMPLATE"
rm -rf ../Smithy-Examples
