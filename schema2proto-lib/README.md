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
    --derivationBySubsumption <true|false>                                          enable derivation by subsumption
                                                                                    https://cs.au.dk/~amoeller/XML/schemas/xmlschema-inherit
                                                                                    ance.html
    --failIfRemovedFields <true|false>                                              when using backwards compatibility check via proto.lock
                                                                                    file, fail if proto fields are removed
    --forceProtoPackage <NAME>                                                      force all types in this package
    --ignoreOutputFields <packageName1/messageName1/fieldName1, packageName2/...>   output field names to ignore
    --includeFieldDocs <true|false>                                                 include documentation for fields in output, defaults to
                                                                                    true
    --includeMessageDocs <true|false>                                               include documentation of messages in output, defaults to
                                                                                    true
    --includeSourceLocationInDoc <true|false>                                       include xsd source location relative to source xsd file
                                                                                    in docs, defaults to false
    --includeValidationRules <true|false>                                           generate envoypropxy/protoc-gen-validate validation
                                                                                    rules from xsd rules
    --includeXsdOptions <true|false>                                                include message options describing the xsd type
                                                                                    hierarchy
    --inheritanceToComposition <true|false>                                         define each xsd extension base level as a message field
                                                                                    instead of copying all inherited fields
    --options <option1name:option1value,...>                                        add custom options to each protofile, ie
                                                                                    java_multiple_files:true
    --outputDirectory <DIRECTORYNAME>                                               path to output folder
    --outputFilename <FILENAME>                                                     name of output file
    --protoLockFile <FILENAME>                                                      Full path to proto.lock file
    --skipEmptyTypeInheritance <true|false>                                         skip types just redefining other types with a different
                                                                                    name
```

## Config parameters

Each parameter is explained in the example config file. The parameters have identical name on the command line as well as in the config file.

See [example configuration file with comments here](example_config) .
