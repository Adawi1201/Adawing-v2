# AdaWing MCP — Agent Skill

`adawing-content/SKILL.md` is a portable skill for AI coding agents (Claude
Code, Codex, OpenClaw, OpenCode) that connect to the AdaWing MCP server. It
teaches the agent the tool contracts, the content-field rules, and the review
flow — most importantly that **AI-created content is never auto-published**; it
enters a human review queue.

This skill is for the *client* agent that authors blog content, not for agents
maintaining the AdaWing codebase itself.

## Install

Copy the skill folder into the agent's skills directory, then reconnect the
`adawing` MCP server (see `../mcp.json`).

```bash
# Claude Code (user-level)
cp -R adawing-content ~/.claude/skills/

# OpenCode (user-level)
cp -R adawing-content ~/.config/opencode/skills/
```

Project-level installs work too — drop it under the project's `.claude/skills/`
or `.opencode/skills/`. Skill discovery is by the `name`/`description`
frontmatter, so the agent loads it automatically when the user asks to
draft/search/read AdaWing articles or notes.

## Keep it in sync

If the MCP tool contract changes (`Agent-MCP/.../tool/*Tool.java` or
`GetContentRulesTool`), update the tool tables in `adawing-content/SKILL.md` to
match. The MCP contract only grows (fields added, never removed), so edits here
are additive.
