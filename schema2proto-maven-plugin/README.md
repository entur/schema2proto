# schema2proto maven plugin

## Usage

In your `build` section add:

2 mandatory parameters:
* `configFile` path to conversion config file. See [../schema2proto-lib/example_config/config.yml] for example
* `xsdFile` path to xsd file to convert

```
    <plugin>
        <groupId>no.entur</groupId>
        <artifactId>schema2proto-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
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

See example usage at https://github.com/entur/netex-protobuf