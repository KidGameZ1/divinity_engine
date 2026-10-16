// Alternative to the JSON example, not an additional copy of those same IDs.
DivinityEvents.register(event => {
  event.blessing('example_pantheon:guiding_breath', {
    schema_version: 1,
    favor: 50,
    cooldown_seconds: 12,
    mode: 'active',
    color: '#B5D5EF',
    particle_texture: 'minecraft:textures/particle/glint.png',
    hooks: { pressed: [{ type: 'heal', target: 'self', amount: 2 }] }
  })
  event.god('example_pantheon:aster', {
    schema_version: 1,
    domains: ['Shelter', 'Wayfinding'],
    blessings: ['example_pantheon:guiding_breath'],
    offerings: { 'minecraft:amethyst_shard': 2, 'minecraft:bread': 1 },
    prayer_favor: 1,
    color: '#B5D5EF'
  })
  event.trial('example_pantheon:trial_of_shelter', {
    schema_version: 1,
    god: 'example_pantheon:aster',
    reward_favor: 25,
    start_message: 'Aster: Keep the road open.',
    complete_message: 'Aster: You are a refuge now.',
    phases: [{ name: 'Keep the Watch', objective: 'Defeat the sentries', duration_ticks: 600,
      goal: 'defeat_all', spawns: [{ entity: 'minecraft:zombie', count: 3 }] }]
  })
})
