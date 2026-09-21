package no.entur.schema2proto.wire;

/*-
 * #%L
 * schema2proto Maven Plugin
 * %%
 * Copyright (C) 2019 - 2026 Entur
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Type;

public class WireSchemaLoaderTest {

	/** Archive source roots were supported by the vendored loader (its {@code locateInZipFile} test) and must keep working. */
	@Test
	public void testLocateInZipFile(@TempDir Path tempDir) throws IOException {
		Path zip = writeZip(tempDir.resolve("protos.zip"), "a/b/message.proto", "message Message {}");

		Schema schema = WireSchemaLoader.load(Collections.singletonList(zip), Collections.singletonList("a/b/message.proto"));

		Type message = schema.getType("Message");
		assertNotNull(message);
		assertEquals(zip.toString(), message.getLocation().getBase());
		assertEquals("a/b/message.proto", message.getLocation().getPath());
	}

	/** Deduplication and first-root-wins precedence must hold when a directory and an archive both provide the same proto. */
	@Test
	public void testDirectoryRootTakesPrecedenceOverLaterArchiveRoot(@TempDir Path tempDir) throws IOException {
		Path dir = Files.createDirectories(tempDir.resolve("dir/a/b"));
		Files.writeString(dir.resolve("message.proto"), "message Message {\n  optional int32 from_directory = 1;\n}\n");
		Path zip = writeZip(tempDir.resolve("protos.zip"), "a/b/message.proto", "message Message {}");

		Schema schema = WireSchemaLoader.load(List.of(tempDir.resolve("dir"), zip), Collections.emptyList());

		assertEquals(tempDir.resolve("dir").toString(), schema.getType("Message").getLocation().getBase());
	}

	@Test
	public void testMissingSourceRootIsRejected(@TempDir Path tempDir) {
		List<Path> sources = Collections.singletonList(tempDir.resolve("nope"));
		assertThrows(IllegalArgumentException.class, () -> WireSchemaLoader.load(sources, Collections.emptyList()));
	}

	@Test
	public void testUnreadableArchiveSourceRootIsRejected(@TempDir Path tempDir) throws IOException {
		Path notAnArchive = tempDir.resolve("garbage.zip");
		Files.writeString(notAnArchive, "this is not a zip file");

		List<Path> sources = Collections.singletonList(notAnArchive);
		assertThrows(IllegalArgumentException.class, () -> WireSchemaLoader.load(sources, Collections.emptyList()));
	}

	private Path writeZip(Path zip, String entryName, String content) throws IOException {
		try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
			zipOut.putNextEntry(new ZipEntry(entryName));
			zipOut.write(content.getBytes(StandardCharsets.UTF_8));
			zipOut.closeEntry();
		}
		return zip;
	}
}
