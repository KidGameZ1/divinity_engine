DivinityEvents.blessing('example_pantheon:guiding_breath', event => {
  if (event.hook !== 'pressed') return
  // JSON healing runs first. This optional addition uses the same cooldown cycle.
  // Only modify gameplay here on the server, not in a client or async callback.
  if (event.owner.health < event.owner.maxHealth / 2) {
    event.owner.heal(1)
    event.markSuccessful()
  }
})
