resolvers ++= Seq(
  "Socrata Cloudbees" at "https://repository-socrata-oss.forge.cloudbees.com/release"
)

addSbtPlugin("com.socrata" % "socrata-sbt-plugins" % "1.6.1")

