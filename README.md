# LodeStar Participants

This project manages participant data for LodeStar.

The API is document via swagger and is available at `/q/swagger-ui`

An experimental GraphQL deployment is available at `/q/graphql-ui` and `graphql/schema.graphql`

----

## Configuration

The following environment variables are available:

### Logging
| Name | Default | Description|
|------|---------|------------|
| ENGAGEMENT_API_URL | http://git-api:8080 | The url to get engagement data |
| GITLAB_API_URL | https://acmegit.com | The url to Gitlab |
| GITLAB_TOKEN | t | The Access Token for Gitlab |
| LODESTAR_LOGGING | DEBUG | Logging to the base source package | 
| PARTICIPANT_POSTGRESQL_USER | | The db user | 
| PARTICIPANT_POSTGRESQL_PASSWORD | | The db password |
| PARTICIPANT_POSTGRESQL_URL | | The jdbc url to the db |

## Deployment

See the deployment [readme](./deployment) for information on deploying to a OpenShift environment

## Running the application locally

### Postgresql 

A postgres database that is needed for development Is provided via [Testcontainers](https://www.testcontainers.org/). Testcontainers will also be initiated during tests. For deployment to a non-dev environment see the application.properties file.

### Local Dev

You can run your application in dev mode that enables live coding using:

```
export GITLAB_API_URL=https://gitlab.com/ 
export GITLAB_TOKEN=token
export ENGAGEMENT_API_URL=https://git-api.test.com 
mvn quarkus:dev
```

In dev mode the application uses [Testcontainers](https://www.testcontainers.org/) that automatically spins up a postgresql container so there is no need to configure a database. Docker is needed.

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.
