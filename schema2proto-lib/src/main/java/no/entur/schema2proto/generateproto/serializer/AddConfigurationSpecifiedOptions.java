package no.entur.schema2proto.generateproto.serializer;

/*-
 * #%L
 * schema2proto-lib
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
import java.util.Map;
import java.util.stream.Collectors;

import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.internal.parser.OptionElement;

import no.entur.schema2proto.generateproto.Schema2ProtoConfiguration;

public class AddConfigurationSpecifiedOptions implements Processor {
	private Schema2ProtoConfiguration configuration;

	public AddConfigurationSpecifiedOptions(Schema2ProtoConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		List<OptionElement> mappedOptions = configuration.options.entrySet()
				.stream()
				.map(option -> new OptionElement(option.getKey(), getKind(option), option.getValue(), false))
				.collect(Collectors.toList());

		packageToProtoFileMap.values().forEach(file -> mappedOptions.forEach(option -> file.options().add(option)));
	}

	private OptionElement.Kind getKind(Object object) {
		if (object instanceof Boolean) {
			return OptionElement.Kind.BOOLEAN;
		}
		if (object instanceof Number) {
			return OptionElement.Kind.NUMBER;
		}
		return OptionElement.Kind.STRING;
	}
}
