/**
 * SpeedCheck Web (PWA) - Smart GPS Speed Monitor & MTTD Camera Radar for Ghana
 * Highly accurate speed calculation, jitter-resistant filtering, and iOS permission handling.
 */

// 1. Ghana MTTD Automated Speed Camera Zones
const GHANA_SPEED_CAMERAS = [
  {
    id: "borteyman_junction",
    name: "Borteyman Sports Facility T-Junction",
    roadName: "Borteyman / Motorway Corridor",
    latitude: 5.6675,
    longitude: -0.1030,
    speedLimitKmh: 80,
    radiusMeters: 1400,
    description: "Automated camera enforcing speed along the Borteyman Sports Complex link."
  },
  {
    id: "n1_fiesta_royale",
    name: "N1 Highway (Fiesta Royale Hotel)",
    roadName: "George Walker Bush Highway (N1)",
    latitude: 5.6062,
    longitude: -0.1985,
    speedLimitKmh: 80,
    radiusMeters: 1500,
    description: "High-speed radar camera mounted near Fiesta Royale Hotel / Dzorwulu footbridge."
  },
  {
    id: "n1_baah_junction",
    name: "N1 Highway (Baah / Bar Junction)",
    roadName: "George Walker Bush Highway (N1)",
    latitude: 5.5925,
    longitude: -0.2580,
    speedLimitKmh: 80,
    radiusMeters: 1500,
    description: "Automated camera monitoring the Lapaz to Kwashieman / Baah Junction corridor."
  },
  {
    id: "kawukudi_obasanjo",
    name: "Kawukudi (Obasanjo Highway)",
    roadName: "Olusegun Obasanjo Highway",
    latitude: 5.5892,
    longitude: -0.1932,
    speedLimitKmh: 50,
    radiusMeters: 1200,
    description: "Automated camera enforcing the 50 km/h urban limit at Kawukudi Junction."
  },
  {
    id: "kanda_highway",
    name: "Kanda Highway",
    roadName: "Kanda Highway",
    latitude: 5.5780,
    longitude: -0.1970,
    speedLimitKmh: 50,
    radiusMeters: 1200,
    description: "Automated camera monitoring the Kanda Highway arterial corridor."
  },
  // Central / East Accra & Coastal Corridors
  {
    id: "liberation_nehru_jct",
    name: "Liberation Rd (Jawaharlal Nehru / Liberation Link)",
    roadName: "Liberation Road (N4)",
    latitude: 5.5882,
    longitude: -0.1818,
    speedLimitKmh: 50,
    radiusMeters: 1200,
    description: "Speed camera monitoring Liberation Rd near Airport City and Jawaharlal Nehru Rd junction."
  },
  {
    id: "obasanjo_liberation_end",
    name: "Obasanjo Highway (37 / Liberation End)",
    roadName: "Olusegun Obasanjo Way",
    latitude: 5.5862,
    longitude: -0.1865,
    speedLimitKmh: 50,
    radiusMeters: 1100,
    description: "Enforcing 50 km/h limit on the eastern end of Obasanjo Way near 37 Interchange."
  },
  {
    id: "cantonments_road",
    name: "Cantonments Road (Osu / Cantonments)",
    roadName: "Cantonments Road",
    latitude: 5.5565,
    longitude: -0.1812,
    speedLimitKmh: 50,
    radiusMeters: 1000,
    description: "Automated speed monitoring on the Cantonments / Osu arterial route."
  },
  {
    id: "ring_road_nima_kanda",
    name: "Ring Road Central (Nima / Kanda Overpass)",
    roadName: "Ring Road Central",
    latitude: 5.5742,
    longitude: -0.1982,
    speedLimitKmh: 50,
    radiusMeters: 1100,
    description: "Automated radar enforcing urban speed along Ring Road Central near Nima junction."
  },
  {
    id: "teshie_link",
    name: "Teshie Link Corridor",
    roadName: "Teshie Link",
    latitude: 5.5905,
    longitude: -0.0982,
    speedLimitKmh: 50,
    radiusMeters: 1200,
    description: "Speed enforcement camera along Teshie Link connecting Spintex to Beach Road."
  },
  {
    id: "laterbiokorshie_star_oil",
    name: "Laterbiokorshie (Near Star Oil)",
    roadName: "Link Road / Laterbiokorshie",
    latitude: 5.5502,
    longitude: -0.2335,
    speedLimitKmh: 50,
    radiusMeters: 1000,
    description: "Automated camera enforcing 50 km/h urban speed limit near Star Oil."
  },
  {
    id: "n1_densu_weija",
    name: "N1 Highway (Densu River Bridge / Weija)",
    roadName: "Accra - Cape Coast Highway (N1)",
    latitude: 5.5552,
    longitude: -0.3364,
    speedLimitKmh: 80,
    radiusMeters: 1500,
    description: "High-speed radar camera on the N1 corridor at the Densu River bridge near Weija."
  },
  // Northern Stretch & N6 Highway (Accra West to North)
  {
    id: "apenkwa_achimota_overhead",
    name: "Apenkwa / Achimota Overhead",
    roadName: "George Walker Bush Highway (N1)",
    latitude: 5.6145,
    longitude: -0.2225,
    speedLimitKmh: 80,
    radiusMeters: 1400,
    description: "Automated camera monitoring N1 flow at Apenkwa and Achimota interchange."
  },
  {
    id: "abelenkpe_main",
    name: "Abelenkpe Main Road",
    roadName: "Abelenkpe Road",
    latitude: 5.6012,
    longitude: -0.2085,
    speedLimitKmh: 50,
    radiusMeters: 1000,
    description: "Urban speed camera on the Abelenkpe - Roman Ridge transit road."
  },
  {
    id: "ofankor_taifa_barrier",
    name: "Ofankor Barrier / Taifa (N6)",
    roadName: "Accra - Nsawam Highway (N6)",
    latitude: 5.6565,
    longitude: -0.2582,
    speedLimitKmh: 80,
    radiusMeters: 1500,
    description: "Radar camera monitoring northbound N6 traffic at the Ofankor Barrier / Taifa junction."
  },
  {
    id: "sowutuom_christian_village",
    name: "Sowutuom (Pillar 2 / Anyaa Link)",
    roadName: "Sowutuom Road",
    latitude: 5.6162,
    longitude: -0.2692,
    speedLimitKmh: 50,
    radiusMeters: 1100,
    description: "Automated speed enforcement camera along the Sowutuom - Pillar 2 corridor."
  },
  {
    id: "haatso_atomic_madina",
    name: "Haatso - Atomic - Madina Road",
    roadName: "Atomic - Haatso Road",
    latitude: 5.6705,
    longitude: -0.1872,
    speedLimitKmh: 50,
    radiusMeters: 1300,
    description: "Automated camera monitoring the Haatso, Papao, and Madina Zongo transit stretch."
  },
  // East Legon & Adentan Cluster
  {
    id: "east_legon_shiashie",
    name: "East Legon (Shiashie / Lagos Ave)",
    roadName: "Lagos Avenue",
    latitude: 5.6265,
    longitude: -0.1772,
    speedLimitKmh: 50,
    radiusMeters: 1100,
    description: "Camera monitoring the Shiashie entrance into East Legon along Lagos Avenue."
  },
  {
    id: "east_legon_bawaleshie",
    name: "Bawaleshie / American House (East Legon)",
    roadName: "Boundary Road",
    latitude: 5.6372,
    longitude: -0.1582,
    speedLimitKmh: 50,
    radiusMeters: 1100,
    description: "Speed camera on Boundary Road near Bawaleshie and American House junction."
  },
  {
    id: "adjiringanor_ogbojo",
    name: "Adjiringanor - Ogbojo Link",
    roadName: "Adjiringanor Road",
    latitude: 5.6482,
    longitude: -0.1385,
    speedLimitKmh: 50,
    radiusMeters: 1100,
    description: "Speed camera monitoring the Adjiringanor and Ogbojo residential arterial."
  },
  {
    id: "east_legon_madina_30kmh",
    name: "East Legon - Madina Road (30 km/h School Zone)",
    roadName: "East Legon - Madina Link Road",
    latitude: 5.6582,
    longitude: -0.1552,
    speedLimitKmh: 30,
    radiusMeters: 1000,
    description: "Strict 30 km/h automated speed zone in the school and pedestrian crossing zone."
  },
  // Kumasi & Ashanti Region (N6 Corridor & Metro)
  {
    id: "kumasi_n6_bomso_ayigya",
    name: "Kumasi N6 (Bomso / Ayigya / KNUST Gate)",
    roadName: "Kumasi - Accra Highway (N6)",
    latitude: 6.6852,
    longitude: -1.5832,
    speedLimitKmh: 50,
    radiusMeters: 1300,
    description: "Automated speed enforcement near KNUST Main Gate, Bomso, and Ayigya on N6."
  },
  {
    id: "kumasi_n6_kentinkrono",
    name: "Kumasi N6 (Kentinkrono / Nsenie)",
    roadName: "Kumasi - Accra Highway (N6)",
    latitude: 6.6972,
    longitude: -1.5542,
    speedLimitKmh: 60,
    radiusMeters: 1400,
    description: "High-speed camera monitoring eastern entry into Kumasi at Kentinkrono and Nsenie."
  },
  {
    id: "kumasi_anwiankwanta",
    name: "Anwiankwanta Junction (Kumasi - Obuasi)",
    roadName: "Kumasi - Obuasi Highway (N10)",
    latitude: 6.4782,
    longitude: -1.6372,
    speedLimitKmh: 80,
    radiusMeters: 1500,
    description: "Highway radar camera monitoring approach to Anwiankwanta junction."
  },
  {
    id: "kumasi_asokwa_mall",
    name: "Kumasi Asokwa (City Mall / Bypass)",
    roadName: "24th February Road / Asokwa",
    latitude: 6.6712,
    longitude: -1.6062,
    speedLimitKmh: 50,
    radiusMeters: 1200,
    description: "Automated urban speed camera near Kumasi City Mall on the Asokwa bypass."
  },
  {
    id: "kumasi_adukrom_asokore_mampong",
    name: "Kumasi (Adukrom / Asokore Mampong)",
    roadName: "Asokore Mampong Arterial",
    latitude: 6.7022,
    longitude: -1.5952,
    speedLimitKmh: 50,
    radiusMeters: 1200,
    description: "Speed camera monitoring side routes off N6 towards Asokore Mampong and Adukrom."
  },
  {
    id: "kumasi_knust_ayeduase",
    name: "KNUST Ayeduase Gate",
    roadName: "Ayeduase Road",
    latitude: 6.6752,
    longitude: -1.5652,
    speedLimitKmh: 30,
    radiusMeters: 900,
    description: "Campus and hostel residential zone camera enforcing 30 km/h speed limit.",
    isMobilePatrol: true
  },
  {
    id: "kumasi_boankra_trap",
    name: "Boankra N6 Highway (Mobile Speed Trap)",
    roadName: "Accra - Kumasi Highway (N6)",
    latitude: 6.6385,
    longitude: -1.4180,
    speedLimitKmh: 50,
    radiusMeters: 1500,
    description: "Frequent rotating police mobile speedcam setup where highway limit unpredictably drops to 50 km/h near Boankra.",
    isMobilePatrol: true
  }
];

