# Datareplication
![Build Status]([https://github.com/datareplication/datareplication-java/actions/workflows/maven-publish.yml/badge.svg](https://github.com/datareplication/datareplication-java/actions/workflows/build.yml/badge.svg))

## Documentation
Learn more on the [Datareplication homepage](https://www.datareplication.io)

## License

Licensed under MIT (see LICENSE file)

## Publishing

To publish a release, update the version number in `build.gradle.kts`, then create a new version tag pointing to the latest commit in `master`.

The GitHub action will automatically build and publish the release to Maven Central and GitHub Pages, then create a GitHub Release.

Tags with a prerelease version (e.g. `-alpha.1`) will be marked as prereleases in GitHub and will not be linked from the `/stable` link in the docs.

Credits to: https://github.com/NJAldwin/maven-central-test/blob/master/README.md
