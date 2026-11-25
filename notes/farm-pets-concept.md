# Farm and pet Retromon: a first concept (John's 13:27)

John asked: "create a new RetroMon ... outside your Kanto and Johto equivalents ... farm/pet. So I think a dog, a cat,
a pig, a cow, a sheep, a horse ... chicken as well ... Moomin type cows, or the ones that you see in like Harvest
Moon ... Let them have an evolution, because you'd have a male and female version. You know, you'd have a bull ...
For the dog and cat type ones, probably not ... the interactions with them would be slightly different to, say, a
Pokemon being caught."

The 17 designs are in the Creature Viewer (the "Farm" and "Pets" groups at the end of the list). They aren't in the
game yet. Their models are in tools/farm (made by tools/mons11.py) and their data is in tools/farm/farm.tsv.

## The creatures

Each young one grows into one of two forms: the first if it's a she, the second if it's a he. The dog and the cat
don't grow up.

| Line | Young | She | He |
|---|---|---|---|
| Cattle | MOOPIP (calf) | MOOBELLE (cow: bell, flower, udder) | BRAWNOX (bull: long horns, nose ring) |
| Sheep | LAMBLET (lamb) | FLEECELLE (ewe: a bow) | CURLRAM (ram: curled horns, dark face) |
| Pigs | OINKLET (piglet) | TRUFFLESOW (sow) | TUSKHOG (boar: bristles, tusks) |
| Chickens | PEEPLING (chick) | CLUCKMAID (hen) | CROWCOMB (rooster: comb, cape, sickle tail) |
| Horses | FOALET (foal) | MAREBRAID (mare: braided cream mane) | STORMSTEED (stallion: dark bay, black mane, feathered hooves) |
| Dog | HEELHOUND (a collie) | | |
| Cat | BARNPURR (a ginger tabby) | | |

## The dog's and the cat's coats (John's 14:40)

Every dog and every cat has the same build and size, but each is born in one of seven breeds' colours, so the
farm's pets vary. In the Creature Viewer, each coat is its own card in the dog's and the cat's groups.

| HEELHOUND (the dog) | BARNPURR (the cat) |
|---|---|
| **Collie:** black and white, with tan on its cheeks and brows | **Ginger tabby:** ginger with darker stripes, a white chest and paws |
| **Golden:** golden all over, with a cream ruff | **Tuxedo:** black, with a white chest, paws and muzzle, and amber eyes |
| **Red and cream:** fox red, with cream cheeks, chest and paws | **Pointed:** cream, with dark brown ears, face, legs and tail, and blue eyes |
| **Spotted:** white with black spots and black ears | **Calico:** white with orange and black patches |
| **Snow:** grey and white, with a white mask and blue eyes | **Silver tabby:** silver grey with darker stripes, a white chest and paws |
| **Cocoa:** chocolate brown all over, with amber eyes | **Midnight:** black all over, with yellow eyes |
| **Blue merle:** blue-grey marbled with black, a white ruff and paws, tan points and blue eyes | **Blue grey:** soft blue-grey all over, with green eyes |

The coat could be random when a pet is given or found, or you could pick one at the ranch's litter.

## How they'd play

These are not caught. You get them in other ways:

- You buy them at a ranch or stable.
- You're given one (for example, a puppy from the ranch's litter).
- One is born on your farm to a pair you keep.
- A stray follows you home.

**Growing up.** A young one grows up after some days of care, if it's fed, groomed and let out:

- 3 days for a chick
- 5 days for a lamb or a piglet
- 7 days for a calf
- 10 days for a foal

**Friendship.** Each one has a friendship meter (hearts). More hearts mean more produce, better quality and a faster
horse.

**What each one gives or does:**

- **MOOBELLE:** MOOMOO MILK each morning (a full bottle heals 100 HP).
- **BRAWNOX:** pulls the plough (tills a field in one go) and moves the farm's boulders.
- **FLEECELLE:** wool every 3 days when sheared, sold or made into clothes at the tailor's.
- **CURLRAM:** butts stumps and rocks out of a field.
- **TRUFFLESOW:** finds truffles and buried items when you walk her in the woods.
- **TUSKHOG:** digs up roots and clears scrub.
- **CLUCKMAID:** an egg most mornings. You can eat it, sell it, or keep it warm to hatch.
- **CROWCOMB:** crows at dawn, so the farm starts its day earlier. You need him for eggs that hatch.
- **MAREBRAID:** an easy ride, with saddlebags for more bag space.
- **STORMSTEED:** a fast ride that jumps fences.
- **HEELHOUND:** herds the animals in at dusk, digs things up, keeps watch at night and comes when you whistle.
- **BARNPURR:** keeps RATTATA off the feed, leaves gifts on the doorstep and naps in the sun.

**Breeding.** A she and a he of the same line together in the barn or coop give young now and then.

**Story hooks.** Some ideas:

- A ranch outside Pallet that needs a hand.
- A lost calf to find on Route 1.
- Rustlers (TEAM ROCKET) after the MOOBELLEs.
- A county fair with prizes for the best animal.
- The dog that keeps you company on the road.

## Open questions for John

- **Battles.** Do they battle at all? For example, the horses and the dog could battle at your side, while the others
  only work the farm.
- **Which farm.** Is it your own farm, a ranch in a town, or both?
- **Tie-ins.** Should this tie into the outfits (wool into clothes) and the shops?