// 1b. Ghana Speed Limit Zones (roads WITHOUT cameras — road-aware speed detection)
const SPEED_LIMIT_ZONES = [
  // === Major Accra Highways ===
  // Tema Motorway (3 segments)
  { id: "tema_motorway_west", name: "Tema Motorway (Tetteh Quarshie – Interchange)", latitude: 5.6250, longitude: -0.1450, speedLimitKmh: 100, radiusMeters: 2500 },
  { id: "tema_motorway_mid", name: "Tema Motorway (Afienya Corridor)", latitude: 5.6400, longitude: -0.0800, speedLimitKmh: 100, radiusMeters: 2500 },
  { id: "tema_motorway_east", name: "Tema Motorway (Tema Interchange)", latitude: 5.6600, longitude: -0.0200, speedLimitKmh: 100, radiusMeters: 2500 },

  // George Walker Bush Highway N1 segments
  { id: "n1_pokuase", name: "N1 Highway (Pokuase)", latitude: 5.6800, longitude: -0.3000, speedLimitKmh: 80, radiusMeters: 1500 },
  { id: "n1_mallam", name: "N1 Highway (Mallam Junction)", latitude: 5.5650, longitude: -0.2800, speedLimitKmh: 80, radiusMeters: 1500 },
  { id: "n1_santa_maria", name: "N1 Highway (Santa Maria)", latitude: 5.5980, longitude: -0.2300, speedLimitKmh: 80, radiusMeters: 1500 },
  { id: "n1_tetteh_quarshie", name: "N1 Highway (Tetteh Quarshie)", latitude: 5.6150, longitude: -0.1550, speedLimitKmh: 80, radiusMeters: 1500 },

  // Airport Bypass Road (2 segments)
  { id: "airport_bypass_north", name: "Airport Bypass Road (North)", latitude: 5.6050, longitude: -0.1700, speedLimitKmh: 80, radiusMeters: 1500 },
  { id: "airport_bypass_south", name: "Airport Bypass Road (South)", latitude: 5.5920, longitude: -0.1650, speedLimitKmh: 80, radiusMeters: 1500 },

  // === Accra Urban 50 km/h Roads ===
  { id: "independence_ave", name: "Independence Avenue", latitude: 5.5560, longitude: -0.1970, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "liberation_rd_west", name: "Liberation Road (West)", latitude: 5.5830, longitude: -0.1900, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "liberation_rd_east", name: "Liberation Road (East)", latitude: 5.5870, longitude: -0.1750, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "ring_road_east", name: "Ring Road East", latitude: 5.5700, longitude: -0.1850, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "ring_road_west", name: "Ring Road West", latitude: 5.5650, longitude: -0.2200, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "ring_road_south", name: "Ring Road South", latitude: 5.5500, longitude: -0.2100, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "graphic_road", name: "Graphic Road", latitude: 5.5530, longitude: -0.2180, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "abossey_okai", name: "Abossey Okai Road", latitude: 5.5480, longitude: -0.2280, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "winneba_road", name: "Winneba Road", latitude: 5.5400, longitude: -0.2350, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kaneshie_odorkor", name: "Kaneshie-Odorkor Road", latitude: 5.5700, longitude: -0.2500, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "dansoman_high_st", name: "Dansoman High Street", latitude: 5.5350, longitude: -0.2550, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "spintex_rd_west", name: "Spintex Road (West)", latitude: 5.6350, longitude: -0.1200, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "spintex_rd_mid", name: "Spintex Road (Central)", latitude: 5.6400, longitude: -0.1000, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "spintex_rd_east", name: "Spintex Road (East)", latitude: 5.6450, longitude: -0.0800, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "oxford_st_osu", name: "Oxford Street Osu", latitude: 5.5560, longitude: -0.1780, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "labone", name: "Labone", latitude: 5.5650, longitude: -0.1820, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "dzorwulu", name: "Dzorwulu", latitude: 5.5980, longitude: -0.2050, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "north_dzorwulu", name: "North Dzorwulu", latitude: 5.6050, longitude: -0.2080, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "east_legon_main", name: "East Legon Main Road", latitude: 5.6350, longitude: -0.1600, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "madina_accra_hwy", name: "Madina-Accra Highway", latitude: 5.6600, longitude: -0.1700, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "adenta_dodowa_rd", name: "Adenta-Dodowa Road", latitude: 5.6900, longitude: -0.1550, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "tema_community_1", name: "Tema Community 1", latitude: 5.6650, longitude: -0.0050, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "tema_harbour_rd", name: "Tema Harbour Road", latitude: 5.6350, longitude: 0.0050, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "beach_rd_labadi_nungua", name: "Beach Road (Labadi/Nungua)", latitude: 5.5500, longitude: -0.1300, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "sakumono", name: "Sakumono", latitude: 5.6200, longitude: -0.0400, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kasoa_main_rd", name: "Kasoa Main Road", latitude: 5.5350, longitude: -0.4200, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "dodowa_rd_madina", name: "Dodowa Road Madina", latitude: 5.6750, longitude: -0.1650, speedLimitKmh: 50, radiusMeters: 1000 },

  // === School Zones (30 km/h) ===
  { id: "achimota_school_zone", name: "Achimota School Zone", latitude: 5.6200, longitude: -0.2300, speedLimitKmh: 30, radiusMeters: 800 },
  { id: "uog_campus", name: "University of Ghana Campus", latitude: 5.6500, longitude: -0.1850, speedLimitKmh: 30, radiusMeters: 800 },
  { id: "presec_legon", name: "Presec Legon", latitude: 5.6550, longitude: -0.1750, speedLimitKmh: 30, radiusMeters: 800 },

  // === N1/N6 Highway Segments ===
  { id: "n1_kasoa_bawjiase", name: "N1 Kasoa-Bawjiase", latitude: 5.4800, longitude: -0.4500, speedLimitKmh: 80, radiusMeters: 2000 },
  { id: "n1_winneba_jct", name: "N1 Winneba Junction", latitude: 5.3500, longitude: -0.5500, speedLimitKmh: 80, radiusMeters: 2000 },

  // N6 town segments (township limits 50 km/h)
  { id: "n6_nsawam_town", name: "N6 Nsawam Township", latitude: 5.8100, longitude: -0.3500, speedLimitKmh: 50, radiusMeters: 1200 },
  { id: "n6_suhum_town", name: "N6 Suhum Township", latitude: 6.0400, longitude: -0.4500, speedLimitKmh: 50, radiusMeters: 1200 },
  { id: "n6_nkawkaw_town", name: "N6 Nkawkaw Township", latitude: 6.5500, longitude: -0.7800, speedLimitKmh: 50, radiusMeters: 1200 },
  { id: "n6_konongo_town", name: "N6 Konongo Township", latitude: 6.6200, longitude: -1.2200, speedLimitKmh: 50, radiusMeters: 1200 },
  { id: "n6_ejisu_town", name: "N6 Ejisu Township", latitude: 6.6900, longitude: -1.4600, speedLimitKmh: 50, radiusMeters: 1200 },

  // N6 highway segments between towns (100 km/h)
  { id: "n6_nsawam_suhum", name: "N6 Highway (Nsawam – Suhum)", latitude: 5.9200, longitude: -0.4000, speedLimitKmh: 100, radiusMeters: 2500 },
  { id: "n6_suhum_nkawkaw", name: "N6 Highway (Suhum – Nkawkaw)", latitude: 6.3000, longitude: -0.6200, speedLimitKmh: 100, radiusMeters: 2500 },
  { id: "n6_nkawkaw_konongo", name: "N6 Highway (Nkawkaw – Konongo)", latitude: 6.5800, longitude: -1.0000, speedLimitKmh: 100, radiusMeters: 2500 },
  { id: "n6_konongo_ejisu", name: "N6 Highway (Konongo – Ejisu)", latitude: 6.6500, longitude: -1.3400, speedLimitKmh: 100, radiusMeters: 2500 },

  // === Kumasi Urban Roads (50 km/h) ===
  { id: "kumasi_kejetia", name: "Kejetia", latitude: 6.6900, longitude: -1.6250, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kumasi_adum", name: "Adum / Prempeh II Street", latitude: 6.6870, longitude: -1.6220, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kumasi_harper_rd", name: "Harper Road", latitude: 6.6950, longitude: -1.6180, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kumasi_lake_rd", name: "Lake Road", latitude: 6.7000, longitude: -1.6300, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kumasi_suame_magazine", name: "Suame Magazine", latitude: 6.7100, longitude: -1.6200, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kumasi_ahinsan_bypass", name: "Ahinsan-Atonsu Bypass", latitude: 6.6600, longitude: -1.6350, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "kumasi_bantama_rd", name: "Bantama Road", latitude: 6.6980, longitude: -1.6400, speedLimitKmh: 50, radiusMeters: 1000 },

  // Kumasi School Zone
  { id: "knust_campus", name: "KNUST Campus Internal Roads", latitude: 6.6730, longitude: -1.5700, speedLimitKmh: 30, radiusMeters: 800 },

  // Kumasi Highways
  { id: "n10_obuasi_hwy", name: "N10 Obuasi Highway", latitude: 6.5500, longitude: -1.6500, speedLimitKmh: 80, radiusMeters: 2000 },
  { id: "n10_mampong_hwy", name: "N10 Mampong Highway", latitude: 6.8000, longitude: -1.5500, speedLimitKmh: 80, radiusMeters: 2000 },

  // === Other Cities (50 km/h) ===
  { id: "takoradi_market_circle", name: "Takoradi Market Circle", latitude: 4.8950, longitude: -1.7600, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "cape_coast_castle_rd", name: "Cape Coast Castle Road", latitude: 5.1050, longitude: -1.2450, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "tamale_bolga_rd", name: "Tamale-Bolgatanga Road", latitude: 9.4100, longitude: -0.8400, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "ho_main_rd", name: "Ho Main Road", latitude: 6.6000, longitude: 0.4700, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "sunyani_ring_rd", name: "Sunyani Ring Road", latitude: 7.3400, longitude: -2.3300, speedLimitKmh: 50, radiusMeters: 1000 },
  { id: "koforidua_main_rd", name: "Koforidua Main Road", latitude: 6.0900, longitude: -0.2600, speedLimitKmh: 50, radiusMeters: 1000 },

  // Tamale Bypass
  { id: "tamale_bypass", name: "Tamale Bypass", latitude: 9.4300, longitude: -0.8100, speedLimitKmh: 80, radiusMeters: 2000 }
];

// 2. Application State
const state = {
  isMonitoring: false,
  hasLocationPermission: localStorage.getItem('sc_has_location_perm') === 'true',
  currentSpeedKmh: 0,
  smoothedSpeedKmh: 0,
  manualLimitKmh: parseInt(localStorage.getItem('sc_manual_limit') || '80', 10),
  autoDetectEnabled: localStorage.getItem('sc_auto_detect') !== 'false',
  alertMode: localStorage.getItem('sc_alert_mode') || (localStorage.getItem('sc_voice_alerts') === 'false' ? 'MUTE' : 'SOUND'),
  voiceAlertsEnabled: localStorage.getItem('sc_voice_alerts') !== 'false',
  voiceGender: localStorage.getItem('sc_voice_gender') || 'FEMALE',
  alertCooldownSeconds: parseInt(localStorage.getItem('sc_cooldown') || '15', 10),
  approachingOffsetKmh: 5,
  isDarkMode: localStorage.getItem('sc_theme') !== 'light',

  // Motion Detection (Normal walking/stationary vs Vehicle movement)
  motionState: 'STOPPED', // 'STOPPED', 'WAITING_FOR_MOVEMENT', 'DRIVING'
  consecutiveDrivingReadings: 0,
  stationarySeconds: 0,

  // Live Location & Radar
  lastPosition: null,
  activeZone: null,
  activeSpeedLimitZone: null,
  hasCameraAhead: false,
  distanceToCameraMeters: null,
  isMovingTowardsCamera: false,
  effectiveLimitKmh: 80,
  lastAnnouncedCameraZoneId: null,

  // Alert Timers
  lastExceededAlertTime: 0,
  hasApproachingFiredInZone: false,

  // Trip Stats
  trip: {
    maxSpeedKmh: 0,
    speedSum: 0,
    speedReadingsCount: 0,
    distanceKm: 0,
    startTime: null
  },

  // Geolocation & WakeLock handles
  watchId: null,
  wakeLock: null
};

// 3. Mathematical Geolocation Utilities
function toRadians(deg) {
  return (deg * Math.PI) / 180;
}

function calculateDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371e3; // Earth radius in meters
  const phi1 = toRadians(lat1);
  const phi2 = toRadians(lat2);
  const deltaPhi = toRadians(lat2 - lat1);
  const deltaLambda = toRadians(lon2 - lon1);

  const a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
            Math.cos(phi1) * Math.cos(phi2) *
            Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function calculateBearing(lat1, lon1, lat2, lon2) {
  const y = Math.sin(toRadians(lon2 - lon1)) * Math.cos(toRadians(lat2));
  const x = Math.cos(toRadians(lat1)) * Math.sin(toRadians(lat2)) -
            Math.sin(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.cos(toRadians(lon2 - lon1));
  const brng = (Math.atan2(y, x) * 180) / Math.PI;
  return (brng + 360) % 360;
}

// 4. Camera & Road Speed Limit Detection Engine (Dual-Layer)
function evaluateCameraZones(currentLat, currentLon, speedKmh, heading) {
  if (!state.autoDetectEnabled) {
    state.activeZone = null;
    state.activeSpeedLimitZone = null;
    state.hasCameraAhead = false;
    state.distanceToCameraMeters = null;
    state.detectedRoadName = null;
    state.effectiveLimitKmh = state.manualLimitKmh;
    return;
  }

  // --- Layer 1: Check Speed Camera Zones (Highest Priority) ---
  let closestCamera = null;
  let minCameraDist = Infinity;
  let isMovingTowards = false;

  for (const zone of GHANA_SPEED_CAMERAS) {
    const dist = calculateDistanceMeters(currentLat, currentLon, zone.latitude, zone.longitude);
    if (dist <= zone.radiusMeters) {
      const bearingToCamera = calculateBearing(currentLat, currentLon, zone.latitude, zone.longitude);
      let movingTowards = true;

      if (heading != null && !isNaN(heading) && speedKmh >= 10) {
        const angleDiff = Math.abs((heading - bearingToCamera + 540) % 360 - 180);
        movingTowards = angleDiff <= 85 || dist <= 350;
      }

      if (movingTowards && dist < minCameraDist) {
        minCameraDist = dist;
        closestCamera = zone;
        isMovingTowards = true;
      }
    }
  }

  if (closestCamera) {
    state.activeZone = closestCamera;
    state.activeSpeedLimitZone = null;
    state.hasCameraAhead = true;
    state.distanceToCameraMeters = minCameraDist;
    state.isMovingTowardsCamera = isMovingTowards;
    state.detectedRoadName = closestCamera.roadName || closestCamera.name;
    state.effectiveLimitKmh = closestCamera.speedLimitKmh;
    return;
  }

  // --- Layer 2: Check Speed Limit Zones (Named Roads, No Cameras) ---
  let closestLimitZone = null;
  let minLimitDist = Infinity;

  for (const zone of SPEED_LIMIT_ZONES) {
    const dist = calculateDistanceMeters(currentLat, currentLon, zone.latitude, zone.longitude);
    if (dist <= zone.radiusMeters && dist < minLimitDist) {
      minLimitDist = dist;
      closestLimitZone = zone;
    }
  }

  if (closestLimitZone) {
    state.activeZone = null;
    state.activeSpeedLimitZone = closestLimitZone;
    state.hasCameraAhead = false;
    state.distanceToCameraMeters = null;
    state.isMovingTowardsCamera = false;
    state.detectedRoadName = closestLimitZone.name;
    state.lastAnnouncedCameraZoneId = null;
    state.effectiveLimitKmh = closestLimitZone.speedLimitKmh;
    return;
  }

  // --- Layer 3: Fallback Ghana Standard Limit ---
  state.activeZone = null;
  state.activeSpeedLimitZone = null;
  state.hasCameraAhead = false;
  state.distanceToCameraMeters = null;
  state.isMovingTowardsCamera = false;
  state.detectedRoadName = null;
  state.lastAnnouncedCameraZoneId = null;
  state.effectiveLimitKmh = speedKmh >= 65 ? 80 : 50;
}

// 5. Text-To-Speech (Web Speech API) Engine
let availableVoices = [];

function loadVoices() {
  if ('speechSynthesis' in window) {
    availableVoices = window.speechSynthesis.getVoices();
  }
}

if ('speechSynthesis' in window) {
  window.speechSynthesis.onvoiceschanged = loadVoices;
  loadVoices();
}

function findBestVoice() {
  if (!availableVoices.length) loadVoices();
  const isMale = state.voiceGender === 'MALE';
  const targetKeywords = isMale ? ['male', 'daniel', 'oliver', 'rjs'] : ['female', 'samantha', 'karen', 'victoria', 'fis'];
  const opposingKeywords = isMale ? ['female', 'samantha', 'karen'] : ['male', 'daniel'];

  const englishVoices = availableVoices.filter(v => v.lang.startsWith('en'));
  const match = englishVoices.find(v => {
    const name = v.name.toLowerCase();
    return targetKeywords.some(kw => name.includes(kw)) && !opposingKeywords.some(kw => name.includes(kw));
  });

  return match || englishVoices[0] || null;
}

function speakVoice(text) {
  if (!state.voiceAlertsEnabled || !('speechSynthesis' in window)) return;
  try {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    const voice = findBestVoice();
    if (voice) utterance.voice = voice;
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    utterance.volume = 1.0;
    window.speechSynthesis.speak(utterance);
  } catch (e) {
    console.warn("Speech synthesis error:", e);
  }
}

// Play silent chime on user interaction to unlock Safari Web Audio
function unlockAudioContext() {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    gain.gain.value = 0.001; // silent
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.05);
  } catch (e) {}
}

