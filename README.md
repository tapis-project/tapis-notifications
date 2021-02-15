# Tapis Notifications


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



