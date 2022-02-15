#!/bin/sh
# Build and optionally push docker images for Notifications service
# This is the job run in Jenkins as part of job TapisJava->3_ManualBuildDeploy->notifications
# Environment name must be passed in as first argument
# Existing docker login is used for push
# Main service docker image is created with a unique tag: tapis/<SVC_NAME>-<ENV>-<VER>-<COMMIT>-<YYYYmmddHHMM>
# Dispatcher service docker image is created with a unique tag: tapis/<SVC_NAME>-dispatcher-<ENV>-<VER>-<COMMIT>-<YYYYmmddHHMM>
#   - other tags are created and updated as appropriate
#
# Env var TAPIS_DEPLOY_MANUAL may be set to "true" to indicate it is a manual deployment and the
#   images should also be tagged with $ENV

PrgName=$(basename "$0")

USAGE="Usage: $PrgName { dev staging prod } [ -push ]"

SVC_NAME="notifications"
REPO="tapis"

BUILD_DIR=../tapis-notificationsapi/target
ENV=$1

# Check number of arguments
if [ $# -lt 1 -o $# -gt 2 ]; then
  echo $USAGE
  exit 1
fi

# Check that env name is valid
if [ "$ENV" != "dev" -a "$ENV" != "staging" -a "$ENV" != "prod" ]; then
  echo $USAGE
  exit 1
fi

# Check second arg
if [ $# -eq 2 -a "x$2" != "x-push" ]; then
  echo $USAGE
  exit 1
fi

# Determine absolute path to location from which we are running
#  and change to that directory.
export RUN_DIR=$(pwd)
export PRG_RELPATH=$(dirname "$0")
cd "$PRG_RELPATH"/. || exit
export PRG_PATH=$(pwd)

# Make sure service has been built
if [ ! -d "$BUILD_DIR" ]; then
  echo "Build directory missing. Please build. Directory: $BUILD_DIR"
  exit 1
fi

# Copy Dockerfiles to build dir
cp Dockerfile_api Dockerfile_dispatcher $BUILD_DIR

# Move to the build directory
cd $BUILD_DIR || exit

# Set variables used for build
VER=$(cat classes/tapis.version)
GIT_BRANCH_LBL=$(awk '{print $1}' classes/git.info)
GIT_COMMIT_LBL=$(awk '{print $2}' classes/git.info)
TAG_UNIQ1="${REPO}/${SVC_NAME}:${ENV}-${VER}-$(date +%Y%m%d%H%M)-${GIT_COMMIT_LBL}"
TAG_UNIQ2="${REPO}/${SVC_NAME}-dispatcher:${ENV}-${VER}-$(date +%Y%m%d%H%M)-${GIT_COMMIT_LBL}"
TAG_ENV1="${REPO}/${SVC_NAME}:${ENV}"
TAG_ENV2="${REPO}/${SVC_NAME}-dispatcher:${ENV}"
TAG_LATEST1="${REPO}/${SVC_NAME}:latest"
TAG_LATEST2="${REPO}/${SVC_NAME}-dispatcher:latest"
TAG_LOCAL1="${REPO}/${SVC_NAME}:dev_local"
TAG_LOCAL2="${REPO}/${SVC_NAME}-dispatcher:dev_local"

# If branch name is UNKNOWN or empty as might be the case in a jenkins job then
#   set it to GIT_BRANCH. Jenkins jobs should have this set in the env.
if [ -z "$GIT_BRANCH_LBL" -o "x$GIT_BRANCH_LBL" = "xUNKNOWN" ]; then
  GIT_BRANCH_LBL=$(echo "$GIT_BRANCH" | awk -F"/" '{print $2}')
fi

# Build image from Dockerfile_api
echo "Building local images using primary tags: $TAG_UNIQ1 and $TAG_UNIQ2"
echo "  ENV=        ${ENV}"
echo "  VER=        ${VER}"
echo "  GIT_BRANCH_LBL= ${GIT_BRANCH_LBL}"
echo "  GIT_COMMIT_LBL= ${GIT_COMMIT_LBL}"
docker build -f Dockerfile_api \
   --label VER="${VER}" --label GIT_COMMIT="${GIT_COMMIT_LBL}" --label GIT_BRANCH="${GIT_BRANCH_LBL}" \
    -t "${TAG_UNIQ1}" .
# Build image from Dockerfile_dispatcher
docker build -f Dockerfile_dispatcher \
   --label VER="${VER}" --label GIT_COMMIT="${GIT_COMMIT_LBL}" --label GIT_BRANCH="${GIT_BRANCH_LBL}" \
    -t "${TAG_UNIQ2}" .

# Create other tags for remote repo
echo "Creating images for local testing using tags: $TAG_LOCAL1 and $TAG_LOCAL2"
docker tag "$TAG_UNIQ1" "$TAG_LOCAL1"
docker tag "$TAG_UNIQ2" "$TAG_LOCAL2"

# Push to remote repo
if [ "x$2" = "x-push" ]; then
  if [ "$ENV" = "prod" ]; then
    echo "Creating third image tags for prod env: $TAG_LATEST1 and $TAG_LATEST2"
    docker tag "$TAG_UNIQ1" "$TAG_LATEST1"
    docker tag "$TAG_UNIQ2" "$TAG_LATEST2"
  fi
  echo "Pushing images to docker hub."
  # NOTE: Use current login. Jenkins job does login
  docker push "$TAG_UNIQ1"
  docker push "$TAG_UNIQ2"
  if [ "x$TAPIS_DEPLOY_MANUAL" = "xtrue" ]; then
    echo "Creating ENV image tags: $TAG_ENV1 and $TAG_ENV2"
    docker tag "$TAG_UNIQ1" "$TAG_ENV1"
    docker tag "$TAG_UNIQ2" "$TAG_ENV2"
    docker push "$TAG_ENV1"
    docker push "$TAG_ENV2"
  fi
  if [ "$ENV" = "prod" ]; then
    docker push "$TAG_LATEST1"
    docker push "$TAG_LATEST2"
  fi
fi
cd "$RUN_DIR"
