# Migration: drop vendored `schema2proto-wire` fork → stock `com.squareup.wire:wire-schema-jvm:6.4.0`

Chosen approach (user decision): **full migration to the immutable stock API**. No schema2proto-owned
mutable model classes. Consumers in `schema2proto-lib` are rewritten to use stock immutable wire types,
replacing in-place mutation with `.copy(...)` reconstruction.

## Key facts (verified against sources)
- Stock types `Field`, `MessageType`, `EnumType`, `EnumConstant`, `OneOf`, `ProtoFile`, `Reserved` are
  `data class`es (so `.copy()` works); `Options` is a plain class. `MessageType.extensionFields` is a
  `MutableList` (schema2proto uses `declaredFields`, which is immutable).
- `MessageType`/`EnumType`/`ProtoFile` are public data classes with public constructors → constructible
  directly (no forced element-layer rewrite).
- Tests use a **semantic** comparator (`ProtoComparator`): loads+links expected & generated protos and
  compares the model. Formatting differences do NOT break tests; semantic equivalence + linkability do.

## API gaps fork→stock (authoritative)
- `Field`: fork `(packageName, loc, label, name, doc, tag, [default,] elementType, options, extension[, fromElement])`
  → stock `(namespaces: List<String>, loc, label?, name, doc, tag, default?, elementType, options, isExtension, isOneOf, declaredJsonName?)`.
  packageName ↔ `namespaces[0]`. Fork mutators `updateName/updateTag/updateElementType/updateDocumentation/updatePackageName/clearPackageName/setLabel/setFromAttribute/setFromElement` → `.copy(...)`.
- `MessageType`: stock ctor `(type, loc, doc, name, declaredFields, extensionFields(Mutable), oneOfs, nestedTypes, nestedExtendList, extensionsList, reserveds(private), options, syntax)`.
  Fork `addField/removeDeclaredField/setDeclaredFields/updateName/updateDocumentation/removeOneOf/getNextFieldNum/advanceFieldNum/setWrapperMessageType/getReserveds/addReserved` → reconstruct via copy / schema2proto-side state. `reserveds` is private (read via `toElement()`); copy() can still set it.
- `EnumType`: stock `(type, loc, doc, name, constants, reserveds(private), options, syntax)`. Fork `updateName/addReserved`.
- `EnumConstant`: stock `(loc, name, tag, doc, options)`. Fork `updateName/updateTag`.
- `OneOf`: stock `(name, doc, fields, location, options)` — adds `location`. Fork `addField/updateDocumentation`.
- `Options`: stock `(optionType: ProtoType, optionElements: List<OptionElement>)`. Companion ProtoType consts
  `FILE_/MESSAGE_/FIELD_/ENUM_/ENUM_VALUE_/SERVICE_/METHOD_OPTIONS` and **`ONEOF_OPTIONS`** (fork: `ONE_OF_OPTIONS`).
  Raw elements via property `elements` (fork `getOptionElements()`); no `add()`/`replaceOption()` → rebuild Options.
- `OptionElement`: stock `(name, kind, value, isParenthesized)` public; `Kind{STRING,BOOLEAN,NUMBER,ENUM,MAP,LIST,OPTION}`.
- `ProtoFile`: stock `(loc, imports, publicImports, weakImports, packageName?, types, services, extendList, options, syntax?)`.
  fork `mergeFrom/setLocation` + mutable lists; serialize via `toElement().toSchema()` (fork had `ProtoFile.toSchema()`).
- `SchemaLoader`: stock `SchemaLoader(okio.FileSystem)` or JVM `SchemaLoader(java.nio.FileSystem)`;
  `initRoots(sourcePath: List<Location>, protoPath)` + `loadSchema(): Schema`. fork: no-arg + `addSource/addProto/sources/protos/load`.
- Pruning: fork `IdentifierSet(.Builder).include/exclude/build`, `unusedIncludes/unusedExcludes/includes/excludes`,
  `schema.prune(IdentifierSet)` → stock `PruningRules(.Builder).addRoot/prune/build`, `unusedRoots/unusedPrunes/isRoot/prunes`,
  `schema.prune(PruningRules)`.
- `Schema`: stock `protoFiles: List`, `protoFile(String):ProtoFile?`, `getType(...):Type?`; **no `protoFileForPackage`** → implement helper.
- schema2proto-specific statics that the fork bolted onto wire classes must move into schema2proto:
  `MessageType.XSD_MESSAGE_OPTIONS_PACKAGE`, `MessageType.BASE_TYPE_MESSAGE_OPTION`,
  `MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME`, plus field-numbering, fromAttribute/fromElement, wrapperMessageType flags.

## Plan / stages
0. Remove `schema2proto-wire` module; depend on `wire-schema-jvm:6.4.0` (+ okio). [in progress]
1. schema2proto-side support: `WireFactory` (convenience constructors→stock), relocate custom constants,
   side-state for build-time concerns (field numbering, fromAttribute, wrapper flag) keyed by identity,
   serialization helper `toSchema(ProtoFile)`.
2. Migrate `SchemaParser` (scratch-builder → immutable construct).
3. Migrate `ProtoSerializer` (functional reconstruction across passes; helper to replace a type/field in parents).
4. Migrate `ModifyProto` (SchemaLoader/PruningRules/copy-based edits).
5. Migrate compat checkers (FieldConflictChecker, EnumConflictChecker, ProtolockBackwardsCompatibilityChecker).
6. Migrate `ValidationRuleFactory`, `Schema2Proto`, `LocalType`, and test harness (`ProtoComparator`, etc.).
7. Build + run tests; fix until green.
