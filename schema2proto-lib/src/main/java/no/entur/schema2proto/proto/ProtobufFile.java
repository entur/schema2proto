package no.entur.schema2proto.proto;

import java.util.List;

public class ProtobufFile {
	private List<ProtobufImport> imports;

	private String packageName;

	private List<ProtobufOption> options;

	private List<ProtobufMessage> messages;

	private List<ProtobufEnumeration> enums;

	private List<ProtobufService> services;
}
