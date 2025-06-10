# Horizon Backend

This repository contains the backend microservices for the Horizon application.

## Integration Testing

An end-to-end integration test for the "forget user" saga is located in `userservice/src/test/java/com/horizon/userservice/integration/SagaSynchronizationTest.java`.

This test uses [Testcontainers](https://www.testcontainers.org/) to create a complete, isolated testing environment that includes all the necessary microservices (`userservice`, `eventservice`, `rsvpservice`), their databases, and a RabbitMQ instance.

### Prerequisites

Before running the integration test, you must build the Docker images for each of the microservices. You can do this by running the following command from the root of the project:

```bash
./gradlew :userservice:bootBuildImage --imageName=horizon/userservice && \
./gradlew :eventservice:bootBuildImage --imageName=horizon/eventservice && \
./gradlew :rsvpservice:bootBuildImage --imageName=horizon/rsvpservice
```

This will create the necessary Docker images in your local Docker registry.

### Running the Test

Once the images have been built, you can run the integration test using the following command:

```bash
./gradlew :userservice:test --tests "com.horizon.userservice.integration.SagaSynchronizationTest"
```

### Important Notes

*   **Resource Usage**: This test starts up a significant number of Docker containers and may be resource-intensive. If you experience timeouts or other issues during the test run, it may be due to a lack of available CPU or memory on your host machine. Ensure that Docker has been allocated sufficient resources.
*   **Environmental Issues**: The test has been written to be as robust as possible, but it may still be sensitive to the host environment. If you continue to experience issues after allocating sufficient resources, you may need to investigate your local Docker and network configuration. 