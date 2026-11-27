#!/usr/bin/env python3
"""
(0.71, John's 27 Nov 18:06) The EVENT PICKER's steps (src/main/resources/retromon/data/kanto_events.tsv), from the
Kanto Campaign Events sheet (tools/kanto_sheet_rows.json: its rows, step by step) and, for each step, where BEFORE IT
and IN IT take you, and what's done once it's over (each later step's game has it done):

  go:    start | out:<door> (outside a building's door) | room:<id>[@x,z[,yaw]][|<door>] | xz:<x>,<z>[,<yaw>]
  gives: item:<id>[:n] take:<id> story:<flag> unstory:<flag> static:<key> badge:<NAME> beat:<key>[,<key>] visit:<place> caught:<n>

The party's point (Campaign.STAGES) is the badges you'd have by then (CHAMPION once you've won).

    python3 tools/kanto_events.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data", "kanto_events.tsv")

def gym(n, badge, tm):
    return "badge:%s beat:%s_gym_0 static:gift:%s_gym:%s item:%s" % (badge, n, n, tm, tm)

# step: (before, in, gives)
S = {
    1: ("start", "start", ""),
    2: ("out:home", "out:home", ""),
    3: ("room:lab", "room:lab", "started"),
    4: ("room:lab", "room:lab", ""),
    5: ("xz:-802,652", "xz:-802,645", "item:potion"),
    6: ("out:viridian_mart", "room:mart|viridian_mart", "story:parcel item:oaks_parcel visit:viridian_city story:townmap"),
    7: ("out:lab", "room:lab", "take:oaks_parcel unstory:parcel story:pokedex item:poke_ball:5 beat:rival_lab"),
    8: ("out:lab", "out:rival", "story:townmap"),
    9: ("xz:-796,531", "xz:-796,522", "static:gift:viridian_old_man:teachy_tv item:teachy_tv"),
    10: ("xz:-795,582", "xz:-795,569", "story:rocket_viridian beat:rocket_viridian"),
    11: ("xz:-852,535,90", "xz:-864,535,90", "beat:rival_22"),
    12: ("xz:-766,534", "out:viridian_gym", ""),
    13: ("out:forest_south", "room:viridian_forest", "visit:viridian_forest"),
    14: ("out:pewter_museum", "room:museum_1f", "story:museum_paid static:gift:old_amber item:old_amber"),
    15: ("xz:-760,306,270", "xz:-754,306,270", ""),
    16: ("out:pewter_gym", "room:pewter_gym", gym("pewter", "BOULDERBADGE", "tm_bide")),
    17: ("xz:-760,306,270", "xz:-750,306,270", "story:running_shoes item:running_shoes"),
    18: ("out:route_4_center", "room:center|route_4_center", ""),
    19: ("out:route_4_center", "xz:-531,256", ""),
    20: ("out:mt_moon_route_3", "room:mt_moon_b2f", "beat:rocket_jj_mtmoon,mt_moon_b2f_0 static:item:mt_moon_b2f_dome_fossil item:dome_fossil"),
    21: ("xz:-352,253", "xz:-352,243", "beat:rival_cerulean"),
    22: ("xz:-352,238", "xz:-352,200", "beat:route_24_1,route_24_2,route_24_3,route_24_4,route_24_5,route_24_0 static:gift:route_24:nugget item:nugget"),
    23: ("out:bills_house", "room:bills_house", "static:bill_in_pod static:bill_separated static:pet:bills_house_clefairy static:gift:bills_house:s_s_ticket item:s_s_ticket"),
    24: ("out:cerulean_house1", "out:cerulean_house1_back", "beat:cerulean_city_0 static:gift:cerulean_city:tm_dig item:tm_dig"),
    25: ("out:cerulean_gym", "room:cerulean_gym", gym("cerulean", "CASCADEBADGE", "tm_bubblebeam")),
    26: ("out:gate_r5_b", "room:gate_r5|gate_r5_b", ""),
    27: ("out:daycare", "room:daycare", ""),
    28: ("out:ug_route5", "room:ug_r5", ""),
    29: ("out:pokemon_fan_club", "room:pokemon_fan_club", "static:bike_voucher item:bike_voucher"),
    30: ("out:bike_shop", "room:bike_shop", "take:bike_voucher item:bicycle"),
    31: ("out:vermilion_old_rod_house", "room:vermilion_old_rod_house", "item:old_rod"),
    32: ("xz:-353,603", "out:ss_anne_gangway", ""),
    33: ("room:ss_anne_1f|ss_anne_gangway", "room:ss_anne_2f|ss_anne_gangway", "beat:rival_ssanne"),
    34: ("room:ss_anne_2f|ss_anne_gangway", "room:ss_anne_captains_room|ss_anne_gangway", "static:gift:ss_anne_captains_room:hm_cut item:hm_cut"),
    35: ("room:ss_anne_1f|ss_anne_gangway", "out:ss_anne_gangway", "static:ss_anne_sailed"),
    36: ("out:vermilion_gym", "room:vermilion_gym", gym("vermilion", "THUNDERBADGE", "tm_thunderbolt")),
    37: ("out:route2_gate_s", "room:route2_gate", "static:gift:route_2_gate:hm_flash item:hm_flash caught:10"),
    38: ("out:cerulean_city_center", "out:route_10_center", ""),
    39: ("out:rock_tunnel_north", "room:rock_tunnel_1f", ""),
    40: ("out:pokemon_tower", "room:pokemon_tower_2f", "beat:rival_tower"),
    41: ("room:pokemon_tower_2f", "room:pokemon_tower_3f", ""),
    42: ("room:pokemon_tower_5f", "room:pokemon_tower_6f", ""),
    43: ("out:ug_route8", "room:ug_r8", ""),
    44: ("out:celadon_mansion", "room:celadon_mansion_1f", "static:gift:celadon_mansion_1f:tea item:tea"),
    45: ("out:celadon_mansion", "room:celadon_mansion_roof_house", "static:eevee_gift"),
    46: ("out:celadon_dept_w", "room:celadon_mart_1f", ""),
    47: ("out:celadon_diner", "room:celadon_diner", "static:gift:celadon_diner:coin_case item:coin_case"),
    48: ("out:celadon_game_corner", "room:game_corner", "static:hideout_open"),
    49: ("room:game_corner", "room:rocket_hideout_b1f", "static:item:rocket_hideout_b4f_i4 item:lift_key"),
    50: ("room:rocket_hideout_b3f", "room:rocket_hideout_b4f", "beat:rocket_jj_hideout,rocket_hideout_b4f_0 static:item:rocket_hideout_b4f_i3 item:silph_scope"),
    51: ("out:celadon_gym", "room:celadon_gym", gym("celadon", "RAINBOWBADGE", "tm_mega_drain")),
    52: ("room:pokemon_tower_5f", "room:pokemon_tower_6f", "static:marowak_calmed"),
    53: ("room:pokemon_tower_6f", "room:pokemon_tower_7f",
         "beat:rocket_jj_tower,pokemon_tower_7f_fr_team_rocket_grunt_19,pokemon_tower_7f_fr_team_rocket_grunt_20,pokemon_tower_7f_fr_team_rocket_grunt_21 static:fuji_rescued"),
    54: ("out:lavender_volunteer_house", "room:lavender_volunteer_house", "static:gift:mr_fujis_house:poke_flute item:poke_flute"),
    55: ("xz:-124,574", "xz:-124,581", "static:snorlax_route_12 static:snorlax_route_16"),
    56: ("out:route12_house", "room:route12_house", "item:super_rod"),
    57: ("out:route16_house", "room:route16_house", "static:gift:route_16_fly_house:hm_fly item:hm_fly"),
    58: ("out:gate_r16n_a", "room:gate_r16n|gate_r16n_a", ""),
    59: ("out:fuchsia_house4", "room:fuchsia_house4", "item:good_rod"),
    60: ("out:fuchsia_safari", "room:safari_gate", "static:gift:safari_zone_secret_house:hm_surf item:hm_surf static:item:safari_zone_west_gold_teeth static:item:safari_zone_west_i3 item:gold_teeth"),
    61: ("out:fuchsia_house3", "room:fuchsia_house3", "take:gold_teeth static:gift:wardens_house:hm_strength item:hm_strength"),
    62: ("out:fuchsia_gym", "room:fuchsia_gym", gym("fuchsia", "SOULBADGE", "tm_toxic")),
    63: ("out:gate_r11_a", "room:gate_r11_2f|gate_r11_a", "static:gift:route_11_gate_2f:itemfinder item:itemfinder caught:30"),
    64: ("out:gate_r15_a", "room:gate_r15_2f|gate_r15_a", "static:gift:route_15_gate_2f:exp_share item:exp_share caught:50"),
    65: ("out:gate_r7_a", "room:gate_r7|gate_r7_a", ""),
    66: ("out:silph_co", "room:silph_co_5f", "static:item:silph_co_5f_i2 item:card_key"),
    67: ("room:silph_co_5f", "room:silph_co_7f", "beat:rival_silph static:lapras_gift"),
    68: ("room:silph_co_7f", "room:silph_co_11f", "beat:rocket_jj_silph,silph_co_11f_0 static:gift:silph_co_11f:master_ball item:master_ball"),
    69: ("out:saffron_dojo", "room:fighting_dojo", ""),
    70: ("out:mr_psychics_house", "room:mr_psychics_house", "static:gift:mr_psychics_house:tm_psychic_m item:tm_psychic_m"),
    71: ("out:copycats_house", "room:copycats_house_2f", ""),
    72: ("out:saffron_gym", "room:saffron_gym", gym("saffron", "MARSHBADGE", "tm_psywave")),
    73: ("out:seafoam_west", "room:seafoam_islands_1f", ""),
    74: ("out:power_plant", "room:power_plant", ""),
    75: ("out:cinnabar_mansion", "room:pokemon_mansion_1f", "static:item:pokemon_mansion_b1f_i4 item:secret_key"),
    76: ("out:cinnabar_gym", "room:cinnabar_gym", gym("cinnabar", "VOLCANOBADGE", "tm_fire_blast") + " static:door:cinnabar_gym"),
    77: ("out:cinnabar_lab", "room:cinnabar_lab_fossil_room|cinnabar_lab", ""),
    78: ("out:cinnabar_island_center", "out:cinnabar_island_center", ""),
    79: ("out:viridian_gym", "room:viridian_gym", gym("viridian", "EARTHBADGE", "tm_fissure")),
    80: ("xz:-852,535,90", "xz:-864,535,90", "beat:rival_22b"),
    81: ("out:league_n", "out:league_n", ""),
    82: ("out:victory_road_west", "room:victory_road_1f", ""),
    83: ("out:indigo_plateau", "room:loreleis_room|indigo_plateau", "beat:loreleis_room_0,brunos_room_0,agathas_room_0,lances_room_0"),
    84: ("room:lances_room|indigo_plateau", "room:champions_room|indigo_plateau", "beat:rival_champion story:champion"),
    85: ("room:champions_room|indigo_plateau", "room:hall_of_fame|indigo_plateau", ""),
    86: ("out:cerulean_cave", "room:cerulean_cave_1f", ""),
    87: ("out:cinnabar_island_center", "out:cinnabar_island_center", ""),
}

BADGE_AT = {16: 1, 25: 2, 36: 3, 51: 4, 62: 5, 72: 6, 76: 7, 79: 8, 84: 9}


def clean(v):
    return "-" if v in (None, "", "None") else str(v).replace("\t", " ").replace("\n", " ")


def main():
    rows = json.load(open(os.path.join(HERE, "kanto_sheet_rows.json")))
    out = ["# The EVENT PICKER's steps (tools/kanto_events.py, from the Kanto Campaign Events sheet).",
           "# step\tstage\tmain\tbefore\tin\tgives\tstage name\twhere\twhat happens\tyou need\tyou get"]
    stage = 0
    for r in rows:
        n = int(r[0])
        before, inn, gives = S[n]
        gives = " ".join(g for g in gives.split() if g != "started")
        out.append("\t".join([str(n), str(stage), clean(r[8]), before, inn, gives or "-", clean(r[1]), clean(r[2]), clean(r[3]), clean(r[6]), clean(r[7])]))
        stage = BADGE_AT.get(n, stage)
    open(OUT, "w").write("\n".join(out) + "\n")
    print(len(rows), "steps")


if __name__ == "__main__":
    main()
