# Datareplication
![Build Status](https://github.com/datareplication/datareplication-java/actions/workflows/build.yml/badge.svg)
![Maven Central Version](https://img.shields.io/maven-central/v/io.datareplication/datareplication)

## Documentation
Learn more on the [Datareplication homepage](https://www.datareplication.io)

## License

Licensed under [MIT](LICENSE)

## Publishing

To publish a new version of the library:

* Update the [Changelog](CHANGELOG.md) with the new version and a summary of changes.
* Create a [Snapshot](https://github.com/datareplication/datareplication-java/actions/workflows/release-snapshot.yml)
* Create a [Release](https://github.com/datareplication/datareplication-java/actions/workflows/release.yml)
* After the release is published, update the version in the `build.gradle.kts` file to the next patch version.
  For example, if the current version is `1.0.0`, update it to `1.0.1`.
