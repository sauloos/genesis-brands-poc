# Genesis Brands — Demo Build Plan

> **Goal:** A working end-to-end demo of the core proposition.
> Not production-ready. No auth, no payments, no infrastructure.
> One wow moment: *describe a business → receive a complete brand identity in minutes.*

---

## What We Are Proving

1. The questionnaire feels intelligent — not a form, a conversation
2. The output looks like a real brand — not generic AI output
3. End-to-end in under 3 minutes

---

## Scope

### In

- LLM-driven conversational questionnaire (3–5 dynamic turns)
- Signal extraction → structured BrandDNA
- Copy generation: tagline, mission statement, tone of voice, brand story, elevator pitch
- Visual identity: 5-colour palette with rationale, typography pair (primary + secondary font)
- Logo concept image (DALL-E 3)
- React UI: questionnaire → generating → brand output
- Docker Compose local deployment

### Out (deferred to production MVP)

| Deferred | Replaced with |
|---|---|
| Authentication / JWT | Hardcoded demo user |
| Stripe payments | Not needed for demo |
| PDF / ZIP delivery | Display on screen only |
| Email notifications | Not needed |
| Admin panel | Not needed |
| Kubernetes / AKS | Docker Compose |
| Azure Service Bus | Direct synchronous HTTP calls |
| Redis | In-memory session map |
| Cosmos DB | PostgreSQL |
| Asset Composer (logo variants) | Raw generated images |
| Delivery Service | Not needed |
| Strapi CMS | Not needed |
| Spring Cloud Gateway | Single service, no gateway |

---

## Architecture

**Single Spring Boot monolith** — internally structured with clean package boundaries
so the split into microservices later is mechanical, not a rewrite.

```
com.genesisbrands.demo
├── conversation/     ← questionnaire engine + signal extraction (calls LLM)
├── brand/            ← BrandDNA model, persistence, orchestration
├── copy/             ← copy generation (calls LLM)
├── visual/           ← colour palette, typography, logo (calls LLM + image gen)
└── api/              ← REST controllers (thin — delegates to services above)
```

**Database:** PostgreSQL (single Docker container)
- One `brands` table — stores BrandDNA as JSONB, plus status column
- One `conversation_turns` table — stores Q&A history per brand

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Backend | Java 21 + Spring Boot 3 | Matches production stack — no throwaway code |
| Database | PostgreSQL (Docker Compose) | Single container, JSONB for BrandDNA |
| LLM (text) | Azure OpenAI GPT-4o | Use Ollama or Groq locally while building (free) |
| Image gen | DALL-E 3 via Azure OpenAI | Same API key — no second integration |
| Frontend | React + Vite + Tailwind CSS | Fast to build, looks professional |
| Deployment | Docker Compose | Single `docker compose up` to run everything |

### Local dev without Azure costs
Set `AI_PROVIDER=ollama` in `.env` to route all LLM calls to a local Ollama instance
(`ollama run llama3.1`). Switch to `AI_PROVIDER=azure-openai` when demoing.
Provider is swapped via Spring profile — no code changes.

---

## API Surface (demo only)

```
POST /api/conversation/start                Start a new brand + conversation session
                                            → returns { brandId, sessionId, firstQuestion }

POST /api/conversation/{sessionId}/answer   Submit answer, receive next question
                                            → returns { nextQuestion } or { done: true, brandId }

GET  /api/conversation/{sessionId}/signals  View extracted BrandDNA signals so far

POST /api/brands/{id}/generate              Trigger generation (called after signals confirmed)
                                            → kicks off copy + visual generation, returns immediately

GET  /api/brands/{id}/status               Poll generation status (GENERATING | READY | FAILED)

GET  /api/brands/{id}/output               Full generated brand output (copy + colours + logo URL)
```

---

## Build Order

### Week 1 — Backend

| Day | Task |
|---|---|
| 1 | Project scaffolding: Spring Boot, PostgreSQL, Docker Compose, `BrandDNA` model |
| 2 | Conversation engine: LLM questionnaire turns, session state, signal extraction |
| 3 | Copy generator: tagline, mission, tone of voice, brand story via GPT-4o |
| 4 | Visual identity: colour palette + typography via GPT-4o; logo concept via DALL-E 3 |
| 5 | Wire REST API end-to-end, test full flow via Postman |

### Week 2 — Frontend + Integration

| Day | Task |
|---|---|
| 1 | React project setup (Vite + Tailwind); questionnaire screen (chat-style, one question at a time) |
| 2 | Signal review screen: show extracted BrandDNA, confirm or go back |
| 3 | Progress screen: animated states (Extracting signals… Writing copy… Creating visuals…) |
| 4 | Brand output screen: colour swatches, typography preview, copy blocks, logo image |
| 5 | End-to-end integration test, polish, Docker Compose full-stack run |

---

## Definition of Done

A person sits at the screen, types a description of their business, answers 3–5 follow-up questions,
clicks Generate, and within 3 minutes sees:

- [ ] Brand tagline
- [ ] Mission statement
- [ ] Tone of voice guide
- [ ] 5-colour palette with colour codes and rationale
- [ ] Typography recommendation (primary + secondary font with usage rules)
- [ ] Logo concept image

---

## Folder Structure

```
genesis-brands/
├── backend/                  ← Spring Boot monolith (Java 21)
│   ├── src/
│   └── pom.xml
├── frontend/                 ← React SPA (Vite + Tailwind)
│   ├── src/
│   └── package.json
├── docs/                     ← All planning documents
│   ├── demo-plan.md          ← this file
│   ├── execution-plan.md     ← full phased roadmap (Phase 1 → 3)
│   ├── requirements-summary.md
│   ├── architecture-components.md
│   ├── cost-estimates.md
│   ├── diagram-connections.md
│   └── diagrams/             ← Mermaid + draw.io diagrams
├── docker-compose.yml        ← PostgreSQL + backend + frontend
├── CLAUDE.md                 ← Claude Code context (read this first)
└── .gitignore
```

---

## Path to Production MVP After Demo

The demo monolith is structured so each internal package becomes a microservice:

| Demo package | Production service |
|---|---|
| `conversation/` | Conversation Service |
| `brand/` | Brand Engine |
| `copy/` | Copy Generator (Service Bus consumer) |
| `visual/` | Visual Generator (Service Bus consumer) |

Steps after demo lands:
1. Extract packages into standalone Spring Boot services
2. Add Azure Service Bus between them (method calls → events)
3. Add Identity Service (auth + JWT)
4. Add Stripe payment on checkout
5. Add Delivery Service (PDF + ZIP)
6. Docker Compose → Kubernetes (AKS)
