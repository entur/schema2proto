/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema;

/*-
 * #%L
 * schema2proto-wire
 * %%
 * Copyright (C) 2019 Entur
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.ServiceElement;

public final class Service {
	private final ProtoType protoType;
	private final Location location;
	private final String name;
	private final String documentation;
	private final ImmutableList<Rpc> rpcs;
	private final Options options;

	private Service(ProtoType protoType, Location location, String documentation, String name, ImmutableList<Rpc> rpcs, Options options) {
		this.protoType = protoType;
		this.location = location;
		this.documentation = documentation;
		this.name = name;
		this.rpcs = rpcs;
		this.options = options;
	}

	static Service fromElement(ProtoType protoType, ServiceElement element) {
		ImmutableList<Rpc> rpcs = Rpc.fromElements(element.getRpcs());
		Options options = new Options(Options.SERVICE_OPTIONS, element.getOptions());

		return new Service(protoType, element.getLocation(), element.getDocumentation(), element.getName(), rpcs, options);
	}

	public Location location() {
		return location;
	}

	public ProtoType type() {
		return protoType;
	}

	public String documentation() {
		return documentation;
	}

	public String name() {
		return name;
	}

	public ImmutableList<Rpc> rpcs() {
		return rpcs;
	}

	/** Returns the RPC named {@code name}, or null if this service has no such method. */
	public Rpc rpc(String name) {
		for (Rpc rpc : rpcs) {
			if (rpc.name().equals(name)) {
				return rpc;
			}
		}
		return null;
	}

	public Options options() {
		return options;
	}

	void link(Linker linker) {
		linker = linker.withContext(this);
		for (Rpc rpc : rpcs) {
			rpc.link(linker);
		}
	}

	void linkOptions(Linker linker) {
		linker = linker.withContext(this);
		for (Rpc rpc : rpcs) {
			rpc.linkOptions(linker);
		}
		options.link(linker);
	}

	void validate(Linker linker) {
		linker = linker.withContext(this);
		for (Rpc rpc : rpcs) {
			rpc.validate(linker);
		}
	}

	Service retainAll(Schema schema, MarkSet markSet) {
		// If this service is not retained, prune it.
		if (!markSet.contains(protoType)) {
			return null;
		}

		ImmutableList.Builder<Rpc> retainedRpcs = ImmutableList.builder();
		for (Rpc rpc : rpcs) {
			Rpc retainedRpc = rpc.retainAll(schema, markSet);
			if (retainedRpc != null && markSet.contains(ProtoMember.get(protoType, rpc.name()))) {
				retainedRpcs.add(retainedRpc);
			}
		}

		return new Service(protoType, location, documentation, name, retainedRpcs.build(), options.retainAll(schema, markSet));
	}

	static ImmutableList<Service> fromElements(String packageName, List<ServiceElement> elements) {
		ImmutableList.Builder<Service> services = ImmutableList.builder();
		for (ServiceElement service : elements) {
			ProtoType protoType = ProtoType.get(packageName, service.getName());
			services.add(Service.fromElement(protoType, service));
		}
		return services.build();
	}

	static ImmutableList<ServiceElement> toElements(List<Service> services) {
		ImmutableList.Builder<ServiceElement> elements = new ImmutableList.Builder<>();
		for (Service service : services) {
			elements.add(new ServiceElement(service.location, service.name, service.documentation, Rpc.toElements(service.rpcs), service.options.toElements()));
		}
		return elements.build();
	}
}
