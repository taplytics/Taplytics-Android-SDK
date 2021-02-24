#!/bin/sh

# Make sure that the script works no matter where its called from
parent_path=$(
  cd "$(dirname "${BASH_SOURCE[0]}")"
  pwd -P
)
cd "$parent_path"

# Constants
RELEASE_BRANCH="test/automate"
MAPPING_FILE="Taplytics/taplytics/build/outputs/mapping/release/mapping.txt"
SDK_VERSION=$(cat ./Taplytics/taplytics/build.gradle | grep ^version | awk '{gsub(/"/, "", $3); print $3}')
NEW_RELEASE_TITLE="Taplytics ${SDK_VERSION} Release"
GIT_TOKEN_FILE="$HOME/.git_token"
SDK_REPO_LOCATION="../../Taplytics-Android-SDK"

read -p "Would you like to make a release for version ${SDK_VERSION}? Enter [Y/n]: " RELEASE
if [ "$RELEASE" != "Y" ]; then
  exit 1
fi

# Check for homebrew and install if not found
printf "Looking for homebrew \n"
if [[ $(command -v brew) == "" ]]; then
  printf "Installing Hombrew \n"
  /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
fi

printf "Looking for Github package \n"
brew list gh || (printf "Installing Github Package \n"; brew install gh)

if [ ! -f $GIT_TOKEN_FILE ]; then
  printf "Github Auth Token not set \n"
  printf "Go to Github > Settings > Developer Settings and generate a new Personal Token with repo and read:org permission \n"
  read -p "Token: " GIT_TOKEN
  touch $GIT_TOKEN_FILE
  printf "$GIT_TOKEN" >$GIT_TOKEN_FILE
fi

gh auth login --with-token <$GIT_TOKEN_FILE || (
  printf "Failed to authenticate with Github \n"
  exit 1
)

# Check for local changes
printf "Checking for any local changes \n"
#if ! git diff-index --quiet HEAD --; then
#    printf "Can't continue until the following changes are committed"
#    git diff-index HEAD
#    exit 1
#fi

# Pull latest from remote if necessary
printf "Updating branch ${RELEASE_BRANCH} to the latest \n"
#git fetch origin
#RESLOG=$(git log HEAD..origin/"${RELEASE_BRANCH}" --oneline)
#if [[ "${RESLOG}" != "" ]]; then
#  git merge origin/"${RELEASE_BRANCH}"
#fi

printf "Generating AAR and mapping \n"
#./../gradlew -p .. uploadArchives

printf "Copying mapping to Taplytics/Taplytics \n"
#cp -fr $MAPPING_FILE "$parent_path/Taplytics/Taplytics"

printf "Committing mapping to ${RELEASE_BRANCH} \n"
#git add Taplytics/taplytics/mapping.txt
#git commit -m "Update mapping"
#git push origin ${RELEASE_BRANCH}

# Create the tag and release in github
printf "Creating release on Github \n"
#gh release create "$SDK_VERSION" --target "$RELEASE_BRANCH" -t "$NEW_RELEASE_TITLE" -F CHANGELOG.md

printf "Creating a PR in the public SDK repo \n"
git --git-dir=$SDK_REPO_LOCATION/.git checkout -b release/"$SDK_VERSION"
cp -fr "$parent_path/SDK/AndroidStudio/com/taplytics/sdk/taplytics" "$SDK_REPO_LOCATION/AndroidStudio/com/taplytics/sdk/taplytics"
git --git-dir=$SDK_REPO_LOCATION/.git add .
git --git-dir=$SDK_REPO_LOCATION/.git commit -m "Release $SDK_VERSION"
git --git-dir=$SDK_REPO_LOCATION/.git push origin release/"$SDK_VERSION"
cd $parent_path/$SDK_REPO_LOCATION
gh pr create --title "Release $SDK_SDK_VERSION" --reviewer gourave