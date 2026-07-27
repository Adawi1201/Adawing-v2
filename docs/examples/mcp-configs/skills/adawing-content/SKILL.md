---
name: adawing-content
description: Use when creating, searching, or reading blog articles or notes (动态) on an AdaWing site through the connected "adawing" MCP server. Triggers whenever the user asks to publish/draft a post or a note, look up existing articles/notes, or fetch their content on their AdaWing blog. Covers the content-creation rules, the review flow (AI content is never auto-published), and the exact tool contracts.
---

# AdaWing Content (MCP)

## Overview

AdaWing exposes a small, stable MCP surface for authoring blog content. There
are two content types: **articles** (long-form posts) and **notes** (动态 —
short-form updates). This skill tells you how to use both correctly — the tool
contracts, the field rules, and the one thing that trips up every agent: **your
drafts are NOT published. They enter a human review queue.**

**Core principle:** never imply to the user that an article or note is live.
You create a *draft that awaits human approval*. Say so, every time.

## The Iron Law

```
AI-GENERATED CONTENT IS NEVER AUTO-PUBLISHED.
create_article_draft / create_note_draft → PENDING_REVIEW → human approves → PUBLISHED
```

If a user says "publish this to my blog," the honest, accurate action is:
create a draft and tell them it's queued for review. You cannot approve it.
There is no publish tool, by design — this holds for both articles and notes.

## When to use this skill

- User wants to turn work (a writeup, a changelog entry, a quick update) into an **article** or a **note** on their AdaWing site.
- User asks what already exists on the blog ("do I have a post about X?").
- User wants the full text of an existing article or note to edit/reference.

**Article vs note:** an article is a full long-form post (has a `summary`,
supports `tags`); a note (动态) is a short-form update, tagged with a `type`
(`PERSONAL` or `TECH`) and needs no summary. When unsure which the user wants,
ask — or default to a note for anything short and an article for anything with
sections/headings.

If the `adawing` MCP server is not connected, stop and tell the user to
configure it (see the connection section) — don't fabricate results.

## First move: read the rules, don't assume them

Before your first draft in a session, fetch the current rules for the content
type you're creating: `get_content_rules` for articles (also served as the
`adawing://content-rules` resource), or `get_note_rules` for notes. The rules
are served by the site and can change; the values below are current defaults,
not a contract to hardcode.

**Article fields** (`get_content_rules`):

| Field | Rule |
|-------|------|
| `title` | required, max 256 chars |
| `content` | required, **Markdown** |
| `summary` | optional, max 512 chars |
| `tags` | max 5, `allowCreate: false` — you cannot invent tags |
| `sourceAgent` | required, one of `codex` / `claude-code` / `openclaw` / `opencode` |

**Note fields** (`get_note_rules`):

| Field | Rule |
|-------|------|
| `title` | **optional**, max 256 chars |
| `content` | required, **Markdown** |
| `type` | **required**, one of `PERSONAL` / `TECH` |
| `sourceAgent` | required, one of `codex` / `claude-code` / `openclaw` / `opencode` |

## The tools

Eight tools, split into an **article** set and a **note** set. Names and
existing fields are frozen — the server contract only grows, never shrinks.
Call them exactly as specified.

### Articles

#### `get_content_rules`
No arguments. Returns the article field rules above. Call it first.

#### `search_articles`
Find existing articles by keyword before creating a new one — avoid duplicates.
```json
{ "keyword": "spring boot cache", "limit": 10 }
```
- `keyword` required. `limit` optional, default 10, **capped at 20** server-side.
- Returns `{ items: [{id, title, summary}], total, capped }`. If `capped` is
  true your requested limit was clamped.

#### `get_article`
Fetch one article's full Markdown by id (ids come from `search_articles`).
```json
{ "id": 42 }
```
Returns `{ id, title, content, summary, sourceAgent }`. If missing, returns
`{ error: "Article not found: 42" }` — handle it, don't pretend it exists.

#### `create_article_draft`
Create an article draft. This is the article write path.
```json
{
  "title": "Tuning Caffeine cache in Spring Boot",
  "content": "# Intro\n\nMarkdown body here...",
  "summary": "A short teaser, optional.",
  "sourceAgent": "claude-code"
}
```
- Required: `title`, `content`, `sourceAgent`.
- `sourceAgent` must be your identity from the enum above — pick the one that
  matches the tool you're running in, don't guess a fictional value.
- Returns `{ draftId: <number> }`. This is a *draft id*, not a published URL.

### Notes (动态)

#### `get_note_rules`
No arguments. Returns the note field rules above. Call it first when drafting a note.

#### `search_notes`
Find published notes by keyword. Same shape as `search_articles`.
```json
{ "keyword": "release", "limit": 10 }
```
- `keyword` required. `limit` optional, default 10, **capped at 20** server-side.
- Returns `{ items: [{id, title, type}], total, capped }`.

#### `get_note`
Fetch one note's full Markdown by id (ids come from `search_notes`).
```json
{ "id": 7 }
```
Returns `{ id, title, content, type, sourceAgent }`. If missing, returns
`{ error: "Note not found: 7" }` — handle it, don't pretend it exists.

#### `create_note_draft`
Create a note draft. This is the note write path.
```json
{
  "content": "Shipped the cache write-back buffer today.",
  "type": "TECH",
  "title": "Optional short title",
  "sourceAgent": "claude-code"
}
```
- Required: `content`, `type`, `sourceAgent`. **`title` is optional** for notes.
- `type` must be `PERSONAL` or `TECH` — pick based on the subject, don't invent
  a value.
- Returns `{ draftId: <number> }`. A draft id, not a published URL.

## Workflow

1. Decide the content type — **article** (long-form) or **note** (short update).
2. Fetch rules — `get_content_rules` for an article, `get_note_rules` for a note
   (once per session is enough).
3. Search first — `search_articles` or `search_notes` to check the topic doesn't
   already exist. If a close match turns up, ask the user whether to update the
   existing piece or add a new one.
4. Write the body as clean Markdown. Respect the length caps. For articles don't
   invent tags; for notes pick the right `type` (`PERSONAL` / `TECH`).
5. Create the draft — `create_article_draft` or `create_note_draft` — and
   capture the returned `draftId`.
6. Report honestly:
   > Draft #<draftId> created and queued for review. It won't appear on the
   > site until a human approves it in the AdaWing admin.

## Doing it right vs wrong

**Wrong**
> Done — I've published your article to the blog! ✅

**Right**
> Created draft #57 via the AdaWing MCP. It's in the review queue now; it goes
> live only after you approve it in the admin panel.

**Wrong**
> (invents `sourceAgent: "gpt"` because it's not in the enum)

**Right**
> (running inside Codex) uses `"sourceAgent": "codex"`.

**Wrong**
> (asks for 50 results, ignores that only 20 came back)

**Right**
> Checks `capped: true` and tells the user the search was limited to 20 hits.

## Connection (if not already set up)

The server is Streamable-HTTP at `http://<host>:<port>/mcp`, authenticated with
an `X-MCP-Key` header. Minimal client config:

```json
{
  "mcpServers": {
    "adawing": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": { "X-MCP-Key": "${ADAWING_MCP_KEY}" }
    }
  }
}
```

Generate the API key from the AdaWing admin (`POST /api/v2/mcp-keys`) — it's
shown once. See `../README.md` in this examples folder for the full setup.
