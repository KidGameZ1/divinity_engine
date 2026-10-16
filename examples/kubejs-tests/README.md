# Isolated KubeJS Tests

These fixture scripts are for automated tests, not a gameplay pack. Copy `kubejs/`
to `~/.gradle/workspace-runs/divinity-content-kubejs-gametest/kubejs/`, then run:

```sh
./gradlew runGameTestServer -PgameTestNamespaces=divinity_content_kubejs -PwithKubeJS=true --console=plain
```

The suite contains four tests and intentionally has no Java registration fallback.
The separate `divinity_content` suite runs without KubeJS. Never install these
fixtures into a personal world or normal production server.
