package no.entur.schema2proto.generateproto;

/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 - 2021 Entur
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.Joiner;

public class GoPackageNameHelper {

	private GoPackageNameHelper() {

	}

	/**
	 * Convert proto package name to an appropriate golang package name
	 * 
	 * @param packageName proto package name
	 * @return go package name
	 */
	public static String packageNameToGoPackageName(String goPackageSource, String packageName) {
		// Go package names should be all lower caps and do not allow underscore
		String packageNameLegalChars = packageName.replace("_", "").toLowerCase();
		String[] parts = packageNameLegalChars.split("\\.");

		// Use last part of proto package name as go package name
		// Avoid purely numeric package name, concat as many parts as we need to get name with other chars
		List<String> goPackageNameParts = new ArrayList<>();
		for (int i = parts.length - 1; i >= 0; i--) {
			String part = parts[i];
			goPackageNameParts.add(part);
			if (!NumberUtils.isCreatable(part)) {
				break;
			}
		}
		if (goPackageSource != null) {
			goPackageNameParts.add(goPackageSource);
		}
		Collections.reverse(goPackageNameParts);

		return Joiner.on("").join(goPackageNameParts);
	}

}
