# SPEC-Driven Development 约束文档
- 参考 ~/.config/SPEC_GUIDE.md

# 全局研发环境与行为准则
- 参考 ~/.config/GLOBAL_GUIDE.md

# 开源车联网研发与行为准则
- 参考 ~/Projects/open-iov/cloud/parent/iov-cloud-parent-cloud/PROJECT_GUIDE.md

# 当前项目研发与行为准则
- JDK使用的是17，LOMBOK版本及编译插件在上层POM有，不用额外添加或调整
- 本地JDK17地址为：/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home
- 每次解决当前问题后，必须整体项目编译通过才算完成，如果有报错则直到解决所有报错问题后才停止

# graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)