function triggerAlert(message) {
  if (state.alertMode === 'SOUND') {
    speakVoice(message);
  } else if (state.alertMode === 'VIBRATE') {
    // 4 bursts across 4 seconds: [buzz 250ms, silence 750ms x 4]
    if ('vibrate' in navigator) {
      navigator.vibrate([250, 750, 250, 750, 250, 750, 250]);
    }
  }
}

// 6. Unified Alert Logic (Sound / Vibrate / Mute)
function processSpeedAlerts(speedKmh, limitKmh) {
  if (state.alertMode === 'MUTE' || !state.isMonitoring) return;
  if (speedKmh < 8.0) return; // Suppress alerts when stationary or walking

  const now = Date.now();
  const cooldownMs = state.alertCooldownSeconds * 1000;
  const approachingThreshold = Math.max(1, limitKmh - state.approachingOffsetKmh);
  const isApproachingOrExceeded = speedKmh >= approachingThreshold;

  const hasCameraAhead = state.hasCameraAhead;
  const cameraZoneId = hasCameraAhead && state.activeZone ? state.activeZone.id : null;

  if (hasCameraAhead && cameraZoneId) {
    const isMobile = state.activeZone && state.activeZone.isMobilePatrol;
    const safeMsg = isMobile ? "Potential police speed camera ahead, be careful." : "Speed cameras ahead, be careful.";
    const warnMsg = isMobile ? "Potential police speed camera ahead, slow down." : "Speed cameras ahead, slow down.";

    if (isApproachingOrExceeded) {
      if ((now - (state.lastAlertTime || 0) >= cooldownMs) || cameraZoneId !== state.lastAnnouncedCameraZoneId) {
        triggerAlert(warnMsg);
        state.lastAlertTime = now;
        state.lastAnnouncedCameraZoneId = cameraZoneId;
        state.hasApproachingFired = true;
      }
    } else {
      if (cameraZoneId !== state.lastAnnouncedCameraZoneId) {
        triggerAlert(safeMsg);
        state.lastAnnouncedCameraZoneId = cameraZoneId;
        state.lastAlertTime = now;
      }
    }
  } else {
    if (!state.activeZone) {
      state.lastAnnouncedCameraZoneId = null;
    }

    if (isApproachingOrExceeded) {
      if (!state.hasApproachingFired || (now - (state.lastAlertTime || 0) >= cooldownMs)) {
        triggerAlert("You are about to cross the speed limit, slow down.");
        state.lastAlertTime = now;
        state.hasApproachingFired = true;
      }
    } else if (speedKmh < approachingThreshold - 2) {
      state.hasApproachingFired = false;
    }
  }
}

