# Project Guidelines

<file_operations>
- **DO NOT** use `cat << 'EOF'` or shell redirection to write or overwrite files.
- **ALWAYS** use native file-writing tools (`write_to_file`, `replace_file_content`), which are auto-approved in this workspace.
</file_operations>

<testing_policy>
## Testing Policy & Test-Driven Development (TDD)

### 1. Test-Driven Approach
- **ALWAYS** follow a Test-Driven Development (TDD) approach.
- **First Step of Every Plan:** The very first step of every plan or task **MUST** be to design and write representative test cases to cover the bug (reproduction) or the new feature requirements before implementing the solution.
- Ensure tests fail or capture the missing functionality before writing the implementation.

### 2. Module-Specific Execution Order
Replace `<module>` with the specific Gradle module path (e.g., `:remotecompose:lottie`, `:sample`, etc.). Execute in this exact sequence:

1. **Format Code:** `./gradlew :<module>:ktfmtFormat`
2. **Update Metalava Signatures:** `./gradlew :<module>:metalavaGenerateSignatureDebug` *(Skip for application/demo modules like `:sample`; required only for library modules)*
3. **Compile Kotlin:** `./gradlew :<module>:compileDebugKotlin`
4. **Assemble Build:** `./gradlew :<module>:assembleDebug`
5. **Run Unit Tests:** `./gradlew :<module>:testDebugUnitTest`
6. **Run All Checks:** `./gradlew :<module>:check`

### 3. Screenshot & Roborazzi Tasks
Try the module-specific command first; if it runs successfully, verify if the global variant works:
- **Local (Module-specific):**
  - Record: `./gradlew :<module>:recordRoborazziDebug`
  - Verify: `./gradlew :<module>:verifyRoborazziDebug`
- **Global:**
  - Record: `./gradlew recordRoborazziDebug`
  - Verify: `./gradlew verifyRoborazziDebug`

### 4. Verification Scope
- **ALWAYS** prioritize running checks on the specific module modified.
- If changes affect downstream modules, run tests for downstream modules as well.
</testing_policy>

<commit_guidelines>
## Commit Message Guidelines

When generating or suggesting a commit message, analyze the git commit history and current diff. Simplify code review and long-term maintenance. Never generate generic messages like "Fix bug" or "Update UI".

### 1. Core Structure & Inverted Pyramid
- **The Inverted Pyramid:** Place the most critical information at the very top so the reader gets full context immediately.
- **Use Headings:** For complex changes requiring longer descriptions, structure using Markdown headings.

### 2. Title Line (Imperative & Effect-Focused)
- **Describe the effect, not the implementation:** Focus on what the change actually does to the application ("what"), not the implementation details ("how" or "why").
  - *Bad:* "Add a mutex to guard the database handle"
  - *Good:* "Prevent database corruption during simultaneous sign-ups"

### 3. Body: Impact & Motivation
- **Impact Summary:** Summarize how the change affects clients and end-users with sufficient detail for non-code readers.
- **Motivation ("Why"):** Explain why the change is necessary, the constraints that guided your decision, and how this change fits into any larger architectural designs or team goals.

### 4. Specific Context (When Applicable)
- **Breaking Changes:** Explicitly flag breaking changes under a `### Breaking Changes` heading with migration steps.
- **Cross-References:** Use auto-closing keywords for related issues (e.g., `Fixes #1234`) or reference PR IDs and commit hashes.
- **New Dependencies:** Explicitly flag and justify third-party dependencies and justify *why* it was added and how it was selected.
- **External References:** Link to relevant non-obvious resources, documentation, or design posts.
- **Rich Context:** Document alternatives considered, bug summaries, test coverage/limitations, or what was learned.

### 5. Antipatterns (Strictly Prohibited)
**NEVER** include the following in commit descriptions:
- Information that is obvious from reading the code.
- Code maintenance instructions (place these in code comments instead).
- Short-term discussions.
- Preview URLs and build artifacts.
- Comments or tags like "build with AI".

### 6. Scope and Isolation
- **DO NOT** mix functional and non-functional changes in the same commit. If you are asked to add a feature, do not simultaneously reformat surrounding code, reorganize imports, or refactor unrelated methods in the same commit.
- Keep commits atomic (narrowly scoped): Split into **Commit A (Refactor/Cleanup)** and **Commit B (Feature Logic)** when preparing code for a new feature.
</commit_guidelines>

<pull_request_guidelines>
## Pull Request Guidelines

When drafting Pull Requests (distinct from commit messages), you **MUST** follow this template:

#### WHAT
(Briefly explain what this PR does)

#### WHY
(Explain the motivation and context)

#### HOW
(Explain the technical approach and justify complex or non-obvious design choices)

#### Checklist :clipboard:
- [ ] **Diff Audit:** I have reviewed my own diff and ensured no debugging code, unused imports, or unrelated changes accidentally slipped in.
- [ ] Add explicit visibility modifier and explicit return types for public declarations
- [ ] Run spotless check
- [ ] Run tests
- [ ] Update metalava's signature text files
</pull_request_guidelines>

<code_review_handling>
## Handling Code Review & Feedback

When receiving user feedback, change requests, or clarification questions:

1. **Answer with code, not just chat:** If the user expresses confusion or asks "Why did you do this?", the code is not self-documenting. **Refactor the code for clarity or add explicit explanatory code comments directly in the codebase**, rather than only replying in chat.
2. **Explicitly communicate resolutions:** Clearly specify what changed (e.g., "Extracted parsing logic into `UserParser.kt` and added unit tests") instead of generic confirmations like "Fixed".
</code_review_handling>

<design_docs_style>
## Design Document & Writing Style Guidelines

1. **Accessibility:** Easy to read. Assume the reader does not know internal terminology and is not fluent in English. Avoid unexplained acronyms.
2. **Tone:** Dry, practical language. No figurative expressions, idioms, or anthropomorphism (components do not "look", "see", or "watch").
3. **Structure:** Important information first. Follow the sequence: *what* → *how* → *details*.
4. **Conciseness:** Fewer words win. Cut whole concepts when possible, not just words.
5. **Ubiquitous Language:** Exactly one term per concept, used consistently. Maintain a glossary in the document.
6. **Alternatives Considered:** Rejected options **ALWAYS** belong in an "Alternatives considered" section at the end, detailing pros, cons, and rationale.
7. **Quantifiers:** Use universal quantifiers (*all, every, never, always*) only when literally true by design; otherwise specify exact sets.
8. **Referents:** Avoid ambiguous pronouns (*it, them*); repeat the explicit noun.
9. **Precision:** Avoid subjective words (*small, big, fast, slow, cheap, expensive*); provide concrete numbers, sizes, and frequencies.
10. **Ownership:** Be explicit about ownership and audience: explicitly state where values are stored, access permissions, and target audiences.

*Feedback Rule:* Treat user comments as alternatives to evaluate (pros/cons), adopt the selected approach in the main text, and document rejected alternatives under "Alternatives considered".
</design_docs_style>

<spec_driven_execution>
## Spec-Driven Execution Protocol (dev-flow integration)

When using the `dev-flow` Spec-Driven Development (SDD) process, enforce strict discipline during the **Implementation** phase:

### 1. Step-by-Step Implementation & TDD
- **NEVER execute the entire spec in one pass.**
- **TDD First:** The very first step of every plan **MUST** design and implement representative test cases covering the bug reproduction or new feature acceptance criteria before writing production code.
- Read the spec task list, select **Task 1**, and write *only* the code required for that task.
- Stop, verify the code (run tests/formatters), and request user approval before proceeding to Task 2.

### 2. Isolate Changes (Functional vs. Non-Functional)
- If a task requires refactoring existing code to make room for a new feature, **split it into two commits**:
  - **Commit A:** Refactoring / cleanup (non-functional).
  - **Commit B:** New feature logic (functional).
  - Do not mix styling/reformatting of untouched files with feature changes.

### 3. Atomic Commits via Terminal
- Once a task is complete and verified, immediately run `git add` and `git commit` via terminal.
- Use the overarching Spec/PRD as context for the commit message (following the commit guidelines above).
- **Rule of Thumb:** A single commit should address only one task from the spec.
</spec_driven_execution>
