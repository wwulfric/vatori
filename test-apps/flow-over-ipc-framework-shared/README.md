# Flow over IPC Shared Validation Fixture

This test-only Maven module models an application-owned shared reactor
dependency. Its filtered classpath resource is consumed by
`flow-over-ipc-framework-app` so explicit macOS acceptance can verify
dependent-module resource and POM reload behavior.

It is not a framework module and production code must not depend on it.
