# RemoteCompose Lottie Guidelines

Use the dev-flow skill as the main workflow pipeline.

<lottie_specifications>
## Canonical Lottie Specifications
Always consult these authoritative references when developing or verifying Lottie format, values, properties, and animations:
- **One-page Lottie 1.0.1 Specification:** https://lottie.github.io/lottie-spec/1.0.1/single-page/
- **Lottie JSON Schema:** https://lottie.github.io/lottie-spec/1.0.1/lottie.schema.json
- **Property Types (Vector, Scalar, Easing, Keyframes):** https://lottie.github.io/lottie-spec/dev/specs/properties/
</lottie_specifications>

<workflow_routers>
## Workflow Routers (Phase Guidelines)
To prevent prompt noise, detailed phase rules are loaded just-in-time from `.agents/rules/` and specialized subagents:
- **Test Generation (Isolated TDD):** Before writing implementation code, MUST invoke the `dev-test` subagent (`.agents/agents/dev-test.md`) with the target specification (`SP_XXX`).
- **Implementation & SDD:** Follow `.agents/rules/sdd_execution.md` for task step execution, atomic commits, and isolation.
- **Commit Messages:** Follow `.agents/rules/commit_rules.md` (inverted pyramid, imperative effects, headings).
- **Pull Requests:** Follow `.agents/rules/pull_request_rules.md` when drafting PR descriptions.
- **Code Review:** Follow `.agents/rules/code_review_rules.md` (answer with code and comments).
- **Design Docs:** Follow `.agents/rules/design_docs_style.md` for concept/spec writing.
- **Code Comments & Function Docs:** Follow `.agents/rules/code_documentation_rules.md` for function contracts, essential vs. incidental properties, and invariant documentation.
</workflow_routers>
