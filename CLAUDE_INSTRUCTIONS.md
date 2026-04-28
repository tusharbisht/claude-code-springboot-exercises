# Solving Meta 05 with Claude Code — faster and better

This exercise is about extending Claude Code's toolbelt. The cleverness is twofold: (1) implementing the tools, (2) **using the tools you build** in the same session, so the ROI is immediate.

## Recommended workflow

### 1. Install the SDK first, before any code

```bash
pip install mcp
python3 -c "from mcp.server import Server; print('ok')"
```

If you can't import, no MCP server will run. Get this working before writing a line of tool code.

### 2. Implement `list_endpoints()` first — and dogfood it

> "Read `.claude/mcp-servers/codebase_tools.py`. Implement `list_endpoints()` per its docstring. Walk `src/main/java`, find every `*Controller.java`, parse class-level `@RequestMapping` and method-level `@GetMapping` / `@PostMapping` / etc. Return a list of dicts."

After it's implemented, validate it from the shell:

```bash
python3 -c "import sys; sys.path.insert(0, '.claude/mcp-servers'); \
  from codebase_tools import list_endpoints; \
  import json; print(json.dumps(list_endpoints(), indent=2))"
```

You should see ~12 entries (the existing endpoints in the codebase). If you see fewer, your parser is missing cases.

### 3. Implement `summarize_test_failures()` similarly

> "Implement `summarize_test_failures()`. Run `mvn -B test` via subprocess, parse the `Tests run: ... Failures: ...` summary lines and the `[ERROR] ClassName.method` failure lines. Return the structured dict."

Test it via the same one-shot harness pattern. This tool will be slow (mvn takes seconds) — the value is the structure, not the speed.

### 4. Wire the tools into the SDK

The SDK pattern is in the file's comments. Roughly:

```python
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

server = Server("codebase-tools")

@server.list_tools()
async def _list():
    return [
        Tool(name="list_endpoints", description="...", inputSchema={"type": "object", "properties": {}}),
        Tool(name="summarize_test_failures", description="...", inputSchema={"type": "object", "properties": {}}),
    ]

@server.call_tool()
async def _call(name, arguments):
    if name == "list_endpoints":
        return [TextContent(type="text", text=json.dumps(list_endpoints()))]
    if name == "summarize_test_failures":
        return [TextContent(type="text", text=json.dumps(summarize_test_failures()))]
    raise ValueError(f"unknown tool: {name}")
```

### 5. Configure `.mcp.json`

Copy `.mcp.json.example` → `.mcp.json` and adjust the path if needed.

```bash
cp .mcp.json.example .mcp.json
```

Restart Claude Code (it loads MCP servers on startup). You should see codebase-tools listed when Claude initializes.

### 6. Use it for real

This is the payoff. In your next prompt:

> "Use the codebase-tools MCP server to list every endpoint that touches `/api/users`. Then call summarize_test_failures and tell me whether the `User`-related tests are stable."

Claude calls your tools directly. No bash. No grep. Pure tool calls.

### 7. Verify

```bash
mvn -B -Dtest=McpServerArtifactsTest test    # all 5 visible green
./grading/run-grading.sh                     # hidden runtime checks
```

## The deeper lesson

Once you've built one MCP server, the marginal cost of the next one is **5–10 minutes**. You'll start seeing them everywhere:

- "We always run `kubectl get pods -n staging`" → MCP tool
- "I keep asking Claude to summarize Linear ticket XYZ" → MCP server connecting to Linear's API
- "I want Claude to read our Grafana dashboards directly" → MCP server wrapping Grafana's HTTP API

Power users have a personal `~/mcp-servers/` directory with 5–10 of these. They start every project by pointing at it.

## What NOT to do

- Don't try to build the JSON-RPC protocol from scratch. The SDK exists for a reason.
- Don't expose tools that take long-running mutating actions without obvious safety (e.g. `deploy_to_prod()` with no confirmation parameter).
- Don't forget to `.gitignore` `.mcp.json` if it contains secrets. The example here is safe to commit.
- Don't expose more than ~10 tools per server. Beyond that, Claude has trouble picking the right one. Split into multiple servers organized by domain.

## When you're stuck

> "I uncommented the SDK imports but `python3 .claude/mcp-servers/codebase_tools.py` still exits immediately. Read the file and tell me whether the `if __name__ == \"__main__\"` block is correctly invoking `asyncio.run(main())` — and whether the imports actually resolved (try a `python3 -c 'import mcp.server'` to check)."

## After this exercise

Look at `.claude/mcp-servers/codebase_tools.py` — that's a custom MCP server for a Java/Spring Boot codebase, ~80 lines including the stub. **Build one of these per real codebase you work on.** It's the single highest-leverage Claude Code customization most engineers never make.
