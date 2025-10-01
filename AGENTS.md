# Guildelines for AI Assistants

## Code Style

- Ensure new code follows existing code style and design patterns.

### Kotlin

- Follow Kotlin coding conventions
- Use the provided `.editorconfig` for consistent formatting
- Use Kotlin typesafe DSL builders where possible and prioritize fluent builders style over standard builder methods.
  If DSL builders produce less readable code, use standard setter methods.
- Prefer DSL builder style (method with lambda blocks) over constructors, if possible.
- Use Kotlin's `val` for immutable properties and `var` for mutable properties
- Use fully qualified imports instead of star imports
- Ensure to preserve backward compatibility when making changes

## Testing instructions

- Write comprehensive tests for new features
- Update existing tests when refactoring
- **Prioritize test readability**
- Write tests in Kotlin for JUnit5 test runner
- Use function `Names with backticks` for test methods in Kotlin, e.g. "fun `should return 200 OK`()"
- Avoid writing KDocs for tests, keep code self-documenting
- Write Kotlin tests with [kotlin-test](https://github.com/JetBrains/kotlin/tree/master/libraries/kotlin.test),
  [mockk](https://mockk.io/) and [Kotest-assertions](https://kotest.io/docs/assertions/assertions.html)
  with infix form assertions `shouldBe` instead of `assertEquals`.
- Use Kotest's `withClue("<failure reason>")` to describe failure reasons, but only when the assertion is NOT obvious.
  Remove obvious cases for simplicity.
- If multiple assertions are maid against nullable field, first check for null, e.g.: `params shoulNotBeNull { params.id shouldBe 1 }`
- Use `assertSoftly(subject) { ... }` to perform multiple assertions. Never use `assertSoftly { }` to verify properties
  of different subjects, or when there is only one assertion per subject. Avoid using `assertSoftly(this) { ... }`

### Documentation

- Update README files when adding new features
- Document API changes in the appropriate module's documentation
- Make sure that in production code interfaces and abstract classes are properly documented. Avoid adding KDocs to
  override functions to avoid verbosity.
- Update KDocs when api is changed.
- When referring classes in KDoc, use references: `[SendMessageRequest]` instead of `SendMessageRequest`.
- Add brief code examples to KDoc
- Add links to specifications, if known. Double-check that the link actual and pointing exactly to the specification.
  Never add broken or not accurate links.


## Dev environment tips

- Use `git mv` command when renaming files to preserve git history, if possible
- Never commit or push changes to git automatically. It should be done manually.




