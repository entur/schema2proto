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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.UNDERSCORE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CaseFormat;
import com.squareup.wire.schema.*;
import com.squareup.wire.schema.internal.parser.OptionElement;

public class UpdateEnumValues implements Processor {

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().forEach(file -> {
			updateEnumValues(file.types());
		});

	}

	private void updateEnumValues(List<Type> types) {
		for (Type t : types) {
			if (t instanceof EnumType) {
				EnumType e = (EnumType) t;
				updateEnum(e);
			}
			updateEnumValues(t.nestedTypes());
		}
	}

	private void updateEnum(EnumType e) {
		// add UNSPECIFIED value first
		List<OptionElement> optionElementsUnspecified = new ArrayList<>();

		// Prefix with enum type name
		String enumValuePrefix = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, e.name()) + UNDERSCORE;
		for (EnumConstant ec : e.constants()) {
			String enumValue = escapeEnumValue(ec.getName());
			if (enumValue.equalsIgnoreCase("UNSPECIFIED")) {
				enumValue += "EnumValue";
			}
			String uppercaseEnumValue = enumValue;
			if (!StringUtils.isAllUpperCase(enumValue)) {
				uppercaseEnumValue = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, enumValue);
			}
			ec.updateName(enumValuePrefix + uppercaseEnumValue);
		}
		EnumConstant unspecified = new EnumConstant(new Location("", "", 0, 0), enumValuePrefix + "UNSPECIFIED", 0, "Default",
				new Options(Options.ENUM_VALUE_OPTIONS, optionElementsUnspecified));
		e.constants().add(0, unspecified);
	}

	private String escapeEnumValue(String name) {
		switch (name) {
		case "+":
			return "plus";
		case "-":
			return "minus";
		default:
			return name.replaceAll("-", "");
		}

	}
}
