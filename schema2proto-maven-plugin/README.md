# schema2proto maven plugin

## Usage

### Generate proto from xsd

In your `build` section add:

2 mandatory parameters:
* `configFile` path to conversion config file. See https://github.com/entur/schema2proto/blob/master/schema2proto-lib/example_config/generateproto.yml for example
* `xsdFile` path to xsd file to convert

```
    <plugin>
        <groupId>no.entur</groupId>
        <artifactId>schema2proto-maven-plugin</artifactId>
        <version>1.0</version>
        <configuration>
            <configFile>netex_to_protobuf_config.yaml</configFile>
            <xsdFile>target/${netexVersion}/NeTEx_publication.xsd</xsdFile>
        </configuration>
        <executions>
            <execution>
                <id>generate-resources</id>
                <goals>
                    <goal>generate</goal>
                </goals>
            </execution>
        </executions>

    </plugin>
```

If you have enabled the backwards incompability check using protolock, here is an example of setting up the maven plugin: https://github.com/salesforce/proto-backwards-compat-maven-plugin

Note: Config expects proto.lock file to be placed in the root folder

```
            <plugin>
                <groupId>com.salesforce.servicelibs</groupId>
                <artifactId>proto-backwards-compatibility</artifactId>
                <version>${proto-backwards-compatibility.version}</version>
                <configuration>
                    <protoSourceRoot>target/proto</protoSourceRoot>
                    <lockDir>${basedir}</lockDir>
                    <options>--debug=true</options>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>backwards-compatibility-check</goal>
                        </goals>
                        <phase>process-resources</phase>
                    </execution>
                </executions>
            </plugin>
```

### Modify proto files

You can add, remove and modify both messages and message fields in proto files

In your `build` section add:

Single mandatory parameter:
* `configFile` path to modify config file. See https://github.com/entur/schema2proto/blob/master/schema2proto-lib/example_config/modifyproto.yml for example

```
    <plugin>
        <groupId>no.entur</groupId>
        <artifactId>schema2proto-maven-plugin</artifactId>
        <version>1.0</version>
        <configuration>
            <configFile>netex_to_protobuf_config.yaml</configFile>
        </configuration>
        <executions>
            <execution>
                <id>generate-resources</id>
                <goals>
                    <goal>modify</goal>
                </goals>
            </execution>
        </executions>

    </plugin>
```

See example usage at https://github.com/entur/netex-protobuf

.