# Related projects

- **[mcp_flutter / mcp_toolkit](https://github.com/Arenukvern/mcp_flutter)**: an MCP server and
  in-app toolkit for Flutter debug apps (screenshots, widget tree, errors, app-registered tools).
  Flutter only, and its tools are hand-written functions, not screens with typed state and actions.
- **WebMCP / MCP-B**: web pages expose tools to agents in the browser. Web only; tools are per page
  and nothing compares them with what is rendered.
- **CopilotKit**: gives an in-app AI copilot access to app state and actions (React). A product
  feature for end users rather than a debug and testing interface.
- **[Chrome DevTools for agents, third-party tools](https://developer.chrome.com/docs/devtools/agents/use-cases/third-party-tools)**:
  a page registers tools that expose its state to an agent driving Chrome. The same idea for the web,
  without per-screen availability or a catalog.
- **Callstack agent-device**: lets agents drive iOS and Android devices from outside, through
  accessibility snapshots and taps. Outside-in; LS-DO adds the app's own meaning and pairs well with it.
- **Playwright MCP / agent-browser**: browser automation for agents through accessibility snapshots.
  Web only, outside-in.
- **Compose Hot Reload MCP**: an experimental MCP server in Compose Hot Reload that lets coding agents
  reload a running Compose desktop app, take screenshots and inspect its semantic tree. About editing
  code live, not driving screens by their actions.
- **cmp-bridge**: connects a running Compose Multiplatform app to an agent.

What LS-DO combines: actions and state declared by the app itself, a snapshot of what is really
rendered next to them, schemas derived from the Kotlin types, availability and busy-waiting per
screen, and a catalog of the screens not on show, aimed at testing and verifying apps.

