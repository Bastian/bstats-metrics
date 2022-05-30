# bStats Java Metrics Classes

This repository contains the code for all Java-based Metrics classes.

## Code Generation

### Old way (pre 1.16)

~~The recommended way to include the Metrics classes is to use a build management tool like Gradle or Maven and
shade + relocate the required classes.~~

### New way (1.16 and later)

Since API 1.16, spigot implemented new solution to make plugins lighter. In order to get this library working, just add
it to `libraries` section of `plugin.yml` file and server will automatically download the dependency from Maven Central
and link it to all plugins that uses it.

```
libraries:
- org.bstats:bstats:3.0.0
```

Reference: [Click here](https://www.spigotmc.org/wiki/plugin-yml/#optional-attributes) (scroll a bit down).

---

However, to make bStats more accessible for beginners, a single file Metrics class is automatically generated on every
release and pushed to the `single-file` branch. This file can simply be copy-and-pasted.

To generate a Metrics class locally, you can run the `gradlew generateMetrics` command.
It will write the generated files into the `<platform>/build/generated/` directory.

## Publishing

Snapshots are automatically published on every push.

To publish a new release, one can simply remove the `-SNAPSHOT` suffix from the version in the `gradle.properties` file
and in the [`MetricsBase`](/base/src/main/java/org/bstats/MetricsBase.java) class.
This will automatically trigger a GitHub Action that
* Publishes a GitHub Release
* Pushes the new version to Maven Central
* Creates and pushes a new commit that updates the version by one patch level and append a `-SNAPSHOT` suffix (e.g., `2.3.4` -> `2.3.5-SNAPSHOT`).

## GitHub Action Secrets

For the GitHub Actions to properly work, one must configure the following [encrypted secrets](https://docs.github.com/en/actions/reference/encrypted-secrets):
* `OSSRH_USERNAME`: The username used to publish the Maven Central
* `OSSRH_TOKEN`: The password used to publish to Maven Central
* `SIGNING_KEY`: The PGP private key used to sign the built artifacts (can be obtained by running `gpg --export-secret-keys -a <key-id> > key.txt`)
* `SIGNING_PASSWORD`: The passphrase that protects the private key
