resolvers ++= Seq(
  "socrata releases" at "https://repo.socrata.com/artifactory/libs-release",
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.socrata" % "socrata-sbt-plugins" % "1.6.1")
