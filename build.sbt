name := "scatter"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

organization := "org.scatter"




resolvers += "akka-repo" at "http://repo.akka.io/releases/"

resolvers += "spray-repo" at "http://repo.spray.cc/"





libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3.1"

libraryDependencies += "cc.spray" % "spray-server" % "0.9.0"

libraryDependencies += "cc.spray" %% "spray-json" % "1.1.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.7.1" % "test"





publishTo <<= version { version =>
    Some(Resolver.file("file", new File("/maven/") / {
        if (version.trim.endsWith("SNAPSHOT")) "snapshots/" else "releases/"
    }))
}

publishMavenStyle := true