// 7. Screen WakeLock (prevents screen lock while driving)
async function requestWakeLock() {
  try {
    if ('wakeLock' in navigator) {
      state.wakeLock = await navigator.wakeLock.request('screen');
      state.wakeLock.addEventListener('release', () => {
        state.wakeLock = null;
      });
    }
  } catch (err) {
    console.warn("Screen WakeLock failed:", err);
  }
}

function releaseWakeLock() {
  if (state.wakeLock) {
    state.wakeLock.release().catch(() => {});
    state.wakeLock = null;
  }
}

document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && state.isMonitoring) {
    requestWakeLock();
  }
});

// 8. Jitter-Resistant GPS Speed & Vehicle Motion Evaluation
function handlePositionUpdate(position) {
  const coords = position.coords;

  // Filter 1: Discard wildly inaccurate GPS readings (accuracy > 35 meters)
  // This prevents cell-tower triangulation jumps from causing 100+ km/h spikes while stationary
  if (coords.accuracy && coords.accuracy > 35) {
    console.warn(`GPS accuracy low (${Math.round(coords.accuracy)}m) - filtering position`);
    if (state.lastPosition === null) {
      state.lastPosition = position;
    }
    return;
  }

  let rawCalculatedSpeed = 0;
  const now = position.timestamp || Date.now();

  // Priority 1: Hardware Doppler GPS Speed from device chip
  if (coords.speed !== null && !isNaN(coords.speed) && coords.speed >= 0) {
    rawCalculatedSpeed = coords.speed * 3.6;
  } else if (state.lastPosition) {
    // Priority 2: Fallback delta-distance calculation with safeguards
    const prev = state.lastPosition;
    const prevTime = prev.timestamp || now;
    const deltaSeconds = (now - prevTime) / 1000;

    // Reject division by tiny time intervals (< 1.0 second) to prevent math spikes
    if (deltaSeconds >= 1.0) {
      const distMeters = calculateDistanceMeters(
        prev.coords.latitude,
        prev.coords.longitude,
        coords.latitude,
        coords.longitude
      );

      // Ignore micro-drift under 3.5 meters (natural GPS wobble)
      if (distMeters >= 3.5) {
        const unconstrainedSpeed = (distMeters / deltaSeconds) * 3.6;

        // Physical Acceleration Sanity Check:
        // A normal vehicle cannot accelerate faster than ~30 km/h per second.
        // Clamping prevents GPS teleport jumps from registering as 135 km/h.
        const maxPermittedSpeed = state.currentSpeedKmh + (30 * deltaSeconds);
        rawCalculatedSpeed = Math.min(unconstrainedSpeed, maxPermittedSpeed);
      }
    } else {
      // Time delta too small, carry forward current speed without sudden spikes
      rawCalculatedSpeed = state.currentSpeedKmh;
    }
  }

  // Filter 2: Deadband for Stationary / Walking
  // Speeds under 8 km/h are considered stationary (eliminates walking/pocket wobble)
  if (rawCalculatedSpeed < 8.0) {
    rawCalculatedSpeed = 0;
  }

  // Filter 3: Exponential Moving Average Smoothing
  // Produces a smooth gauge needle and removes jitter
  if (rawCalculatedSpeed === 0) {
    state.smoothedSpeedKmh = 0;
  } else if (state.smoothedSpeedKmh === 0) {
    state.smoothedSpeedKmh = rawCalculatedSpeed;
  } else {
    state.smoothedSpeedKmh = (0.45 * rawCalculatedSpeed) + (0.55 * state.smoothedSpeedKmh);
  }

  state.currentSpeedKmh = Math.max(0, state.smoothedSpeedKmh);

  // Motion State & Vehicle Detection:
  // Requires consistent speed >= 18 km/h to confirm vehicle driving
  if (state.currentSpeedKmh >= 18) {
    state.consecutiveDrivingReadings++;
    state.stationarySeconds = 0;
    if (state.consecutiveDrivingReadings >= 2) {
      state.motionState = 'DRIVING';
    }
  } else if (state.currentSpeedKmh >= 6) {
    state.motionState = 'MANEUVERING';
  } else {
    state.consecutiveDrivingReadings = 0;
    state.motionState = 'STOPPED';
  }

  // Update Trip Stats only during genuine driving movement
  if (state.lastPosition && state.currentSpeedKmh >= 6) {
    const dKm = calculateDistanceMeters(
      state.lastPosition.coords.latitude,
      state.lastPosition.coords.longitude,
      coords.latitude,
      coords.longitude
    ) / 1000;
    state.trip.distanceKm += dKm;
  }

  if (state.currentSpeedKmh > state.trip.maxSpeedKmh) {
    state.trip.maxSpeedKmh = state.currentSpeedKmh;
  }

  if (state.currentSpeedKmh >= 6) {
    state.trip.speedSum += state.currentSpeedKmh;
    state.trip.speedReadingsCount++;
  }

  // Camera Zone Evaluation
  evaluateCameraZones(coords.latitude, coords.longitude, state.currentSpeedKmh, coords.heading);

  // Voice Alert Evaluation
  processSpeedAlerts(state.currentSpeedKmh, state.effectiveLimitKmh);

  state.lastPosition = position;
  updateUI();
}

