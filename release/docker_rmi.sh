#!/bin/sh
# Remove docker images created during a run of docker_build.sh
# This job run in Jenkins as part of job TapisJava->3_ManualBuildDeploy->notifications
# Environment name must be passed in as first argument
# Existing docker login is used for push

PrgName=$(basename "$0")

USAGE="Usage: $PrgName { dev staging prod }"

SVC_NAME="notifications"
SVC_NAME2="tapis-${SVC_NAME}"
SVC_NAME2_W="${SVC_NAME2}-workers"
REPO="tapis"

BUILD_DIR=../api/target
ENV=$1

# Check number of arguments
if [ $# -ne 1 ]; then
  echo $USAGE
  exit 1
fi

# Check that env name is valid
if [ "$ENV" != "dev" -a "$ENV" != "staging" -a "$ENV" != "prod" ]; then
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

# Remove docker images
echo "Removing docker images based on primary tags: $TAG_UNIQ1" and "$TAG_UNIQ2"
echo "  ENV=        ${ENV}"
echo "  VER=        ${VER}"
echo "  GIT_BRANCH_LBL= ${GIT_BRANCH_LBL}"
echo "  GIT_COMMIT_LBL= ${GIT_COMMIT_LBL}"
docker rmi "${TAG_UNIQ1}"
docker rmi "${TAG_LOCAL1}"
docker rmi "${TAG_LATEST1}"
docker rmi "${TAG_ENV1}"
docker rmi "${TAG_UNIQ2}"
docker rmi "${TAG_LOCAL2}"
docker rmi "${TAG_LATEST2}"
docker rmi "${TAG_ENV2}"
cd "$RUN_DIR"
