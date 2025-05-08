# Datareplication

[![Maven Central Version](https://img.shields.io/maven-central/v/io.datareplication/datareplication)](https://central.sonatype.com/artifact/io.datareplication/datareplication)
[![javadoc](https://javadoc.io/badge2/io.datareplication/datareplication/javadoc.svg)](https://datareplication.io/datareplication-java/)
[![javadoc](https://img.shields.io/badge/spec-datareplication.io-purple)](https://datareplication.io/spec/)
![Build Status](https://github.com/datareplication/datareplication-java/actions/workflows/build.yml/badge.svg)

## Documentation

Learn more on the [Datareplication homepage](https://www.datareplication.io)

## License

Licensed under [MIT](LICENSE)

## Updating the Changelog

Update the changelog when you make changes to the library, when you make a change that justifies a changelog entry.

When adding a feature that justifies a minor version bump also update the version number in the `build.gradle.kts` and
changelog.

## Publishing a Snapshot
Snapshot releases can be published at any time using the
[`release-snapshot`](https://github.com/datareplication/datareplication-java/actions/workflows/release-snapshot.yml)
workflow. The version set in the source code is always the next version to be released.

## Publishing a Release

To publish a new version of the library:

* Double check the version numbers in the changelog entry and `build.gradle.kts`
* Double check the summary of changes in the changelog entry
    * If there were only dependabot updates, mention something like "Dependency updates" so the changelog isn't empty
* Set the release date for the current changelog entry
* Run the [release workflow](https://github.com/datareplication/datareplication-java/actions/workflows/release.yml)
* Update the version in `build.gradle.kts` to the next patch version (e.g. from `1.0.0` to `1.0.1`)
* Add a new changelog entry with the next patch version and the release date `Unreleased`
