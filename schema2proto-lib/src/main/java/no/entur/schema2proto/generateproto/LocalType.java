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
package no.entur.schema2proto.generateproto;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;

public class LocalType {
	// String enclosingName;
	XSComponent xsComponent;
	MessageType localType;
	MessageType enclosingType;
	Field referencingField;
	String targetPackage;
	XSComplexType enclosingComplexType;

	public LocalType(XSComponent xsComponent, MessageType localType, MessageType enclosingType, Field referencingField, String targetPackage,
			XSComplexType enclosingComplexType) {
		// this.enclosingName = enclosingName;
		this.xsComponent = xsComponent;
		this.localType = localType;
		this.enclosingType = enclosingType;
		this.referencingField = referencingField;
		this.targetPackage = targetPackage;
		this.enclosingComplexType = enclosingComplexType;
	}

}
