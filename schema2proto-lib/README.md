# schema2proto standalone tool

## Usage

```
Usage: java schema2proto-<VERSION>.jar [--output=FILENAME]
                           [--package=NAME] filename.xsd

  --configFile=FILENAME           : path to configuration file

OR

  --filename=FILENAME             : store the result in FILENAME instead of standard output
  --package=NAME                  : set namespace/package of the output file
  --nestEnums=true|false          : nest enum declaration within messages that reference them, only supported by protobuf, defaults to true
  --splitBySchema=true|false      : split output into namespace-specific files, defaults to false
  --customTypeMappings=a:b,x:y    : represent schema types as specific output types (regex support)
  --customNameMappings=cake:ake   : represent schema types as specific output types (regex support)
  --typeInEnums=true|false        : include type as a prefix in enums, defaults to true
  --includeMessageDocs=true|false : include documentation of messages in output, defaults to true
  --includeFieldDocs=true|false   : include documentation for fields in output, defaults to true
```