function handlePositionError(error) {
  console.warn("Geolocation error:", error);
  const statusEl = document.getElementById('statusText');

  if (error.code === 1) { // PERMISSION_DENIED
    if (statusEl) statusEl.textContent = "Location Denied";
    state.hasLocationPermission = false;
    localStorage.setItem('sc_has_location_perm', 'false');

    // Show troubleshooting instructions
    const deniedCard = document.getElementById('permissionDeniedCard');
    if (deniedCard) deniedCard.style.display = 'block';
    openModal('permissionModal');
  } else {
    if (statusEl) statusEl.textContent = "Acquiring GPS Signal...";
  }
}

// 9. Interactive Permission Flow & Monitoring Control
function requestLocationAndStart() {
  unlockAudioContext();

  if (!navigator.geolocation) {
    alert("Geolocation is not supported by your browser.");
    return;
  }

  // Request high-accuracy initial fix to trigger browser permission dialog
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      // Permission granted!
      state.hasLocationPermission = true;
      localStorage.setItem('sc_has_location_perm', 'true');
      closeModal('permissionModal');
      const deniedCard = document.getElementById('permissionDeniedCard');
      if (deniedCard) deniedCard.style.display = 'none';

      actuallyStartMonitoring();
    },
    (err) => {
      handlePositionError(err);
    },
    {
      enableHighAccuracy: true,
      maximumAge: 0,
      timeout: 12000
    }
  );
}

