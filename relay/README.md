# Termux Android Remote Bridge Relay

Railway deployment source for the public MCP + persistent outbound WSS Termux bridge.

Current relay version: **0.3.3**

- MCP: `/mcp`
- Device WebSocket: `/device`
- Health: `/health`
- OAuth discovery: `/.well-known/oauth-authorization-server`
- MCP resource metadata: `/.well-known/oauth-protected-resource/mcp`
- OpenAI domain verification: `/.well-known/openai-apps-challenge`
- Privacy: `/privacy`
- Terms: `/terms`
- Support: `/support`

The Android client always initiates the WSS connection. Device transport does not use OpenAI Secure MCP Tunnel, an OpenAI Runtime API key, or OpenAI API credits.
