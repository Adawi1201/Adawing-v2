package cc.adabyte.agent.mcp.controller;

import cc.adabyte.agent.mcp.filter.McpSessionFilter;
import cc.adabyte.agent.mcp.session.InMemoryMcpSessionStore;
import cc.adabyte.agent.mcp.session.McpSessionStore;
import cc.adabyte.agent.mcp.tool.McpToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpServerControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final McpSessionStore sessionStore = new InMemoryMcpSessionStore();

    @Test
    void initializeSessionIdShouldRemainStableForSubsequentRequests() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new McpServerController(new McpToolRegistry(List.of()), objectMapper, sessionStore))
                .addFilters(new McpSessionFilter())
                .build();

        String initializeBody = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-03-26",
                    "capabilities": {
                      "tools": {}
                    },
                    "clientInfo": {
                      "name": "codex",
                      "version": "test"
                    }
                  }
                }
                """;

        String sessionId = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initializeBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Mcp-Session-Id");

        assertThat(sessionId).isNotBlank();

        String toolsListBody = """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/list"
                }
                """;

        String nextSessionId = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", sessionId)
                        .content(toolsListBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Mcp-Session-Id");

        assertThat(nextSessionId).isEqualTo(sessionId);
    }

    @Test
    void toolsListShouldRejectMissingSessionHeader() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new McpServerController(new McpToolRegistry(List.of()), objectMapper, sessionStore))
                .addFilters(new McpSessionFilter())
                .build();

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 2,
                                  "method": "tools/list"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(-32000));
    }

    @Test
    void toolsListShouldRejectUnknownSessionHeader() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new McpServerController(new McpToolRegistry(List.of()), objectMapper, sessionStore))
                .addFilters(new McpSessionFilter())
                .build();

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", "missing-session")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 2,
                                  "method": "tools/list"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(-32001));
    }

    @Test
    void initializeShouldStoreClientMetadata() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new McpServerController(new McpToolRegistry(List.of()), objectMapper, sessionStore))
                .addFilters(new McpSessionFilter())
                .build();

        String sessionId = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-03-26",
                                    "clientInfo": {
                                      "name": "codex",
                                      "version": "1.2.3"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Mcp-Session-Id");

        assertThat(sessionId).isNotBlank();
        assertThat(sessionStore.getSession(sessionId))
                .isPresent()
                .get()
                .extracting("clientName", "clientVersion", "protocolVersion")
                .containsExactly("codex", "1.2.3", "2025-03-26");
    }
}
