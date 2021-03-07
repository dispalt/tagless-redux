ThisBuild / scalaVersion := "2.13.5"
ThisBuild / organization := "com.dispalt"

val gh = GitHubSettings(org = "dispalt", proj = "tagless-redux", publishOrg = "com.dispalt", license = apache)

val libs = org.typelevel.libraries
  .add("cats", "2.3.1")
  .add("scalatestplus", version = "3.1.0.0-RC2", org = "org.scalatestplus", "scalatestplus-scalacheck")

val taglessV = "0.12"
val akkaV    = "2.6.13"
val boopickleV    = "1.3.1"
val scodecBitsV    = "1.1.24"
val scodecCoreV    = "1.11.7"
val chillV   = "0.9.5"
val scalaV = "2.13.5"

ThisBuild / intellijPluginName := "tagless-redux-ijext"
ThisBuild / intellijBuild := "203.5981.41"

lazy val root = (project in file("."))
  .settings(noPublishSettings)
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .aggregate(macros, tests, `encoder-macros`, `encoder-kryo`, `intellij-ijext`, `encoder-akka`, `encoder-boopickle`)

lazy val macros = (project in file("macros"))
  .settings(
    name := "tagless-redux-macros",
    libraryDependencies += "org.typelevel" %% "cats-tagless-macros" % taglessV,
    macroSettings,
    resourceGenerators in Compile += Def.task {
      val rootFolder = (resourceManaged in Compile).value / "META-INF"
      rootFolder.mkdirs()

      IO.write(rootFolder / "intellij-compat.json", s"""{
         |  "artifact": "com.dispalt % tagless-redux-ijext_2.12 % ${version.value}"
         |}""".stripMargin)

      Seq(rootFolder / "intellij-compat.json")
    }
  )
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .settings(libs.dependency("cats-core"))
  .settings(libs.testDependencies("scalatest", "cats-free", "cats-effect"))
  .settings(scalaMacroDependencies(libs))

lazy val tests = (project in file("tests"))
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .settings(name := "tagless-redux-tests", noPublishSettings, macroSettings)
  .dependsOn(macros % "compile->compile;test->test")

lazy val `encoder-macros` = (project in file("encoder-macros"))
  .settings(
    name := "tagless-redux-encoder-macros",
    libraryDependencies ++= Seq("org.typelevel" %% "cats-tagless-core" % taglessV),
    macroSettings
  )
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .settings(scalaMacroDependencies(libs))
  .settings(libs.testDependencies("scalatest", "cats-free", "cats-effect"))

lazy val `encoder-kryo` = (project in file("encoder-kryo"))
  .settings(
    name := "tagless-redux-encoder-kryo",
    libraryDependencies ++= Seq("com.twitter" %% "chill-bijection" % chillV),
    macroSettings
  )
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .settings(libs.testDependencies("scalatest", "cats-free", "cats-effect"), scalaMacroDependencies(libs))
  .dependsOn(`encoder-macros` % "test->test;compile->compile", macros % "test->test")

lazy val `encoder-akka` = (project in file("encoder-akka"))
  .settings(
    name := "tagless-redux-encoder-akka",
    libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % akkaV),
    macroSettings
  )
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .settings(libs.testDependencies("scalatest", "cats-free", "cats-effect"), scalaMacroDependencies(libs))
  .dependsOn(`encoder-macros` % "test->test;compile->compile", macros % "test->test")

  lazy val `encoder-boopickle` = (project in file("encoder-boopickle"))
  .settings(
    name := "tagless-redux-encoder-boopickle",
    libraryDependencies ++= Seq(
      "io.suzaku" %% "boopickle" % boopickleV,
      "org.scodec" %% "scodec-bits" % scodecBitsV,
    "org.scodec" %% "scodec-core" % scodecCoreV,
      ),
    macroSettings
  )
  .settings(commonSettings ++ buildSettings ++ publishSettings)
  .settings(libs.testDependencies("scalatest", "cats-free", "cats-effect"), scalaMacroDependencies(libs))
  .dependsOn(`encoder-macros` % "test->test;compile->compile", macros % "test->test")

lazy val `intellij-ijext` = (project in file("intellij-ijext"))
  .enablePlugins(SbtIdeaPlugin)
  .settings(commonSettings ++ publishSettings)
  .settings(
    name := "tagless-redux-ijext",
    intellijPluginName := name.value,
    intellijPlugins += "org.intellij.scala".toPlugin,
    packageMethod := PackagingMethod.Standalone(),
    scalaVersion := scalaV,
    crossScalaVersions := Seq(scalaV),
    patchPluginXml := pluginXmlOptions { xml =>
      xml.version    = version.value
      xml.sinceBuild = (intellijBuild in ThisBuild).value
      xml.untilBuild = "203.*"
    },
    resourceGenerators in Compile += Def.task {
      val rootFolder = (resourceManaged in Compile).value / "META-INF"
      rootFolder.mkdirs()
      val fileOut = rootFolder / "intellij-compat.xml"

      IO.write(fileOut, s"""
          |<!DOCTYPE intellij-compat PUBLIC "Plugin/DTD"
          |        "https://raw.githubusercontent.com/JetBrains/intellij-scala/idea183.x/scala/scala-impl/src/org/jetbrains/plugins/scala/components/libextensions/intellij-compat.dtd">
          |<intellij-compat>
          |    <id>dispalt.taglessRedux</id>
          |    <name>Tagless Intellij Support</name>
          |    <description>Provides an autoFunctorK, finalAlg, kryoEncoder, akkaEncoder injector for tagless programs</description>
          |    <version>${version.value}</version>
          |    <vendor>tagless-redux</vendor>
          |    <ideaVersion since-build="2020.3.0" until-build="2020.10.0">
          |        <extension interface="org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector"
          |                   implementation="com.dispalt.tagless.FunctorKInjector">
          |            <name>Tagless macro support</name>
          |            <description>FunctorK injector</description>
          |        </extension>
          |    </ideaVersion>
          |</intellij-compat>
          """.stripMargin)

      Seq(fileOut)
    }
  )

  lazy val paradisePlugin = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch))
      case _ =>
        // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
        // https://github.com/scala/scala/pull/6606
        Nil
    }
  }

lazy val macroSettings: Seq[Def.Setting[_]] = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  libraryDependencies ++= paradisePlugin.value,
)

lazy val buildSettings = sharedBuildSettings(gh, libs) ++ Seq(
  crossScalaVersions := Seq(scalaVersion.value, libs.vers("scalac_2.13")),
) ++ scalacAllSettings

lazy val commonSettings = sharedCommonSettings ++ Seq(
  parallelExecution in Test := false,
  developers := List(
    Developer("Dan Di Spaltro", "@dispalt", "dan.dispaltro@gmail.com", new java.net.URL("http://dispalt.com"))
  ),
  addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.11.3").cross(CrossVersion.full)),
) ++
  unidocCommonSettings


lazy val publishSettings = sharedPublishSettings(gh) ++ sharedReleaseProcess
