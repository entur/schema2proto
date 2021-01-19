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

public class GoPackageNameHelper {

	private GoPackageNameHelper() {

	}

	/**
	 * Convert proto package name to an appropriate golang package name
	 * 
	 * @param packageName proto package name
	 * @return go package name
	 */
	public static String packageNameToGoPackageName(String goPackageSourcePrefix, String packageName) {

		StringBuilder sb = new StringBuilder();
		if (goPackageSourcePrefix != null) {
			sb.append(goPackageSourcePrefix);
			if (!goPackageSourcePrefix.endsWith("/")) {
				sb.append("/");
			}
		}
		sb.append(packageName.replace(".", "/"));
		return sb.toString();
	}

}
