name := """misms-seed-project"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  //jdbc,
  //cache,
  ws,

  // Automatically translate plain English stories to BDD test cases
  "org.scalatest" % "scalatest_2.11" % "3.0.0-M1",
  "org.scalatestplus" % "play_2.11" % "1.4.0-M3",
  "info.cukes" % "cucumber-scala_2.11" % "1.2.2",

  // Integrate Swagger UI with Play! 2.x
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "com.wordnik" %% "swagger-jaxrs" % "1.3.12",
  "javax.ws.rs" % "jsr311-api" % "1.1.1",
  "org.mockito" % "mockito-core" % "1.9.5" % "test"

  /////// local publish ////////
  // follow the instructions below:
  // git clone https://github.com/mikalveli/swagger-core
  // cd swagger-core
  // git checkout 1.3.12-play2.4
  // cd modules/swagger-play2
  // sbt publish-local
  // then you can add the dependency here:
  // "com.wordnik" % "swagger-play2_2.11" % "1.3.12_play24",

  // AWS KCL and KPL
  // "com.amazonaws" % "amazon-kinesis-producer" % "0.9.0",
  //"com.amazonaws" % "amazon-kinesis-client" % "1.4.0",
  // Gilt Gfc Kinesis
  //"com.gilt" % "gfc-kinesis_2.11" % "0.0.10"
)

resolvers += "Sonatype-Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += Resolver.file("Local repo", file("./lib/.ivy2/local"))(Resolver.ivyStylePatterns)
resolvers += Resolver.mavenLocal

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
// routesGenerator := InjectedRoutesGenerator


fork in run := true