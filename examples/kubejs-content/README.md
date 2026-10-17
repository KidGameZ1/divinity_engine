# KubeJS Content Example

Install Forge KubeJS 2001.6.5-build.26 and its compatible Rhino/Architectury
dependencies. Copy the `kubejs` directory into your instance. Startup declarations
must be installed on both client and server; callback logic belongs in server
scripts. Restart after changing declarations.

This variant declares content in startup JavaScript. To keep declarations in JSON
and program behavior with server scripts, use [JSON + KubeJS](../json-kubejs-content/README.md).
Choose one Aster example; they use the same IDs.
Use the resource pack from `../json-content/resourcepack` for names and descriptions.
The server callback runs after the JSON actions and marks success only when it
does useful work. See [KubeJS API](../../docs/wiki/KubeJS.md).
