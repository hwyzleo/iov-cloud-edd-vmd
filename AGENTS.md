# SPEC-Driven Development 约束文档
- 参考 Notion Page ID: 37d0ec7a4b9280bd8c84c922f57f6d3e

# 全局研发环境与行为准则
- 参考 Notion Page ID: 37d0ec7a4b928005aee8d863af5b2518

# 开源车联网研发与行为准则
- 参考 Notion Page ID: 37d0ec7a4b92802d8915c7c310e24486

# 当前项目需求文档
- 参考 Notion Page ID: 37d0ec7a4b9280449f1ae54822245bf7
- 变更记录 Notion Page ID: 37f0ec7a4b9280ffbc71d0b76faa480f

# 当前项目设计文档
- 参考 Notion Page ID: 37d0ec7a4b9280dda094c7de79459037
- 变更记录 Notion Page ID: 37f0ec7a4b928090b8efd3e27d39aa7c

# 当前项目系统架构文档
- 参考 Notion Page ID: 4120ec7a4b9280f7b3e3c46aaa04d5d2

# 当前项目功能架构文档
- 参考 Notion Page ID: 41f0ec7a4b9280d499c8dec1edd13f75

# 当前项目数据库文档
- 参考 Notion Page ID: 41f0ec7a4b92806cba71e9208efe8600

# 当前项目错误码文档
- 参考 Notion Page ID: 41f0ec7a4b9280a69751d7f8ebf1e66c

# graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)
