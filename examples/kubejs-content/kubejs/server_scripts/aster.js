DivinityEvents.blessing('example_pantheon:guiding_breath', event => {
  if (event.hook !== 'pressed') return
  // JSON healing runs first. This optional addition uses the same cooldown cycle.
  // Only modify gameplay here on the server, not in a client or async callback.
  const owner = event.owner
  const before = owner.getHealth()
  if (before < owner.getMaxHealth() / 2) {
    owner.heal(1)
    if (owner.getHealth() > before) event.markSuccessful()
  }
})
