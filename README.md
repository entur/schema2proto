# schema2proto  [![CircleCI](https://circleci.com/gh/entur/schema2proto.svg?style=svg)](https://circleci.com/gh/entur/schema2proto)

This tool does 2 things:

* Converts XML Schema files (.xsd) to Protocol Buffers (.proto). 
* Modifies existing proto files by adding, modifying and removing fields, messages etc. Also support for merging proto files using the same package

NOTE: This project depend on an artifact xsom-2.4.0-b190812-entur.jar . This artifact is not published to any repo (yet) but can be downloaded from
 https://github.com/entur/schema2proto/tree/develop/xsom-entur-jar or built from https://github.com/entur/jaxb-ri/tree/master/jaxb-ri/xsom .

## Usage

### Standalone (Only for converting XSD to PROTO)

See [standalone tool](schema2proto-lib/README.md) 

### Maven (Both converting XSD to PROTO and for modifying PROTO files

See [maven plugin](schema2proto-maven-plugin/README.md)


## Contribution

See code style [CODESTYLE.md](CODESTYLE.md)

## Licensing

EUPL, see [LICENSE](LICENSE.txt) and https://en.wikipedia.org/wiki/European_Union_Public_Licence

The schema2proto-wire module is a modified copy from https://github.com/square/wire/tree/master/wire-schema

The codebase was once based on xsd2thrift but has been completely rewritten.
