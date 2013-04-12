name := "mineral-spider"

version := "0.1.0"

organization := "com.nowanswers"

scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
    "com.nowanswers" %% "chemistry" % "0.3.6",
    "com.typesafe.akka" %% "akka-actor" % "2.1.2",
    "io.spray" % "spray-client" % "1.1-M7",
    "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1" withSources(),
    "org.scalesxml" %% "scales-xml" % "0.4.5",
    "org.mongodb" %% "casbah" % "2.5.0",
    "com.novus" %% "salat" % "1.9.2-SNAPSHOT",
    "org.mockito" % "mockito-all" % "1.9.0" % "test",
    "org.specs2" %% "specs2" % "1.12.3" % "test"
)

resolvers ++= Seq(
    "spray repo" at "http://repo.spray.io",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Scala-Tools Maven2 Snapshots Repository" at "https://oss.sonatype.org/content/groups/scala-tools",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