function startMonitoring() {
  unlockAudioContext();

  // If permission has never been granted or was blocked, open explanatory dialog first
  if (!state.hasLocationPermission) {
    const deniedCard = document.getElementById('permissionDeniedCard');
    if (deniedCard) deniedCard.style.display = 'none';
    openModal('permissionModal');
    return;
  }

  actuallyStartMonitoring();
}

function actuallyStartMonitoring() {
  requestWakeLock();

  state.isMonitoring = true;
  state.trip.startTime = Date.now();
  state.trip.maxSpeedKmh = 0;
  state.trip.speedSum = 0;
  state.trip.speedReadingsCount = 0;
  state.trip.distanceKm = 0;
  state.currentSpeedKmh = 0;
  state.smoothedSpeedKmh = 0;
  state.motionState = 'STOPPED';

  if ('geolocation' in navigator) {
    state.watchId = navigator.geolocation.watchPosition(
      handlePositionUpdate,
      handlePositionError,
      {
        enableHighAccuracy: true,
        maximumAge: 0,
        timeout: 10000
      }
    );
  }

  speakVoice("SpeedCheck active.");
  updateUI();
}

function stopMonitoring() {
  if (state.watchId !== null) {
    navigator.geolocation.clearWatch(state.watchId);
    state.watchId = null;
  }
  releaseWakeLock();

  state.isMonitoring = false;
  state.currentSpeedKmh = 0;
  state.smoothedSpeedKmh = 0;
  state.activeZone = null;
  state.distanceToCameraMeters = null;
  state.lastPosition = null;
  state.motionState = 'STOPPED';

  updateUI();
}

