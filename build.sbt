lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion    = "2.4.20"

enablePlugins(JavaAppPackaging)
// resolvers += Resolver.bintrayRepo("hseeberger", "maven")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.presheaf",
      scalaVersion    := "2.12.4"
    )),
    name := "presheaf2",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
//      "de.heikoseeberger" %% "accessus" % "0.1.0", // access logging, @see https://github.com/hseeberger/accessus

      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    )
  )
