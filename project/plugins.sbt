resolvers ++= Seq(
  Resolver.url("socrata releases", url("https://repo.socrata.com/artifactory/ivy-libs-release/"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.1")
