val scala3Version = "3.1.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "processing-language-server",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    run / fork := true,
    connectInput := true,
    libraryDependencies ++= Seq(
      "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.12.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "org.slf4j" % "jul-to-slf4j" % "1.7.32",
      "com.novocode" % "junit-interface" % "0.11" % "test"
    ),
    assembly / mainClass := Some("net.kgtkr.processingLanguageServer.main"),
    assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      val base = (assembly / baseDirectory).value.getAbsolutePath
      cp.filter(jar => jar.data.getAbsolutePath.startsWith(base + "/lib/"))
    }
  )
