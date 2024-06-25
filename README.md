# gridcapa-core-cc-data-bridge
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

This repository contains the data bridge for GridCapa Core CC process. It is dedicated to retrieve and validate input files and
finally save them to MinIO server.

## Build application

Application is using Maven as base build framework. Application is simply built with following command.

```bash
mvn install
```

## Build docker image

For building Docker image of the application, start by building application.

```bash
mvn install
```

Then build docker image

```bash
docker build -t farao/gridcapa-core-cc-data-bridge .
```

## Synchronize repository with standard data-bridge
### Automatic synchronization
When some changes have been made on standard data-bridge and need to be carried over to core-cc-data-bridge, you can try to click on "Sync fork" button on GitHub interface (see screen capture below).

If there is no conflict between the master branch of both repositories, you may open a Pull Request directly from here.

![Screen capture](sync-repo-data-bridge-1.png)

### Manual synchronization
If there are some conflicts, you will need to use git command line to synchronize core-cc-data-bridge repository with standard data-bridge's one.

#### Configure remote repository
First, if not done before, you will need to configure a new remote repository in your core-cc-data-bridge local repository.

Open a terminal at root of your core-cc-data-bridge repository and execute the following command to check the currently configured remote repositories. You should see only two lines in response referencing `origin` remote repository (see below).
```bash
$ git remote -v
> origin  https://github.com/farao-community/gridcapa-core-cc-data-bridge.git (fetch)
> origin  https://github.com/farao-community/gridcapa-core-cc-data-bridge.git (push)
```

Specify a new `upstream` remote repository, corresponding to standard data-bridge's repository, that will be used to sync the fork.
```bash
$ git remote add upstream https://github.com/farao-community/gridcapa-data-bridge.git
```

Now execute previous command again to check that the new `upstream` repository has been added as remote repository.
```bash
$ git remote -v
> origin  https://github.com/farao-community/gridcapa-core-cc-data-bridge.git (fetch)
> origin  https://github.com/farao-community/gridcapa-core-cc-data-bridge.git (push)
> upstream        https://github.com/farao-community/gridcapa-data-bridge.git (fetch)
> upstream        https://github.com/farao-community/gridcapa-data-bridge.git (push)
```

For detail, see [this link](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/configuring-a-remote-repository-for-a-fork).

#### Synchronize repositories
Open a terminal at root of your core-cc-data-bridge repository and execute the following command to fetch `upstream` repository.
```bash
$ git fetch upstream
```

Checkout on your local `master` branch, pull changes from `origin` to ensure your local version is up-to-date with distant one and then checkout to a new branch (name it the way you want, in this example we will use `sync-with-upstream`).
```bash
$ git checkout master
$ git pull origin master
$ git checkout -b sync-with-upstream
```

Merge the changes from the `upstream` default branch (`upstream/master`) into your local default branch. In case of conflicts, resolve them normally and commit changes.
```bash
$ git merge upstream/master
```

Then push your local branch to `origin` and open a Pull Request from this branch to `master`.
```bash
$ git push origin master
```

For detail, see [this link](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/syncing-a-fork#syncing-a-fork-branch-from-the-command-line)