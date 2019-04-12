name := "bubblegumapp"
 
version := "1.0" 
      
lazy val `bubblegumapp` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += Resolver.mavenLocal      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )
libraryDependencies += "io.hbt.bubblegum.core" % "bubblegum-core" % "1.0.1"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.6"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

      
