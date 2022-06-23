# Tapis Notifications

Tapis Notifications Service

There are three primary branches: *local*, *dev*, and *main*.

All changes should first be made in the branch *local*.

When it is time to deploy to the **DEV** kubernetes environment
run the jenkins job TapisJava->3_ManualBuildDeploy->notifications.

This job will:
* Merge changes from *local* to *dev*
* Build, tag and push docker images
* Deploy to **DEV** environment
* Push the merged *local* changes to the *dev* branch.

To move docker images from **DEV** to **STAGING** run the following jenkins job:
* TapisJava->2_Release->promote-dev-to-staging

To move docker images from **STAGING** to **PROD** run the following jenkins job:
* TapisJava->2_Release->promote-staging-to-prod-ver

## General Design

The basic model of the Notification Event is based on the CloudEvents spec

[CloudEvents](https://github.com/cloudevents/spec/blob/v1.0.1/spec.md)


## Setup the database
In the docker container running the postgres database execute the following command

```
createdb -U dev notifications
```


## Environment
The environmental variables: 

```
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
RABBITMQ_VHOST
DB_USERNAME
DB_PASSWORD
DB_NAME
```

must be set before running. They will default to:  

```
RABBITMQ_USERNAME=dev
RABBITMQ_PASSWORD=dev
RABBITMQ_VHOST=dev
DB_USERNAME=dev
DB_PASSWORD=dev
DB_NAME=notifications
```

if not set in the process. These are preset in the `docker-compose.yml` file provided for local dev. 


### Run tests

The integration tests are configured to use the `test` database created above.

```
mvn clean install -DskipITs=false -DAPP_ENV=test
```

### Run migrations

```
mvn -pl migrations flyway:clean flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/notifications -Dflyway.user=dev -Dflyway.password=dev -U
```



