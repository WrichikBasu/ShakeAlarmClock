# Welcome to Shake Alarm Clock!

Issues and suggestions for improvements are always welcome. Anyone is allowed to fork this repository, make changes and submit a pull request (PR). I will be happy to make changes to my app for making it better.

However, before contributing, please read the following guidelines:

## Guidelines for contributing:
1. Please fork from the `dev` branch and submit PRs to the `dev` branch. PRs directly to the `master` branch will not be entertained.
1. Exception to the above rule: only `hotfix` branches are allowed from the `master`. These branches aim at squashing bugs that severely impact the basic functionality of the app. Once approved, `hotfix` branches will be merged with master as well as `dev`.
1. Do **not** change the following:
   - Gradle version
   - Android SDK version
   - Build tools version
   - Version of any other dependency, even if it is outdated
   - App version and/or version codes
1. Please try to make a logical series of commits in your PR such that they have meaning when someone looks at the history of the project. If this is not followed, I will do a squash merge rather than normal merge (doing so will put all your commits into one single commit in the history of your project).
1. Clear merge conflicts, if any, that Github shows when you submit the PR. I will help if you need.
1. Follow the "one feature one PR" rule: each PR should add only one feature to the app. You can change as many files as you need for introducing the feature, but these changes should cater to only one feature.
1. Document your code where necessary. Changes without proper documentation may not be accepted.
