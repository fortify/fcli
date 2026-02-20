# fcli Agent Skills

A collection of AI agent Skills built around [Fortify CLI (fcli)](https://github.com/fortify/fcli), enabling AI coding agents to effectively automate interaction with OpenText Fortify products.

> These skills follow the [Agent Skills open standard](https://agentskills.io/) and work across GitHub Copilot (VS Code, CLI), Claude Code, and other compatible agents.

## What's Here

| Directory | Purpose |
|-----------|---------|
| `.github/skills/` | Agent Skills – loaded on-demand when the task matches the skill description |
| `.github/prompts/` | Prompt files – reusable slash commands for common fcli workflows |
| `.github/agents/` | Custom agents – specialized Fortify personas |
| `docs/` | Architecture, backlog, and contribution guides |
| `mcp/` | MCP server configuration for fcli's built-in MCP server |

## Supported Fortify Products

| Product | Module | Skills |
|---------|--------|--------|
| Fortify on Demand (FoD) | `fcli fod` | Authentication, releases, SAST scans, vulnerabilities |
| Software Security Center (SSC) | `fcli ssc` | Authentication, app versions, vulnerabilities |
| ScanCentral SAST | `fcli sc-sast` | SAST scans via SSC/SC-SAST |
| ScanCentral DAST | `fcli sc-dast` | DAST scan management |
| fcli MCP Server | `fcli util mcp-server` | LLM/MCP integration |

## Quick Start

### Prerequisites

- [fcli](https://github.com/fortify/fcli/releases) v3.x installed and on your `PATH`
- A Fortify product license (FoD tenant **or** SSC instance)
- GitHub Copilot (VS Code) or another Agent Skills-compatible agent

### Using Skills

Skills load automatically when your chat prompt matches a skill's description. You can also invoke them explicitly:

```
/fod-authenticate          # Login to Fortify on Demand
/fod-create-release        # Create a FoD release for the current branch
/fod-run-sast-scan         # Package and submit a SAST scan to FoD
/fod-list-vulnerabilities  # Review vulnerabilities for a FoD release

/ssc-authenticate          # Login to SSC
/ssc-create-appversion     # Create an SSC application version
/ssc-run-sast-scan         # Submit a SAST scan via ScanCentral SAST
/ssc-list-vulnerabilities  # Review vulnerabilities in SSC
```

### MCP Integration

fcli has a built-in MCP server. See [mcp/README.md](mcp/README.md) to configure it for use with VS Code or any MCP-compatible client.

## Architecture

See [docs/architecture.md](docs/architecture.md) for the full design rationale and component breakdown.

## Backlog

See [docs/backlog.md](docs/backlog.md) for the prioritized list of planned skills and improvements.

## Contributing

Skills ultimately target the [fcli repository](https://github.com/fortify/fcli). Until then, contributions to this repo are welcome — see [docs/contributing.md](docs/contributing.md).
