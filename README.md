# Tapis Notifications


## General Design

All notifications will be sent to RabbitMQ on the `tapis.notifications` exchange, which is a `topic` exchange. Routing keys
are in the format of `{tenant}.{service}.{action}.{UUID}`. The UUID specifier is optional, for instance the files 
service will send messages for all transfers routed by the UUID specifier of the transfer task. 

The front end api is a websocket


## Environment
The environmental variables: 

```
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
RABBITMQ_VHOST
```

must be set before running. They will default to:  

```
RABBITMQ_USERNAME=dev
RABBITMQ_PASSWORD=dev
RABBITMQ_VHOST=dev
```

if not set in the process. These are preset in the `docker-compose.yml` file provided for local dev. 


## Building 

## Running



