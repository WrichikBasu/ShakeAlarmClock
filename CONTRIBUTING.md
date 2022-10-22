# Guidelines for contributing to this repo

Thank you for your interest in develping this app. Issues and suggestions for improvements are always welcome. Anyone is allowed to fork this repository, make changes and submit a pull request (PR). We will be happy to make changes to the app for making it better.

However, before contributing, please read the following guidelines:

## Guidelines for creating an issue:
Issues should only be created for bug reports. Feature requests and questions should be posted under Discussions.

A template has been set up for bug reports. Please try to follow it. It will be extremely helpful if you can also provide a stack trace of the crash (if available), or screenshots.

If you find that what you have in mind cannot be written properly in the provided template, you may create an issue from a blank template.

## Guidelines for forking and creating pull requests:

1. **All commits must be signed.** Unsigned commits will be strictly rejected.

1. Do **not** change the following:
   - Gradle version;
   - Android SDK version;
   - Build tools version;
   - Version of any library, even if it is outdated;
   - App version and/or version codes.

1. Please try to make a logical series of commits in your PR such that they have meaning when someone looks at the history of the project. If this is not followed, I will do a squash merge rather than normal merge (doing so will put all your commits into one single commit in the history of your project).

1. It's upto the owner to decide whether the PR will be simply merged, or squashed.

1. Follow the "one feature one PR" rule: each PR should add only one feature to the app. You can add as many commits as you want for introducing the feature, but these changes should cater to only one feature.

1. Document your code where necessary. Changes without proper documentation may not be accepted.

## The git format followed by this repository

 - Releases are created as tags from the `master` branch.

 - Any development work (feature development or bugfix/hotfix) must be done in a separate branch, which will be later merged with `master` through a PR.

 - The `master` branch follows a quasi-linear history style. This means that all branches/forks must be rebased with `master` before the PR is merged. You have to rebase, clear conflicts and force push to your branch/fork.

