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
package com.squareup.wire.schema

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

/**
 * Builds a repository of `.proto` and `.wire` files to create schemas, profiles, and adapters for
 * testing.
 */
class RepoBuilder {
    private val fs = Jimfs.newFileSystem(Configuration.unix())
    private val root = fs.getPath("/source")
    private val schemaLoader = SchemaLoader().addSource(root)

    fun add(name: String, protoFile: String): RepoBuilder {
        if (name.endsWith(".proto")) {
            schemaLoader.addProto(name)
        } else if (!name.endsWith(".wire")) {
            throw IllegalArgumentException("unexpected file extension: $name")
        }

        val relativePath = fs.getPath(name)
        try {
            val resolvedPath = root.resolve(relativePath)
            val parent = resolvedPath.parent
            if (parent != null) {
                Files.createDirectories(parent)
            }
            Files.write(resolvedPath, protoFile.toByteArray(UTF_8))
        } catch (e: IOException) {
            throw AssertionError(e)
        }

        return this
    }

/*  @Throws(IOException::class)
  fun add(path: String): RepoBuilder {
    val file = File("../wire-tests/src/test/proto/$path")
    file.source().use { source ->
      val protoFile = source.buffer().readUtf8()
      return add(path, protoFile)
    }
  }*/

    fun schema(): Schema {
        try {
            return schemaLoader.load()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }


}