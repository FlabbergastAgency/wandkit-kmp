## Publishing

WandKit is published to Maven Central under the `agency.flabbergast.wandkit` group:

- `agency.flabbergast.wandkit:core`
- `agency.flabbergast.wandkit:ui-compose`

Publishing uses the [vanniktech maven-publish plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/) and the Sonatype Central Portal.

### One-time setup

1. **Central Portal account.** Sign in at https://central.sonatype.com.
2. **Verify the namespace.** Under *Namespaces*, add `agency.flabbergast`. The portal gives you a verification key; add it as a DNS TXT record on `flabbergast.agency`, then click *Verify*.
3. **User token.** Under *Account → Generate User Token*, create a token. It gives you a username and a password.
4. **GPG signing key.** Maven Central requires every artifact to be signed.

   ```bash
   gpg --full-generate-key                      # RSA 4096, no expiry or a long one
   gpg --list-secret-keys --keyid-format SHORT  # note the 8-char key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --export-secret-keys --armor <KEY_ID>    # used for signingInMemoryKey below
   ```

5. **Credentials.** Put these in `~/.gradle/gradle.properties` (never in the repo):

   ```properties
   mavenCentralUsername=<token username>
   mavenCentralPassword=<token password>

   signingInMemoryKey=<the armored private key, newlines replaced with \n>
   signingInMemoryKeyId=<8-char key id>
   signingInMemoryKeyPassword=<key passphrase>
   ```

   On CI, use the environment variables `ORG_GRADLE_PROJECT_mavenCentralUsername`, `ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyId` and `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` instead.

### Releasing a version

1. Set `VERSION` in the root `gradle.properties`.
2. Run the publish on macOS (the iOS targets only build there):

   ```bash
   ./gradlew publishToMavenCentral --no-configuration-cache
   ```

   This uploads and validates the deployment but doesn't release it. Check it under *Deployments* on https://central.sonatype.com and click *Publish*.

   Once you trust the process, `./gradlew publishAndReleaseToMavenCentral --no-configuration-cache` uploads and releases in one step.
3. Releases usually appear on Maven Central within 10–30 minutes. Search can take longer.

Versions on Maven Central are permanent: a released version can't be changed or deleted.

### GitHub Packages

The same artifacts can also be published to GitHub Packages. Add a GitHub username and a token with the `write:packages` scope to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

On CI, `GITHUB_ACTOR` and `GITHUB_TOKEN` are used instead. Then run:

```bash
./gradlew publishAllPublicationsToGitHubPackagesRepository --no-configuration-cache
```

Don't use the plain `./gradlew publish` task: it publishes to every configured repository, Maven Central included.

### Publish to Maven Local

To test the artifacts locally:

```bash
./gradlew publishToMavenLocal
```