// 10. UI Rendering & Gauge Control
function updateUI() {
  const speed = Math.round(state.currentSpeedKmh);
  const limit = state.effectiveLimitKmh;

  // Digital Number
  const speedNumberEl = document.getElementById('speedNumber');
  if (speedNumberEl) speedNumberEl.textContent = speed;

  // Motion State Tag
  const motionTagEl = document.getElementById('speedMotionTag');
  if (motionTagEl) {
    if (!state.isMonitoring) {
      motionTagEl.textContent = "STOPPED";
      motionTagEl.className = "speed-motion-tag";
    } else if (state.motionState === 'DRIVING') {
      motionTagEl.textContent = "VEHICLE DRIVING";
      motionTagEl.className = "speed-motion-tag driving";
    } else if (state.motionState === 'MANEUVERING') {
      motionTagEl.textContent = "MANEUVERING";
      motionTagEl.className = "speed-motion-tag";
    } else {
      motionTagEl.textContent = "VEHICLE STOPPED";
      motionTagEl.className = "speed-motion-tag";
    }
  }

  // Gauge Arc Progress (stroke-dasharray = 330)
  const maxDisplaySpeed = 160;
  const ratio = Math.min(1, speed / maxDisplaySpeed);
  const offset = 330 - (ratio * 330);
  const gaugeEl = document.getElementById('gaugeProgress');

  // Status and Gauge Color States
  const badgeEl = document.getElementById('statusBadge');
  const badgeText = document.getElementById('statusText');

  let alertState = 'normal';
  const approachingThreshold = Math.max(1, limit - state.approachingOffsetKmh);

  if (speed >= limit && state.isMonitoring) {
    alertState = 'exceeded';
  } else if (speed >= approachingThreshold && state.isMonitoring) {
    alertState = 'approaching';
  }

  if (gaugeEl) {
    gaugeEl.style.strokeDashoffset = offset;
    if (alertState === 'exceeded') {
      gaugeEl.style.stroke = 'var(--exceeded-red)';
      if (speedNumberEl) speedNumberEl.style.color = 'var(--exceeded-red)';
    } else if (alertState === 'approaching') {
      gaugeEl.style.stroke = 'var(--approaching-amber)';
      if (speedNumberEl) speedNumberEl.style.color = 'var(--approaching-amber)';
    } else {
      gaugeEl.style.stroke = 'var(--safe-green)';
      if (speedNumberEl) speedNumberEl.style.color = 'var(--text-primary)';
    }
  }

  // Status Badge UI
  if (badgeEl && badgeText) {
    badgeEl.className = 'status-badge';
    if (!state.isMonitoring) {
      badgeText.textContent = "Monitoring Stopped";
    } else if (alertState === 'exceeded') {
      badgeEl.classList.add('exceeded');
      badgeText.textContent = `Over Limit (${speed} km/h)!`;
    } else if (alertState === 'approaching') {
      badgeEl.classList.add('approaching');
      badgeText.textContent = `Approaching Limit (${speed} km/h)`;
    } else {
      badgeEl.classList.add('active');
      badgeText.textContent = state.motionState === 'DRIVING' ? "Monitoring Active (Driving)" : "Monitoring Active";
    }
  }

  // Effective Limit Pill
  const limitPillEl = document.getElementById('effectiveLimitNumber');
  if (limitPillEl) limitPillEl.textContent = limit;

  // Radar Card
  const radarCard = document.getElementById('radarCard');
  const radarStatus = document.getElementById('radarStatusText');
  const autoDetectToggle = document.getElementById('autoDetectToggle');

  if (autoDetectToggle) autoDetectToggle.checked = state.autoDetectEnabled;

  if (radarCard && radarStatus) {
    if (state.autoDetectEnabled) {
      radarCard.classList.add('auto-active');
      if (state.activeZone) {
        const d = Math.round(state.distanceToCameraMeters);
        const prefix = state.activeZone.isMobilePatrol ? "⚠️ Potential Police Speedcam:" : "📷 Camera Ahead:";
        radarStatus.innerHTML = `<strong>${prefix}</strong> ${state.activeZone.roadName || state.activeZone.name} (${d}m) • Limit: <strong>${state.activeZone.speedLimitKmh} km/h</strong>`;
      } else if (state.activeSpeedLimitZone) {
        radarStatus.innerHTML = `<strong>📍 Road Limit:</strong> ${state.activeSpeedLimitZone.name} • Limit: <strong>${state.activeSpeedLimitZone.speedLimitKmh} km/h</strong>`;
      } else {
        const area = limit === 80 ? "Ghana Highway" : "Urban Town Corridor";
        radarStatus.innerHTML = `<strong>Auto-Detect Active:</strong> ${area} • Current Limit: <strong>${limit} km/h</strong>`;
      }
    } else {
      radarCard.classList.remove('auto-active');
      radarStatus.textContent = `Manual speed limit mode active: ${state.manualLimitKmh} km/h. Outside camera zones.`;
    }
  }

  // Action Button
  const mainBtn = document.getElementById('mainActionBtn');
  if (mainBtn) {
    if (state.isMonitoring) {
      mainBtn.className = 'main-action-btn stop-btn';
      mainBtn.innerHTML = `<span>⏹</span> STOP MONITORING`;
    } else {
      mainBtn.className = 'main-action-btn start-btn';
      mainBtn.innerHTML = `<span>▶</span> START MONITORING`;
    }
  }

  // Trip Stats
  const maxSpeedEl = document.getElementById('statMaxSpeed');
  if (maxSpeedEl) maxSpeedEl.textContent = `${Math.round(state.trip.maxSpeedKmh)} km/h`;

  const avgSpeedEl = document.getElementById('statAvgSpeed');
  const avg = state.trip.speedReadingsCount > 0 ? Math.round(state.trip.speedSum / state.trip.speedReadingsCount) : 0;
  if (avgSpeedEl) avgSpeedEl.textContent = `${avg} km/h`;

  const distEl = document.getElementById('statDistance');
  if (distEl) distEl.textContent = `${state.trip.distanceKm.toFixed(1)} km`;

  // Manual Limit Preset Chips
  document.querySelectorAll('.chip-btn[data-limit]').forEach(chip => {
    const lim = parseInt(chip.dataset.limit, 10);
    if (!state.autoDetectEnabled && state.manualLimitKmh === lim) {
      chip.classList.add('selected');
    } else {
      chip.classList.remove('selected');
    }
  });

  // Alert Mode Button Icon (Sound -> Vibrate -> Mute)
  const muteBtn = document.getElementById('muteBtn');
  if (muteBtn) {
    if (state.alertMode === 'SOUND') {
      muteBtn.innerHTML = '🔊';
      muteBtn.title = 'Sound Alerts Active (Tap for Vibrate)';
    } else if (state.alertMode === 'VIBRATE') {
      muteBtn.innerHTML = '📳';
      muteBtn.title = 'Vibration Alerts Active (Tap for Mute)';
    } else {
      muteBtn.innerHTML = '🔇';
      muteBtn.title = 'Alerts Muted (Tap for Sound)';
    }
  }

  // Theme Icon
  const themeBtn = document.getElementById('themeBtn');
  if (themeBtn) {
    themeBtn.innerHTML = state.isDarkMode ? '☀️' : '🌙';
  }
}

