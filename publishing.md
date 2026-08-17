## Publishing

This library publishes to GitHub Packages, Maven Central, and Maven Local for local testing.

### Versioning

Published artifact versions come from `gradle.properties`.

```properties
VERSION=0.1.1
```

Update the `VERSION` property in the repository root `gradle.properties` file before publishing a new release.

### GitHub Packages credentials

To publish to GitHub Packages, add the following properties to your Gradle properties file:

- `gpr.user`
- `gpr.key`

Typically this should go in `~/.gradle/gradle.properties`.

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

### Maven Central credentials

To publish to Maven Central, add the following properties to your Gradle properties file:

- `mavenCentralUsername`
- `mavenCentralPassword`
- `signingInMemoryKey`
- `signingInMemoryKeyPassword`

Typically this should go in `~/.gradle/gradle.properties`.

```properties
mavenCentralUsername=YOUR_CENTRAL_TOKEN_USERNAME
mavenCentralPassword=YOUR_CENTRAL_TOKEN_PASSWORD
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----...
signingInMemoryKeyPassword=YOUR_GPG_PASSPHRASE
```

### Publish to GitHub Packages

Run these tasks to publish everywhere configured by the project:

```bash
./gradlew :core:publish
./gradlew :ui-compose:publish
```

### Publish to Maven Central

Upload the published artifacts to Maven Central:

```bash
./gradlew publishToMavenCentral
```

Upload and automatically release the deployment to Maven Central:

```bash
./gradlew publishAndReleaseToMavenCentral
```

`publishToMavenCentral` uploads the deployment for validation. If automatic release is not enabled in Gradle, publish the validated deployment manually in the Central Portal.

### Publish to Maven Local

Use the Maven Local counterparts when you want to test the published artifacts locally:

```bash
./gradlew :core:publishToMavenLocal
./gradlew :ui-compose:publishToMavenLocal
```

Artifacts published with `publishToMavenLocal` are available from your local Maven repository for local development and verification.
