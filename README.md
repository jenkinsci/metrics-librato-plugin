# Codahale Metrics Librato Plugin

This plugin streams [Codahale Metrics](http://wiki.jenkins-ci.org/display/JENKINS/Codahale+Metrics+Plugin) to
[Librato](https://metrics.librato.com/).

See also this [plugin's wiki page][wiki]

# Environment

The following build environment is required to build this plugin

* `java-1.6` and `maven-3.0.5`

# Build

To build the plugin locally:

    mvn clean verify

# Release

To release the plugin:

    mvn release:prepare release:perform -B

# Test local instance

To test in a local Jenkins instance

    mvn hpi:run

  [wiki]: http://wiki.jenkins-ci.org/display/JENKINS/Codahale+Metrics+Librato+Plugin
