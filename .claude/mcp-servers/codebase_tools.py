"""
Custom MCP server for this Spring Boot codebase.

Exposes tools that Claude Code can call directly, instead of having to spawn
shell subprocesses every time it wants to know "where is X" or "what tests
just failed". MCP servers run in their own process and speak JSON-RPC over
stdio with Claude Code.

Implement the TODOs below. The visible tests check that:
  - this file imports the `mcp` SDK
  - it registers at least one tool via @server.list_tools / @server.call_tool
  - the tools include `list_endpoints` and `summarize_test_failures`

Run manually:
    python3 .claude/mcp-servers/codebase_tools.py
(it will block on stdin waiting for JSON-RPC; that's expected — Claude Code
will speak to it.)

Reference: https://github.com/modelcontextprotocol/python-sdk
"""

from __future__ import annotations

import asyncio
import json
import re
import subprocess
from pathlib import Path
from typing import Any

# TODO(meta-05): install the `mcp` Python SDK once with:
#   pip install mcp
# Then uncomment and use these imports:
#
# from mcp.server import Server
# from mcp.server.stdio import stdio_server
# from mcp.types import Tool, TextContent

ROOT = Path(__file__).resolve().parent.parent.parent  # repo root


def list_endpoints() -> list[dict[str, str]]:
    """Walk every controller in src/main/java and extract REST endpoints.

    Returns a list of {method, path, handler} dicts. Implement by:
      1. Walking src/main/java for files ending in Controller.java
      2. For each file, look for class-level @RequestMapping (the path prefix)
      3. For each method, look for @GetMapping / @PostMapping / @PutMapping /
         @DeleteMapping / @PatchMapping and combine with the prefix
    """
    # TODO(meta-05): implement.
    raise NotImplementedError("list_endpoints not implemented yet")


def summarize_test_failures() -> dict[str, Any]:
    """Run `mvn -B test` and return a structured summary.

    Returns:
        {
          "total": int,
          "passed": int,
          "failed": int,
          "failures": [
              {"class": "...", "method": "...", "message": "..."},
              ...
          ]
        }

    Implement by:
      1. subprocess.run(["mvn", "-B", "test"], cwd=ROOT, capture_output=True)
      2. Parse the stdout for `Tests run: N, Failures: F` summary lines
      3. Parse failure messages from `[ERROR]   ClassName.method:line message`
    """
    # TODO(meta-05): implement.
    raise NotImplementedError("summarize_test_failures not implemented yet")


# TODO(meta-05): wire the two functions above into an MCP server.
#
# The shape, once you import the SDK, is roughly:
#
#   server = Server("codebase-tools")
#
#   @server.list_tools()
#   async def list_tools_handler() -> list[Tool]:
#       return [
#           Tool(
#               name="list_endpoints",
#               description="List every REST endpoint in this Spring Boot app.",
#               inputSchema={"type": "object", "properties": {}},
#           ),
#           Tool(
#               name="summarize_test_failures",
#               description="Run mvn test and summarize what failed.",
#               inputSchema={"type": "object", "properties": {}},
#           ),
#       ]
#
#   @server.call_tool()
#   async def call_tool_handler(name: str, arguments: dict) -> list[TextContent]:
#       if name == "list_endpoints":
#           result = list_endpoints()
#       elif name == "summarize_test_failures":
#           result = summarize_test_failures()
#       else:
#           raise ValueError(f"unknown tool: {name}")
#       return [TextContent(type="text", text=json.dumps(result, indent=2))]
#
#   async def main():
#       async with stdio_server() as (read, write):
#           await server.run(read, write, server.create_initialization_options())
#
#   if __name__ == "__main__":
#       asyncio.run(main())
