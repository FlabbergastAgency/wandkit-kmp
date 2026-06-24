## Publishing

This library publishes to GitHub Packages and can also be published to Maven Local for local testing.

### Versioning

Published artifact versions come from `gradle.properties`.

```properties
VERSION=0.1.1
```

Update the `VERSION` property in the repository root `gradle.properties` file before publishing a new release.

### Required credentials

To publish to GitHub Packages, add the following properties to your Gradle properties file:

- `gpr.user`
- `gpr.key`

Typically this should go in `~/.gradle/gradle.properties`.

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

### Publish to configured repositories

Run these tasks to publish everywhere configured by the project:

```bash
./gradlew :core:publish
./gradlew :uiCompose:publish
```

### Publish to Maven Local

Use the Maven Local counterparts when you want to test the published artifacts locally:

```bash
./gradlew :core:publishToMavenLocal
./gradlew :uiCompose:publishToMavenLocal
```

Artifacts published with `publishToMavenLocal` are available from your local Maven repository for local development and verification.
