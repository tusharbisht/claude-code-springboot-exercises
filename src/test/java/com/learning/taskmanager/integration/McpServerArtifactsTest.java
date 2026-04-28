package com.learning.taskmanager.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visible tests for meta/05-mcp-servers.
 *
 * The deliverable is a working custom MCP server. These tests check that
 * the configuration and the server file have the right shape. The hidden
 * grading suite goes deeper.
 */
@DisplayName("[meta-05] custom MCP server for this codebase")
class McpServerArtifactsTest {

    private static final Path MCP_JSON = Path.of(".mcp.json");
    private static final Path SERVER_PY = Path.of(".claude/mcp-servers/codebase_tools.py");

    @Test
    @DisplayName(".mcp.json exists at the repo root")
    void mcpJsonExists() {
        assertThat(MCP_JSON)
                .as("Create .mcp.json at the repo root. Use .mcp.json.example as a starting "
                        + "point. The file tells Claude Code how to launch the codebase-tools MCP server.")
                .exists();
    }

    @Test
    @DisplayName(".mcp.json registers a 'codebase-tools' server pointing at the Python script")
    void mcpJsonRegistersServer() throws IOException {
        if (!Files.exists(MCP_JSON)) return;
        String content = Files.readString(MCP_JSON);
        assertThat(content)
                .as(".mcp.json should declare an `mcpServers` object")
                .contains("mcpServers");
        assertThat(content)
                .as("the 'codebase-tools' server should be registered")
                .contains("codebase-tools");
        assertThat(content)
                .as(".mcp.json should reference the Python server file")
                .contains("codebase_tools.py");
    }

    @Test
    @DisplayName("the MCP server script imports the mcp SDK (uncommented)")
    void serverImportsSdk() throws IOException {
        String content = Files.readString(SERVER_PY);
        assertThat(content.split("\\n"))
                .as("the MCP server file should have an UNCOMMENTED `from mcp...import` line. "
                        + "Install the SDK with `pip install mcp` and uncomment the imports the "
                        + "file already has commented out.")
                .anyMatch(line -> {
                    String t = line.trim();
                    return t.startsWith("from mcp.server import") || t.startsWith("from mcp import")
                            || t.startsWith("from mcp.server.stdio import");
                });
    }

    @Test
    @DisplayName("the MCP server registers list_endpoints and summarize_test_failures")
    void serverRegistersRequiredTools() throws IOException {
        String content = Files.readString(SERVER_PY);
        assertThat(content)
                .as("the server should register an MCP tool named 'list_endpoints'")
                .contains("list_endpoints");
        assertThat(content)
                .as("the server should register an MCP tool named 'summarize_test_failures'")
                .contains("summarize_test_failures");
        assertThat(content)
                .as("the server should use the SDK's @server.list_tools / @server.call_tool decorators")
                .containsAnyOf("@server.list_tools", "@server.call_tool", "list_tools_handler", "call_tool_handler");
    }

    @Test
    @DisplayName("list_endpoints actually walks the codebase (not just stubbed NotImplementedError)")
    void listEndpointsImplemented() throws IOException {
        String content = Files.readString(SERVER_PY);
        // The starter raises NotImplementedError. The learner needs to remove that and implement.
        assertThat(content.split("\\n"))
                .as("list_endpoints should not be a NotImplementedError stub anymore. "
                        + "Implement it to walk src/main/java for *Controller.java files and "
                        + "extract @GetMapping/@PostMapping/etc.")
                .noneMatch(line -> line.trim().startsWith("raise NotImplementedError")
                        && line.contains("list_endpoints"));
    }
}
