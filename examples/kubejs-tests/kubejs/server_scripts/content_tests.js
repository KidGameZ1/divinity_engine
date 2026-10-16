// Per-instance counters let Java assert real callback dispatch without importing KubeJS classes.
DivinityEvents.blessing('divinity_content_kubejs:json_script', event => {
  if (event.getHook() !== 'pressed') return
  const tag = event.getInstance().getOrCreateTag()
  tag.putInt('json_calls', tag.getInt('json_calls') + 1)
  tag.putBoolean('json_success_before_script', event.isSuccessful())
  tag.putString('seen_hook', event.getHook())
  tag.putString('seen_id', event.getId().toString())
  tag.putBoolean('pressed_nullables', event.getTarget() === null && event.getNativeEvent() === null)
  const owner = event.getOwner()
  const before = owner.getHealth()
  owner.heal(1)
  if (owner.getHealth() > before) event.markSuccessful()
  tag.putBoolean('success_after_script', event.isSuccessful())
})

DivinityEvents.blessing('divinity_content_kubejs:pure_script', event => {
  if (event.getHook() !== 'pressed') return
  const tag = event.getInstance().getOrCreateTag()
  tag.putInt('pure_calls', tag.getInt('pure_calls') + 1)
  tag.putBoolean('pure_success_before_script', event.isSuccessful())
  const owner = event.getOwner()
  const before = owner.getHealth()
  owner.heal(2)
  if (owner.getHealth() > before) event.markSuccessful()
  tag.putBoolean('pure_success_after_script', event.isSuccessful())
})

DivinityEvents.blessing('divinity_content_kubejs:isolation', event => {
  if (event.getHook() !== 'pressed') return
  const tag = event.getInstance().getOrCreateTag()
  tag.putInt('isolation_calls', tag.getInt('isolation_calls') + 1)
  const owner = event.getOwner()
  const before = owner.getHealth()
  owner.heal(1)
  if (owner.getHealth() > before) event.markSuccessful()
})
