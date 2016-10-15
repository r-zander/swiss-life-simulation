# Misfit Micro-Service Seed Project

[![Join the chat at https://gitter.im/truman-misfit/microservice-seed-project](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/truman-misfit/microservice-seed-project?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
![Travis-CI](https://travis-ci.org/truman-misfit/microservice-seed-project.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/truman-misfit/microservice-seed-project/badge.svg)](https://coveralls.io/r/truman-misfit/microservice-seed-project)

This is a archetype project (or seed project) for building up micro-service in Misfit Cloud.

# Features
* BDD with cucumber-jvm
* Documentation with Swagger-API (internal)
* Stream module with Async Kinesis Client (can be used as a Message Queue)
* Travis-CI & Coveralls

# Requirements
* Java 1.8.0_05 or above
* Scala 2.11.6 or above
* Activator 1.3.4 or above
* sbt 0.13.8 or above

# Install
**1. Install [JavaSE](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (Java 1.8.0_05 or above)**

**2. Install [Scala](http://www.scala-lang.org/download/) (Scala 2.11.6 or above)**

**3. Install [Activator](https://www.typesafe.com/get-started) (Activator 1.3.4 or above)**

Both mini-package and full-package are OK.

**4. Fix local dependency**

Since the latest public version of cucumber-plugin repo has some compatible problems, you need to compile and publish the xsbt-cucumber-plugin to local by yourself. The repo is [turman-misfit/xsbt-cucumber-pulgin](https://github.com/truman-misfit/xsbt-cucumber-plugin).

* **Step 1**. `git clone https://github.com/truman-misfit/xsbt-cucumber-plugin.git`
* **Step 2**. `cd xsbt-cucumber-plugin`
* **Step 3**. `sbt publish-local`

Then you will find the dependency published in the directory of your defualt ivy local path, for example, `~/.ivy2/local` on my MacBook Pro.

The configuration below in the `play.plugins` file will default look up the specific local ivy path.

```
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

addSbtPlugin("templemore" % "sbt-cucumber-plugin" % "0.9.0-SNAPSHOT")
```

**5. Git pull repo**

* **Step 1**. pull repo
```
git clone https://github.com/truman-misfit/microservice-seed-project.git

```

* **Step 2**. activator run
```
cd misms-seed-project
activator run
```
Open the page `http://localhost:9000/swagger` to checkout whether swagger is up and running.

Or you can test cucumber by following the commands below:
```
activator cucumber
```

# Future
**1. Unified Cache Module**

Centralized Redis or ElastiCache

**2. Time series data storage module**

A Time series logging storage system based on DynamoDB and S3.

**3. Docker Compose**

Docker Compose and Activator Package integration.

**4. Services Discovery**

A distributive services discovery module based on Zookeeper.

# Author
truman@misfit.com
