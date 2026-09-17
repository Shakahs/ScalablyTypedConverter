<p align="center">
    <img width="300" src="https://raw.githubusercontent.com/ScalablyTyped/Converter/d946f2a8a894f318f4ad6d4786fadedb08267923/website/static/img/logo-1.svg"/>
</p>
<h1 align="center">ScalablyTyped</h1>
<p align="center"><i>Typescript to Scala.js converter</i></p>
<p align="center">
    <a href="https://scalablytyped.org/docs/readme">
        www.scalablytyped.org  
    </a> 
</p>
<p align="center">
  <a href="https://gitter.im/ScalablyTyped/community">
    <img src="https://badges.gitter.im/ScalablyTyped/community.svg"/>
  </a>
  <a href="https://circleci.com/gh/ScalablyTyped/Converter">
    <img src="https://img.shields.io/circleci/build/github/ScalablyTyped/Converter?logo=circleci&style=flat"/>
  </a>
  <a href="https://github.com/scala/scala/releases">
    <img src="https://img.shields.io/badge/scala.js-1.0.0+-red.svg?logo=scala&logoColor=red"/>
  </a>
  <a href="https://central.sonatype.com/search?namespace=io.kinoplan.scalablytyped">
    <img src="https://img.shields.io/maven-central/v/io.kinoplan.scalablytyped/cli_3.svg?label=Maven%20Central"/>
  </a>
</p>

## Quick Start

The converter is a command line tool (Scala 3, JDK 21+). The sbt plugin has been removed.

```sh
sbt "cli/run --directory <dir with package.json and node_modules> --flavour scalajs-react"
```

Or publish it (`sbt publishM2`) and run `org.scalablytyped.converter.cli.Main` from
`io.kinoplan.scalablytyped:cli_3`. Run with `--help` to list the options.
`--parallelism <n>` sets how many libraries are converted at the same time (default: the number of cores, at least 2); `1` converts them one by one. Progress is printed as packages finish.

Generated facades target Scala 3 by default; pass `--scala 2.13.x` to generate Scala 2.13 code.
