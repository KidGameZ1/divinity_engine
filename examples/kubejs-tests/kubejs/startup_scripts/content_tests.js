// Install only in the isolated divinity_content_kubejs GameTest directory. Restart to change declarations.
DivinityEvents.register(event => {
  event.blessing('divinity_content_kubejs:json_script', {
    schema_version: 1,
    mode: 'active',
    cooldown_seconds: 2,
    hooks: { pressed: [{ type: 'heal', target: 'self', amount: 2 }] }
  })
  event.blessing('divinity_content_kubejs:pure_script', {
    schema_version: 1,
    mode: 'active',
    cooldown_seconds: 2,
    hooks: {}
  })
  event.blessing('divinity_content_kubejs:isolation', {
    schema_version: 1,
    mode: 'active',
    cooldown_seconds: 2,
    hooks: {}
  })
  event.blessing('divinity_content_kubejs:unhandled', {
    schema_version: 1,
    mode: 'active',
    cooldown_seconds: 2,
    hooks: {}
  })
  event.god('divinity_content_kubejs:script_god', {
    schema_version: 1,
    domains: ['KubeJS fixture'],
    blessings: [
      'divinity_content_kubejs:json_script',
      'divinity_content_kubejs:pure_script',
      'divinity_content_kubejs:isolation',
      'divinity_content_kubejs:unhandled'
    ],
    offerings: { 'minecraft:apple': 1 }
  })
  event.trial('divinity_content_kubejs:script_trial', {
    schema_version: 1,
    god: 'divinity_content_kubejs:script_god',
    reward_favor: 1,
    phases: [{ name: 'Script watch', objective: 'Remain here', duration_ticks: 20, goal: 'survive' }]
  })
})
