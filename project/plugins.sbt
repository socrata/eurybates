resolvers ++= Seq(
  Resolver.url("socrata releases", url("https://repo.socrata.com/artifactory/ivy-libs-release/"))(Resolver.ivyStylePatterns),
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.socrata" % "socrata-sbt-plugins" % "1.6.8")
