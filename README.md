# IAM Integration Testing with Microcks

This repository shows how to perform integration testing for a [Keycloak](https://www.keycloak.org) extension that needs
to interact with an external CRM system. The API of the external CRM system is mocked with
[Microcks](https://microcks.io). Thanks to [Testcontainers](https://testcontainers.com), and the corresponding modules
for Keycloak and Microcks, local execution of the integration tests is straightforward.

See class [`CrmTest`](src/test/java/de/codecentric/iam/crm/CrmTest.java) for an entrypoint to understanding the code.
Run integration tests with `mvn clean test`.