# Development on OpenShift

## Getting Started With Helm

This directory contains a Helm chart which can be used to deploy a development version of this app for rapid testing.

Before you use it, you will need to download & install Helm 3.

If you are not familiar with Helm - how to configure it and run - you can start with this quickstart:

[https://helm.sh/docs/intro/quickstart](https://helm.sh/docs/intro/quickstart)

## Using This Chart

1. Clone the target repo:

```
git clone https://github.com/rht-labs/lodestar-participants
```

2. Change into to the `deployment` directory:

```
cd lodestar-git-api/deployment
```

3. Deploy using the following Helm command:

```shell script
  helm template . \
    --values values-dev.yaml \
    --set db.url=<dbUrl> \
    --set api.gitlab=<gitlabUrl> \
    --set api.engagement=<engagementUrl> \
    --set tokens.gitlab=<gitlabToken> \
    --set git.uri=<your fork> \
    --set git.ref=<your branch> \
  | oc apply -f -
```

It accepts the following variables

| Variable  | Use  |
|---|---|
| `git.uri`  | The HTTPS reference to the repo (your fork!) to build  |
| `git.ref`  | The branch name to build  |
| `db.url`  | The jdbc url to the database  |
| `api.engagement`  | The base URL of the Engagement data  |
| `api.gitlab`  | The base URL of the GitLab instance to use  |
| `tokens.gitlab`  | The access token to use to auth against GitLab  |

This will spin up all of the usual resources that this service needs in production, plus a `BuildConfig` configured to build it from source from the Git repository specified. To trigger this build, use `oc start-build lodestar-participants`. To trigger a build from the source code on your machine, use `oc start-build lodestar-participants --from-dir=. -F` 