// 11. Modal Controllers
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('open');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('open');
}

// 12. Initialization and Event Listeners
document.addEventListener('DOMContentLoaded', () => {
  // Apply saved theme
  document.documentElement.setAttribute('data-theme', state.isDarkMode ? 'dark' : 'light');

  // Check if browser permission query is supported
  if (navigator.permissions && navigator.permissions.query) {
    navigator.permissions.query({ name: 'geolocation' }).then((result) => {
      if (result.state === 'granted') {
        state.hasLocationPermission = true;
        localStorage.setItem('sc_has_location_perm', 'true');
      } else if (result.state === 'denied') {
        state.hasLocationPermission = false;
        localStorage.setItem('sc_has_location_perm', 'false');
      }
    }).catch(() => {});
  }

  // Main Action Button (Start / Stop)
  const mainBtn = document.getElementById('mainActionBtn');
  if (mainBtn) {
    mainBtn.addEventListener('click', () => {
      if (state.isMonitoring) {
        stopMonitoring();
      } else {
        startMonitoring();
      }
    });
  }

  // Permission Modal Buttons
  const grantBtn = document.getElementById('grantPermissionBtn');
  if (grantBtn) {
    grantBtn.addEventListener('click', () => {
      requestLocationAndStart();
    });
  }

  const retryBtn = document.getElementById('retryPermissionBtn');
  if (retryBtn) {
    retryBtn.addEventListener('click', () => {
      requestLocationAndStart();
    });
  }

  // Auto-Detect Switch
  const autoDetectToggle = document.getElementById('autoDetectToggle');
  if (autoDetectToggle) {
    autoDetectToggle.addEventListener('change', (e) => {
      state.autoDetectEnabled = e.target.checked;
      localStorage.setItem('sc_auto_detect', state.autoDetectEnabled);
      if (!state.autoDetectEnabled) {
        state.effectiveLimitKmh = state.manualLimitKmh;
      }
      updateUI();
    });
  }

  // Preset Chips
  document.querySelectorAll('.chip-btn[data-limit]').forEach(chip => {
    chip.addEventListener('click', () => {
      const lim = parseInt(chip.dataset.limit, 10);
      state.manualLimitKmh = lim;
      state.autoDetectEnabled = false;
      state.effectiveLimitKmh = lim;
      localStorage.setItem('sc_manual_limit', lim);
      localStorage.setItem('sc_auto_detect', false);
      updateUI();
    });
  });

  // Custom Limit Chip
  const customChip = document.getElementById('customLimitChip');
  if (customChip) {
    customChip.addEventListener('click', () => {
      const val = prompt("Enter custom speed limit in km/h (e.g. 40, 60, 70):", state.manualLimitKmh);
      if (val) {
        const parsed = parseInt(val, 10);
        if (!isNaN(parsed) && parsed > 0 && parsed <= 250) {
          state.manualLimitKmh = parsed;
          state.autoDetectEnabled = false;
          state.effectiveLimitKmh = parsed;
          localStorage.setItem('sc_manual_limit', parsed);
          localStorage.setItem('sc_auto_detect', false);
          updateUI();
        }
      }
    });
  }

  // Alert Mode Button (Cycles: Sound -> Vibrate -> Mute -> Sound)
  const muteBtn = document.getElementById('muteBtn');
  if (muteBtn) {
    muteBtn.addEventListener('click', () => {
      const nextMode = state.alertMode === 'SOUND' ? 'VIBRATE' : (state.alertMode === 'VIBRATE' ? 'MUTE' : 'SOUND');
      state.alertMode = nextMode;
      state.voiceAlertsEnabled = (nextMode === 'SOUND');
      if (nextMode === 'VIBRATE' && 'vibrate' in navigator) {
        navigator.vibrate(50);
      }
      localStorage.setItem('sc_alert_mode', nextMode);
      localStorage.setItem('sc_voice_alerts', state.voiceAlertsEnabled);
      updateUI();
    });
  }

  // Theme Toggle Button
  const themeBtn = document.getElementById('themeBtn');
  if (themeBtn) {
    themeBtn.addEventListener('click', () => {
      state.isDarkMode = !state.isDarkMode;
      localStorage.setItem('sc_theme', state.isDarkMode ? 'dark' : 'light');
      document.documentElement.setAttribute('data-theme', state.isDarkMode ? 'dark' : 'light');
      updateUI();
    });
  }

  // Settings Button
  const settingsBtn = document.getElementById('settingsBtn');
  if (settingsBtn) {
    settingsBtn.addEventListener('click', () => openModal('settingsModal'));
  }

  // Guide Button
  const guideBtn = document.getElementById('openGuideBtn');
  if (guideBtn) {
    guideBtn.addEventListener('click', () => {
      closeModal('settingsModal');
      openModal('guideModal');
    });
  }

  // Privacy Button
  const privacyBtn = document.getElementById('openPrivacyBtn');
  if (privacyBtn) {
    privacyBtn.addEventListener('click', () => {
      closeModal('settingsModal');
      openModal('privacyModal');
    });
  }

  // Modal Close buttons
  document.querySelectorAll('[data-close-modal]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.dataset.closeModal;
      closeModal(modalId);
    });
  });

  // Test Voice in Settings
  const testVoiceBtn = document.getElementById('testVoiceBtn');
  if (testVoiceBtn) {
    testVoiceBtn.addEventListener('click', () => {
      unlockAudioContext();
      speakVoice("Speed cameras ahead, slow down. You are about to cross the speed limit, slow down.");
    });
  }

  // Settings: Voice Style
  const voiceSelect = document.getElementById('voiceGenderSelect');
  if (voiceSelect) {
    voiceSelect.value = state.voiceGender;
    voiceSelect.addEventListener('change', (e) => {
      state.voiceGender = e.target.value;
      localStorage.setItem('sc_voice_gender', state.voiceGender);
    });
  }

  // Settings: Cooldown
  const cooldownSelect = document.getElementById('cooldownSelect');
  if (cooldownSelect) {
    cooldownSelect.value = state.alertCooldownSeconds;
    cooldownSelect.addEventListener('change', (e) => {
      state.alertCooldownSeconds = parseInt(e.target.value, 10);
      localStorage.setItem('sc_cooldown', state.alertCooldownSeconds);
    });
  }

  // Detect iOS Safari to show "Add to Home Screen" guide if not in standalone
  const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
  const isStandalone = window.navigator.standalone || window.matchMedia('(display-mode: standalone)').matches;
  const iosBanner = document.getElementById('iosInstallBanner');
  if (isIos && !isStandalone && iosBanner) {
    iosBanner.style.display = 'flex';
  }

  // Register PWA Service Worker
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(err => {
      console.warn("SW registration error:", err);
    });
  }

  updateUI();
});
