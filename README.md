# Visitor Pattern - Advanced File System

Sulukhan Aizharykova, SE-2529. Assignment 06, Software Design Patterns.

An in-memory file tree with independent size, search and statistics operations. Java source and tests contain no comments. No production dependencies are needed; JUnit 5 is test-scoped.

Repository: https://github.com/sulumate/spd6_sulumate

## Build, test and run

Requirements: JDK 17 or newer and Maven 3.9+. Maven must be on `PATH`; first execution needs access to Maven Central.

Run from the directory containing `pom.xml`:

```sh
mvn test
mvn package
java -jar target/visitor-filesystem-1.0.0.jar
java -jar target/visitor-filesystem-1.0.0.jar .md 128
```

On a Mac with IntelliJ IDEA and no standalone Maven, its bundled executable can be used:

```sh
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" test
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" package
```

Alternatively, import `pom.xml` in IntelliJ, choose JDK 17 and run the Maven `test` goal. The command-line build does not depend on IntelliJ. Compilation targets Java 17 and treats compiler warnings as errors.

The optional arguments are a case-sensitive filename suffix and an inclusive minimum byte size. Defaults are `.txt` and `256`. Java callers can supply any deterministic `Predicate<String>` to `SearchVisitor`, for example `name -> name.startsWith("invoice")`.

## Deterministic demonstration

The tree in `SampleFileSystem.create()` has these files:

| Root-relative file path | Bytes |
| --- | ---: |
| assets/logo.png | 8192 |
| docs/guide.txt | 1024 |
| docs/drafts/notes.txt | 256 |
| docs/drafts/todo.md | 128 |
| Main.java | 2048 |
| zero.txt | 0 |
| README.md | 512 |

Directories: `workspace`, `assets`, `assets/empty`, `docs`, `docs/drafts`. `assets/empty` has no children. This is a simulated tree; it does not read or modify the computer's real files.

Default output:

```text
Root: workspace
Total size: 12160 bytes
Search: suffix=.txt, minimum=256 bytes
  docs/drafts/notes.txt
  docs/guide.txt
Files: 7
Directories: 5
Maximum depth: 3
Repeated search unchanged: true
```

With `.md 128`, the matches become `README.md` and `docs/drafts/todo.md`. Size and statistics remain unchanged.

## Design and double dispatch

- `Element` is the shared interface. Abstract sealed `Node` centralizes name validation and parent ownership. `FileNode` and `DirectoryNode` are the only concrete element types.
- `Visitor<R>` declares `visit(FileNode)` and `visit(DirectoryNode)` together with the execution lifecycle. Concrete operations extend `AbstractVisitor<R>`.
- `TreeWalker` is the sole traversal owner. Elements only dispatch; visitors only process the current node. Neither recurses into children.
- First dispatch: `element.accept(visitor)` selects the concrete element's overridden `accept` at runtime. Inside `FileNode.accept`, `this` has compile-time type `FileNode`, selecting the `visit(FileNode)` overload. Second dispatch selects that overload's implementation on the actual visitor at runtime. Overload selection itself is compile-time, not a runtime type test.
- There are no `instanceof`, reflective inspections, type casts or type-selection chains. The walker accesses children polymorphically through `Element.children()`; files return an empty list.

`Node` is an implementation base for the two elements, not a third concrete element type. `DirectoryNode.add(Node)` deliberately accepts the sealed owned-node hierarchy so parent updates can remain package-private.

## Traversal, paths, depth and lifecycle

`TreeWalker.walk(root, visitor)` performs iterative pre-order depth-first traversal using an explicit stack. It resets the visitor, sets the current location, invokes `accept` once, and pushes each child once. Children are pushed in reverse insertion order so processing preserves insertion order. The integrity rules guarantee a tree, so no reachable node has two incoming ownership edges.

The selected root starts at depth 0 and internal relative path `""`. A child has parent depth + 1 and path `parentPath + "/" + childName`; the leading separator is omitted at the root. A selected directory's own name and ancestors are excluded. When a single file is the selected root, its result path is its filename. Search results use `/` and Java's natural lexicographic string order, independently of insertion order. Maximum depth includes files and empty directories.

Reuse a visitor by passing it to `walk` again. Every run resets all accumulated state, including after a previous failed run. Search returns an immutable list snapshot; statistics returns an immutable record; earlier results remain valid after another run. A visitor instance must not be shared by concurrent or nested walks. Do not mutate the tree during a walk. Direct `accept` is useful for dispatch testing; execute complete operations through `walk`, which owns reset and location setup. If an operation throws, do not use its partial `result()`; the next `walk` starts cleanly.

## Tree integrity and errors

