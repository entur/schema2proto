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
package no.entur.schema2proto.wire;

import java.util.ArrayList;
import java.util.List;

import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.internal.parser.OptionElement;

/**
 * Mutable builder analogue of {@link com.squareup.wire.schema.Options}. schema2proto manipulates option elements in place during conversion; the stock wire
 * {@code Options} is immutable, so we accumulate elements here and emit an immutable {@code Options} via {@link #toWire()} at serialization time.
 */
public class MutableOptions {

	// Re-export the option-type constants so consumers keep referencing them through Options.* (see stock companion @JvmField).
	public static final ProtoType FILE_OPTIONS = Options.FILE_OPTIONS;
	public static final ProtoType MESSAGE_OPTIONS = Options.MESSAGE_OPTIONS;
	public static final ProtoType FIELD_OPTIONS = Options.FIELD_OPTIONS;
	public static final ProtoType ONEOF_OPTIONS = Options.ONEOF_OPTIONS;
	public static final ProtoType ENUM_OPTIONS = Options.ENUM_OPTIONS;
	public static final ProtoType ENUM_VALUE_OPTIONS = Options.ENUM_VALUE_OPTIONS;
	public static final ProtoType SERVICE_OPTIONS = Options.SERVICE_OPTIONS;
	public static final ProtoType METHOD_OPTIONS = Options.METHOD_OPTIONS;

	private final ProtoType optionType;
	private final List<OptionElement> optionElements;

	public MutableOptions(ProtoType optionType, List<OptionElement> elements) {
		this.optionType = optionType;
		this.optionElements = new ArrayList<>(elements);
	}

	public ProtoType optionType() {
		return optionType;
	}

	public List<OptionElement> getOptionElements() {
		return optionElements;
	}

	public void add(OptionElement option) {
		optionElements.add(option);
	}

	public void replaceOption(String optionName, OptionElement element) {
		optionElements.removeIf(e -> e.getName().equals(optionName));
		optionElements.add(element);
	}

	public Options toWire() {
		return new Options(optionType, new ArrayList<>(optionElements));
	}
}
