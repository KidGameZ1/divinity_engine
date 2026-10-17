DivinityEvents.blessing('example_pantheon:guiding_breath', event => {
  if (event.hook !== 'pressed') return
  const owner = event.owner
  if (!owner.isInWater()) return

  const before = owner.getHealth()
  if (before >= owner.getMaxHealth()) return
  owner.heal(2)
  if (owner.getHealth() > before) event.markSuccessful()
})