- File sizes are immutable, non-negative `long` values. Zero-byte files are allowed. Size accumulation uses `Math.addExact`, rejecting overflow instead of returning a wrapped value.
- Names are immutable, non-blank, case-sensitive single path components. `.`, `..`, separators, control characters and surrounding whitespace are rejected. Unicode and interior spaces are allowed.
- Directories reject duplicate sibling names across files and directories. Different directories may contain the same filename.
- A node has at most one parent. Adding an already-owned node throws `IllegalStateException`. `remove(name)` detaches it before a later `add` can move it.
- Self-containment and adding an ancestor are rejected before mutation by checking parent identity. Invalid names, sizes, duplicates and cycles throw `IllegalArgumentException`; null arguments throw `NullPointerException`; removing a missing child throws `NoSuchElementException`.
- `children()` returns an immutable snapshot. External callers cannot modify the directory collection or parent links. A rejected insertion leaves both parent and candidate unchanged.

## Extension boundary

Commit `bd66764` establishes both element `accept` methods, both `visit` overloads, traversal and the first two operations. Commit `89f0ff1` adds only `Statistics.java` and `StatisticsVisitor.java`. It changes neither elements, walker nor visitor contracts. Inspect with:

```sh
git show --stat 89f0ff1
```

A new operation adds a visitor implementing both overloads. A new element type is more expensive: the sealed hierarchy, visitor interface, every concrete visitor and tests must be updated; the new element must implement its matching `accept`. This is a deliberate trade-off when element types are stable but operations grow.

Traversal takes O(n) node visits. Search additionally takes O(m log m) for sorting m matches. This implementation stores complete paths per stack entry, so path construction and memory also depend on path lengths; it does not claim constant memory. The explicit stack avoids Java call-stack overflow for deep trees.

## Automated verification

`mvn test` runs 41 JUnit 5 invocations, including nine parameterized invalid-name cases, in four test classes. Results: 41 passed, 0 failures, 0 errors, 0 skipped. The test fixtures are independent of the demonstration dataset. Local verification used Maven 3.9.9 on JDK 25 with release 17; the GitHub workflow also ran the tests and both demos successfully on actual Temurin 17.

| Check | Evidence |
| --- | --- |
| T1: matching overloads | `DispatchTest.acceptSelectsEachMatchingOverloadWithoutTraversingChildren` |
| T2: nested sizes | `VisitorOperationsTest.totalSizeIncludesAllNestedFilesExactlyOnce` |
| T3: both search conditions, inclusive boundary | `searchCombinesFilenameAndInclusiveMinimumSize`, `searchCriterionCanBeReconfiguredBeyondSuffixMatching` |
| T4: deterministic root-relative paths | `pathsAreSortedRegardlessOfInsertionOrder`, `pathsAreRelativeToChosenRootEvenWhenItHasAParent` |
| T5: counts and depth | `statisticsIncludeDirectoryRootAndAllElementDepths`, `deepestEmptyDirectoryAlsoDeterminesMaximumDepth` |
| T6: empty and no-match cases | `emptyDirectoryHasZeroBytesNoMatchesAndDepthZero`, `nonemptyTreeCanProduceNoSearchMatches` |
| T7: repeat runs and snapshots | `reusedVisitorsResetAcrossRunsAndReturnIndependentSnapshots`, `failedRunDoesNotContaminateNextExecution` |
| T8: invalid mutations and valid nesting | `TreeIntegrityTest`; `deepNestingUsesAnExplicitStack` tests 2000 nested directories |
| Executable output and changed inputs | `ApplicationTest` |

## Mandatory criteria map

| Requirement | Implementation |
| --- | --- |
| R1 | `Element`, `FileNode`, `DirectoryNode`, nested `Node` ownership |
| R2 | Both concrete `accept` methods and overloaded `Visitor.visit` |
| R3 | `TotalSizeVisitor` |
| R4 | `SearchVisitor(Predicate<String>, long)` |
| R5 | `StatisticsVisitor`, `Statistics` |
| R6 | `Node.validName`, `FileNode` constructor, `DirectoryNode.add/remove/rejectCycle` |
| R7 | `TreeWalker` exclusively owns traversal |
| R8 | Separate statistics extension commit `89f0ff1` |

## Deliverables

- [PDF report](docs/Visitor_Report_Sulukhan_Aizharykova.pdf): 3 report pages plus a separate UML page.
- [UML PNG](docs/uml-class-diagram.png), [SVG](docs/uml-class-diagram.svg), [editable PlantUML](docs/uml-class-diagram.puml).
- [Defense guide in Russian](docs/DEFENSE_RU.md).
- [Captured verification summary](docs/verification.txt).

Moodle submission: the PDF report and the public GitHub repository URL. The assignment allocates 50 points to an individual defense; a repository alone cannot establish those points.

## Sources and assistance

- Course handout: Assignment 06 - Visitor Pattern, Advanced File System.
- [Java Language Specification 17, method invocation](https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.12).
- [Maven build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).
- [JUnit 5.11.4 user guide](https://docs.junit.org/5.11.4/user-guide/).
- ChatGPT/Codex assisted with implementation, tests, documentation and UML. The student must review, understand and defend the work; this disclosure does not replace the course's independent-work requirement.
