# Code style definitions

Code style and import order is governed by an Eclipse JDT formatter definition. It can be imported into IntelliJ as well. 

## Maven setup

Spotless is used to check and format code in Maven builds.

### Manual check/cleanup

run `mvn spotless:check`or `mvn spotless:apply`


See https://github.com/diffplug/spotless/blob/master/plugin-maven/README.md

### Integrated part of build 

By default the code style check is done before compilation. 


## Eclipse setup

https://github.com/diffplug/spotless/blob/master/ECLIPSE_SCREENSHOTS.md


## IntelliJ setup

* To use in IntelliJ there are numerous ways, have a look at https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter
