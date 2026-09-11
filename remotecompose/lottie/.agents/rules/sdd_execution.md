---
trigger: model_decision
description: "Spec-Driven Development execution protocol, subagent test generation, step-by-step TDD, and atomic commits."
---

# Spec-Driven Execution Protocol (dev-flow integration)

When implementing tasks from a Spec (`SP_XXX`) and Plan (`PL_XXX`), enforce strict discipline during the **Implementation** phase:

## 1. Isolated TDD First (Subagent Gate)
- **NEVER execute the entire spec in one pass.**
- **NEVER write production code before tests exist.**
- **Subagent Delegation:** Before writing production code for a task, invoke the `dev-test` subagent (`invoke_subagent`).
  - Provide *only* the task specification (`SP_XXX`) and public interface contracts.
  - The subagent operates in a clean context window with no knowledge of pending implementation details, ensuring black-box testing.
  - The subagent defines equivalence classes, boundary values, pairwise matrix, and outputs the test suite.
  - Confirm the test suite fails (Red Phase of TDD) before writing production code.

## 2. Step-by-Step Task Execution
- Read the spec task list, select **Task 1**, and write *only* the minimal code required for that task to turn the tests green.
- Stop, verify the code (run `./gradlew :remotecompose:lottie:check`), and request user approval before proceeding to Task 2.

## 3. Isolate Changes (Functional vs. Non-Functional)
- If a task requires refactoring existing code to make room for a new feature, **split it into two commits**:
  - **Commit A:** Refactoring / cleanup (non-functional).
  - **Commit B:** New feature logic (functional).
  - Do not mix styling/reformatting of untouched files with feature changes.

## 4. Atomic Commits via Terminal
- Once a task is complete and verified, immediately run `git add` and `git commit` via terminal.
- Use the overarching Spec/PRD as context for the commit message (following `.agents/rules/commit_rules.md`).
- **Rule of Thumb:** A single commit should address only one task from the spec.
