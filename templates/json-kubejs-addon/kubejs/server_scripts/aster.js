// JSON owns the IDs and cooldown. Keep its hooks empty so this water check gates the whole ability.
DivinityEvents.blessing('example_pantheon:guiding_breath', event => {
  if (event.getHook() !== 'pressed') return
  const owner = event.getOwner()
  if (!owner.isInWater()) return

  const before = owner.getHealth()
  if (before >= owner.getMaxHealth()) return
  owner.heal(2)
  if (owner.getHealth() <= before) return
  event.markSuccessful()

  // This bounded counter belongs to this blessing instance, not a shared script global.
  const instance = event.getInstance()
  if (instance !== null) {
    const tag = instance.getOrCreateTag()
    const key = 'example_pantheon:guiding_breath_successes'
    tag.putInt(key, Math.min(1000000000, Math.max(0, tag.getInt(key)) + 1))
    instance.markDirty()
  }
})
