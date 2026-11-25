"""(0.66) John's 13:36 ("the steps in Viridian leading to the gym, they are definitely the old textures. Instead of being
retextured, to sort of follow the path texturing"): the slab steps up from the path to the Gym's west side (diorite and
polished diorite slabs, John's) in the path's own look, as the path round them."""


def round66(add, room, G):
    add("swap", "diorite_slab,polished_diorite_slab,diorite_stairs,polished_diorite_stairs", (-783, 515, -779, 524, "the steps up to Viridian's Gym"),
        G["PATH"], ("25", "26"))
