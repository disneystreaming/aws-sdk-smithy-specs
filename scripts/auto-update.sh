#!/bin/bash 

set -euo pipefail

# reset the branch if it exists
git checkout -B maintenance/auto-update

# update the submodule (added --init so that it works when you checkout)
# initially. it does nothing on CI because `submodules: true` is used on
# the checkout action
git submodule update --init --remote --recursive

if [[ $(git status --porcelain) ]]; then
    echo "Staging and commiting submodule update"
    git add aws-sdk-js-v3
    git commit --message "Update aws-sdk-js-v3 submodule reference"
else
    echo "No changes"
fi