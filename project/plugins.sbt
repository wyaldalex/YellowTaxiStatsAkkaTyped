addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

//Scala Style
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

//SonarQuebe
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("com.sonar-scala" % "sbt-sonar" % "2.3.0")

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.1.1")

addDependencyTreePlugin

//For Deployment
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")