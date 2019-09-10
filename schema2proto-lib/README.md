# schema2proto standalone tool

## Usage

```
java Schema2Proto [OPTIONS] XSDFILE
Generate proto files from xsd file. Either --configFile or --outputDirectory must be specified.
    --configFile <outputFilename>                                                   name of configfile specifying these parameters (instead
                                                                                    of supplying them on the command line)
    --customImportLocations <folder1,folder2,...>                                   root folder for additional imports
    --customImports <filename1,filename2,...>                                       add additional imports
    --customNameMappings <cake:kake,...>                                            translate message and field names
    --customTypeMappings <a:b,x:y>                                                  represent schema types as specific output types
    --defaultProtoPackage <NAME>                                                    default proto package of the output file if no xsd
                                                                                    target defaultProtoPackage is specified
    --forceProtoPackage <NAME>                                                      force all types in this package
    --ignoreOutputFields <packageName1/messageName1/fieldName1, packageName2/...>   output field names to ignore
    --includeFieldDocs <true|false>                                                 include documentation for fields in output, defaults to
                                                                                    true
    --includeMessageDocs <true|false>                                               include documentation of messages in output, defaults to
                                                                                    true
    --includeSourceLocationInDoc <true|false>                                       include xsd source location in docs, defaults to true
    --includeValidationRules <true|false>                                           generate envoypropxy/protoc-gen-validate validation
                                                                                    rules from xsd rules
    --inheritanceToComposition <true|false>                                         define each xsd extension base level as a message field
                                                                                    instead of copying all inherited fields
    --options <option1name:option1value,...>                                        translate message and field names
    --outputDirectory <DIRECTORYNAME>                                               path to output folder
    --outputFilename <FILENAME>                                                     name of output file
    --skipEmptyTypeInheritance <true|false>                                         skip types just redefining other types with a different
                                                                                    name
```

TODO update

## Config parameters

Each parameter is explained in the example config file. The parameters have identical name on the command line as well as in the config file.

See [example configuration file with comments here](example_config/config.yml) .