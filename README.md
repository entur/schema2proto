# schema2proto  [![CircleCI](https://circleci.com/gh/entur/schema2proto.svg?style=svg)](https://circleci.com/gh/entur/schema2proto)

This tool converts XML Schema files (.xsd) to Protocol Buffers (.proto). It was once based on xsd2thrift but has been completely rewritten.

You can also use it to modify proto files by removing unnecessary packages, messages and fields

## Usage

### Standalone

See [standalone tool](schema2proto-lib/README.md)

### Maven

See [maven plugin](schema2proto-maven-plugin/README.md)


## Contribution

See code style [CODESTYLE.md](CODESTYLE.md)

## Licensing

EUPL, see [LICENSE](LICENSE.txt) and https://en.wikipedia.org/wiki/European_Union_Public_Licence

The schema2proto-wire module is a modified copy from https://github.com/square/wire/wire-schema

.