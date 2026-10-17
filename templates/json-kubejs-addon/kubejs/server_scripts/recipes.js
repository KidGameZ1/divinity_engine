ServerEvents.recipes(event => {
  event.shaped('example_pantheon:aster_statue', [
    ' A ',
    ' S ',
    'SSS'
  ], {
    A: 'minecraft:amethyst_shard',
    S: 'minecraft:stone'
  }).id('example_pantheon:crafting/aster_statue')
})
