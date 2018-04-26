
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

val additionalScalacOptions = Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-unused-import",
  "-Ypartial-unification",
//  "-Xfatal-warnings",
  "-Xlint"
)

val projectSettings = Seq(
  name := "",
  description := "",
  version := "1.0",
  scalaVersion := "2.11.9",
  organization := "Sebastian Bach",
  scalacOptions ++= additionalScalacOptions
)

val dependencies = Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "io.netty" % "netty-tcnative" % "2.0.8.Final" classifier "osx-x86_64",
  "ch.qos.logback" % "logback-classic" % "1.0.13"
)

lazy val root = (project in file("."))
  .settings(projectSettings: _*)
  .settings(libraryDependencies ++= dependencies)
