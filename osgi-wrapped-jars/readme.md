# Wrap OSGi JARs

If you need a JAR dependency that doesn't have the OSGi metadata, always try to get this added at source.
However, sometimes it is necessary or useful to wrap something manually first.
For this, you can use the [KARAF tool](https://svn.apache.org/repos/asf/karaf/site/production/manual/latest/creating-bundles.html):

Wrap the jar with `bundle:install wrap:mvn:<mvn-coordiantes>`, e.g.:

```sh
bundle:install wrap:mvn:io.micrometer/micrometer-registry-prometheus/1.9.3
```

Then find the JAR in the Karaf `cache/bundles` directory.