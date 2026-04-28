# Meta 05 — Build a custom MCP server for this codebase

**Type:** meta-skill (the deliverable is a working MCP server + Claude Code config)
**Estimated time:** 90–120 min with Claude Code

## What's missing

Claude Code's default toolbelt is generic (Read, Edit, Bash, etc.). To make Claude *fast on a specific codebase*, you extend the toolbelt with **custom MCP servers** — small processes that expose project-specific tools via JSON-RPC over stdio.

This branch ships a stub MCP server (`.claude/mcp-servers/codebase_tools.py`) with two TODO functions. Your job:

1. Install the `mcp` Python SDK
2. Implement `list_endpoints()` (walk the controllers, return all REST endpoints)
3. Implement `summarize_test_failures()` (run `mvn test`, return a structured summary)
4. Wire them into the SDK's server using `@server.list_tools` / `@server.call_tool`
5. Create `.mcp.json` at the repo root telling Claude Code how to launch the server (use `.mcp.json.example` as a template)

After this, in any future Claude session, you can ask:

> "Use the codebase-tools MCP server to list every endpoint that handles a user-related URL. Then run summarize_test_failures and tell me which test classes are unstable."

…and Claude calls your tools directly. No grep, no `mvn` shell-out — the toolbelt has grown.

## Reproduce

```bash
git checkout meta/05-mcp-servers
ls .mcp.json                          # → no such file (visible test fails)
cat .claude/mcp-servers/codebase_tools.py  # see the stub + TODOs
mvn -B -Dtest=McpServerArtifactsTest test  # → 3 failures
```

## What "done" looks like

- `pip install mcp` succeeded
- `.mcp.json` exists at the repo root, registers `codebase-tools`, points at the Python script
- The Python file imports the SDK (uncommented) and registers the two tools
- `list_endpoints()` actually walks the codebase (not the `NotImplementedError` stub)
- `summarize_test_failures()` actually runs `mvn` and parses output
- All 5 visible artifact tests pass
- Hidden grading suite passes — it invokes both functions via subprocess and asserts they return sensibly

## Why this is the highest-ROI extensibility lever

Most teams use Claude Code with the default toolbelt forever. Power users build 1–3 MCP servers per project: a database introspector, a domain query tool, a deploy/observe wrapper. The cost is one afternoon per server. The dividend is that **every prompt afterward is shorter and more reliable**, because the operations Claude needs are now first-class tools instead of derived from grep + bash.

In real use the most common MCP servers are:

| Pattern | What it gives Claude |
| --- | --- |
| **Database introspection** (Postgres/MySQL/SQLite/H2) | direct read access for "what's actually in the DB right now" |
| **Codebase tools** (this exercise's pattern) | `list_endpoints`, `find_bean`, `describe_jpa_mapping` — domain-aware navigation |
| **Build & test** (also this exercise) | structured test/build results without parsing log output |
| **External APIs** (Linear, Jira, Slack, GitHub) | move tickets, post messages, comment on PRs |
| **Domain-specific wrappers** (your billing API, your feature flags) | the things `bash + curl` makes Claude reinvent every session |

## The MCP protocol in 60 seconds

- An MCP server is a process that speaks **JSON-RPC over stdio**
- Claude Code launches it with the command in `.mcp.json` and pipes stdin/stdout
- The server registers tools (`tools/list`) and answers calls (`tools/call`)
- The `mcp` Python SDK handles the protocol — you just write functions

## See also

- [`CLAUDE_INSTRUCTIONS.md`](CLAUDE_INSTRUCTIONS.md) — recommended workflow
- [Anthropic MCP docs](https://modelcontextprotocol.io/)
- [`mcp` Python SDK](https://github.com/modelcontextprotocol/python-sdk)
