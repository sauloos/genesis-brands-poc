# Genesis Brands — Claude Code Context

## What this project is

Genesis Brands is an AI-powered brand generation platform for startups.
A user describes their business in natural language; the platform generates a complete brand identity
(logo, colours, typography, copy, brand guidelines) in minutes.

**This repo is the working codebase.** The `docs/` folder contains all planning documents.

## Current stage

**Building the demo** — a working end-to-end proof of concept, not production-ready.
See `docs/demo-plan.md` for full scope and build order.

**Not yet started:** backend and frontend are empty scaffolds.

## What we are building (demo)

A single Spring Boot monolith + React SPA that demonstrates the core flow:

```
User describes business → LLM questionnaire (3-5 turns) → signal extraction
→ copy generation (tagline, mission, tone of voice, brand story)
→ visual identity (colours, typography, logo concept image)
→ brand output displayed on screen
```

## Tech stack

| Layer | Choice |
|---|---|
| Backend | Java 21 + Spring Boot 3 (monolith for demo) |
| Database | PostgreSQL |
| LLM | Azure OpenAI GPT-4o (Ollama locally for dev — set `AI_PROVIDER=ollama`) |
| Image gen | DALL-E 3 via Azure OpenAI |
| Frontend | React + Vite + Tailwind CSS |
| Local run | Docker Compose |

## Key architectural decisions

- **Monolith for demo, microservices for production.** Internal packages map 1:1 to future services:
  `conversation/` → Conversation Service, `brand/` → Brand Engine, `copy/` → Copy Generator, `visual/` → Visual Generator.
- **No auth for demo.** Hardcoded demo user. JWT + Identity Service is Phase 1 production scope.
- **No Service Bus for demo.** Direct synchronous HTTP between internal services. Service Bus added when splitting into microservices.
- **Provider abstraction for LLM and image gen.** A Spring `@Profile`-switched bean swaps Ollama ↔ Azure OpenAI. Never hardcode the provider.
- **BrandDNA is the core domain object.** Stored as JSONB in PostgreSQL (Cosmos DB in production). All generation writes into it.

## Internal package structure (backend)

```
com.genesisbrands.demo
├── conversation/     LLM questionnaire + signal extraction
├── brand/            BrandDNA model, persistence, orchestration
├── copy/             Copy generation (tagline, mission, tone, story)
├── visual/           Colour palette, typography, logo image generation
└── api/              REST controllers — thin, delegate to services
```

## Demo API surface

```
POST /api/conversation/start
POST /api/conversation/{sessionId}/answer
GET  /api/conversation/{sessionId}/signals
POST /api/brands/{id}/generate
GET  /api/brands/{id}/status
GET  /api/brands/{id}/output
```

## Key documents

| Document | What it covers |
|---|---|
| `docs/demo-plan.md` | Demo scope, build order, definition of done |
| `docs/execution-plan.md` | Full phased roadmap (Phase 1 MVP → Phase 3 Marketplace) |
| `docs/requirements-summary.md` | Full functional + non-functional requirements |
| `docs/architecture-components.md` | Every service, its purpose, example endpoints |
| `docs/cost-estimates.md` | Dev and prod Azure cost breakdown |
| `docs/diagram-connections.md` | All service connections for diagram cross-checking |
| `docs/diagrams/06-deployment.drawio` | Full production deployment diagram |
| `docs/diagrams/07-screen-flow.mmd` | React app screen flow |
| `docs/diagrams/08-screen-flow-apis.mmd` | Screen flow annotated with API calls |

## Production target (after demo)

Full microservices on AKS (Azure Kubernetes Service):
- Java 21 + Spring Boot 3 services
- Azure Service Bus for async generation pipeline
- Cosmos DB for BrandDNA documents
- PostgreSQL for relational data (users, orders, content library)
- React SPA on Azure Blob Storage + Front Door CDN
- Stripe for payments (Essential £179, Premium £279, Ultimate £679)

See `docs/execution-plan.md` for the full phased roadmap.
