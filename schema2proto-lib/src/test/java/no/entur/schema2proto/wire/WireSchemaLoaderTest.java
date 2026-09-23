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

import java.io.FileNotFoundException;
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

import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Type;

/**
 * Covers the loader contract inherited from the vendored {@code SchemaLoaderTest}, which was deleted along with the fork: load everything when no protos are
 * named, resolve named protos across several roots, fail when a named proto is missing, support archive roots, and let earlier roots win.
 */
public class WireSchemaLoaderTest {

	@Test
	public void testLoadAllFilesWhenNoneSpecified(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("message1.proto"), "message Message1 {}");
		Files.writeString(tempDir.resolve("message2.proto"), "message Message2 {}");
		Files.writeString(tempDir.resolve("readme.txt"), "Here be protos!");

		Schema schema = WireSchemaLoader.load(Collections.singletonList(tempDir), Collections.emptyList());

		assertEquals(tempDir.toString(), schema.getType("Message1").getLocation().getBase());
		assertEquals("message1.proto", schema.getType("Message1").getLocation().getPath());
		assertEquals(tempDir.toString(), schema.getType("Message2").getLocation().getBase());
		assertEquals("message2.proto", schema.getType("Message2").getLocation().getPath());
	}

	@Test
	public void testLocateInMultiplePaths(@TempDir Path tempDir) throws IOException {
		Path source1 = Files.createDirectories(tempDir.resolve("source1"));
		Path source2 = Files.createDirectories(tempDir.resolve("source2"));
		Files.writeString(source1.resolve("file1.proto"), "message Message1 {}");
		Files.writeString(source2.resolve("file2.proto"), "message Message2 {}");

		Schema schema = WireSchemaLoader.load(List.of(source1, source2), List.of("file1.proto", "file2.proto"));

		assertNotNull(schema.getType("Message1"));
		assertNotNull(schema.getType("Message2"));
	}

	@Test
	public void testFailLocate(@TempDir Path tempDir) throws IOException {
		Path source1 = Files.createDirectories(tempDir.resolve("source1"));
		Path source2 = Files.createDirectories(tempDir.resolve("source2"));
		Files.writeString(source2.resolve("file2.proto"), "message Message2 {}");

		List<Path> sources = Collections.singletonList(source1);
		assertThrows(FileNotFoundException.class, () -> WireSchemaLoader.load(sources, Collections.singletonList("file2.proto")));
	}

	@Test
	public void testFailLocateInZipFile(@TempDir Path tempDir) throws IOException {
		Path zip = writeZip(tempDir.resolve("protos.zip"), "a/b/trix.proto", "message Trix {}");

		List<Path> sources = Collections.singletonList(zip);
		assertThrows(FileNotFoundException.class, () -> WireSchemaLoader.load(sources, Collections.singletonList("a/b/message.proto")));
	}

	@Test
	public void testEarlierSourcesTakePrecedenceOverLaterSources(@TempDir Path tempDir) throws IOException {
		Path source1 = Files.createDirectories(tempDir.resolve("source1"));
		Path source2 = Files.createDirectories(tempDir.resolve("source2"));
		Files.writeString(source1.resolve("message.proto"), "message Message {\n  optional string a = 1;\n}\n");
		Files.writeString(source2.resolve("message.proto"), "message Message {\n  optional string b = 2;\n}\n");

		Schema schema = WireSchemaLoader.load(List.of(source1, source2), Collections.emptyList());

		assertNotNull(((MessageType) schema.getType("Message")).field("a"));
	}

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

	/**
	 * The vendored loader resolved named protos with {@code Path.resolve}, so a Windows caller could name one {@code a\b.proto}. Such a path must still resolve
	 * to the slash-separated entry the index holds, and be loaded under that path.
	 */
	@Test
	public void testNamedProtoWithBackslashSeparators(@TempDir Path tempDir) throws IOException {
		Files.createDirectories(tempDir.resolve("a/b"));
		Files.writeString(tempDir.resolve("a/b/message.proto"), "message Message {}");

		Schema schema = WireSchemaLoader.load(Collections.singletonList(tempDir), Collections.singletonList("a\\b\\message.proto"));

		assertEquals("a/b/message.proto", schema.getType("Message").getLocation().getPath());
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

	/** Files.walk yields directories too, so a directory whose name ends in .proto must not be handed to wire as a schema source. */
	@Test
	public void testDirectoryNamedLikeAProtoIsIgnored(@TempDir Path tempDir) throws IOException {
		Files.createDirectories(tempDir.resolve("looks_like_a_file.proto"));
		Files.writeString(tempDir.resolve("message.proto"), "message Message {}");

		Schema schema = WireSchemaLoader.load(Collections.singletonList(tempDir), Collections.emptyList());

		assertNotNull(schema.getType("Message"));
	}

	/** The vendored loader applied the .proto suffix filter only when discovering the default set; a named file resolved by its exact path. */
	@Test
	public void testExplicitlyNamedNonProtoFileIsLoaded(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("message.protodevel"), "message Message {}");

		Schema schema = WireSchemaLoader.load(Collections.singletonList(tempDir), Collections.singletonList("message.protodevel"));

		assertNotNull(schema.getType("Message"));
	}

	/** A transitive import resolves by its exact path too, so an import of a non-.proto file must not be reported as missing. */
	@Test
	public void testNonProtoImportIsResolved(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("extra.protodevel"), "message Extra {}");
		Files.writeString(tempDir.resolve("main.proto"), "import \"extra.protodevel\";\n\nmessage Main {\n  optional Extra extra = 1;\n}\n");

		Schema schema = WireSchemaLoader.load(Collections.singletonList(tempDir), Collections.emptyList());

		assertNotNull(schema.getType("Main"));
		assertNotNull(schema.getType("Extra"));
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
