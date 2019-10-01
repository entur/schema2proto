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

import java.util.Map;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;

public class ComputeFilenames implements Processor {

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Map.Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			ProtoFile file = protoFile.getValue();
			String filename = protoFile.getKey().replaceAll("\\.", UNDERSCORE) + ".proto";
			file.setLocation(new Location("", filename, 0, 0));
		}
	}
}
