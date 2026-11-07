package com.speedcheck.android.service

import android.content.res.AssetManager
import android.location.Location
import com.speedcheck.android.model.CameraDetectionResult
import com.speedcheck.android.model.MatchedRoadSpeedLimit
import com.speedcheck.android.model.SpeedCameraZone
import com.speedcheck.android.model.SpeedLimitInfo
import com.speedcheck.android.model.SpeedLimitZone
import kotlin.math.abs

/**
 * Road-aware dual-layer detector for Ghana speed enforcement.
 *
 * Layer 1 — Speed Camera Zones: Known MTTD automated camera positions.
 *           Triggers camera-specific voice alerts.
 * Layer 2 — Speed Limit Zones: Named road segments with their legal limit.
 *           Provides accurate speed limits without camera warnings.
 * Fallback — the user-selected manual speed limit on unmapped roads.
 *
 * Speed limits prefer a confident OSM road match, then camera/curated zones, then the manual limit.
 * Camera alerts remain independent from the selected speed-limit source.
 */
object SpeedCameraDetector {
    const val CAMERA_ALERT_DISTANCE_METERS = 75f
    private const val CAMERA_DIRECTION_OVERRIDE_DISTANCE_METERS = 50f

    @Volatile
    private var openStreetMapMatcher: OpenStreetMapSpeedLimitMatcher? = null

    @Synchronized
    fun loadOpenStreetMapSpeedLimits(assetManager: AssetManager): Int {
        openStreetMapMatcher?.let { return it.roadCount }
        val matcher = OpenStreetMapSpeedLimitData.load(assetManager)
        openStreetMapMatcher = matcher
        return matcher.roadCount
    }

    // =====================================================================
    // LAYER 1: SPEED CAMERA ZONES (known MTTD automated cameras)
    // =====================================================================
    val GHANA_SPEED_CAMERAS = listOf(
        // -----------------------------------------------------------------
        // 1. Confirmed MTTD Highway & Arterial Corridors (Accra)
        // -----------------------------------------------------------------
        SpeedCameraZone(
            id = "borteyman_junction",
            name = "Borteyman Sports Facility T-Junction",
            roadName = "Borteyman / Motorway Corridor",
            latitude = 5.6675,
            longitude = -0.1030,
            speedLimitKmh = 80,
            radiusMeters = 1400f,
            description = "Automated camera enforcing speed along the Borteyman Sports Complex arterial link."
        ),
        SpeedCameraZone(
            id = "n1_fiesta_royale",
            name = "N1 Highway (Fiesta Royale Hotel)",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.6062,
            longitude = -0.1985,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "High-speed radar camera mounted near Fiesta Royale Hotel / Dzorwulu footbridge."
        ),
        SpeedCameraZone(
            id = "n1_baah_junction",
            name = "N1 Highway (Baah / Bar Junction)",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.5925,
            longitude = -0.2580,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "Automated camera monitoring the Lapaz to Kwashieman / Baah Junction corridor."
        ),
        SpeedCameraZone(
            id = "kawukudi_obasanjo",
            name = "Kawukudi (Obasanjo Highway)",
            roadName = "Olusegun Obasanjo Highway",
            latitude = 5.5892,
            longitude = -0.1932,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Automated camera enforcing the 50 km/h urban limit at Kawukudi Junction."
        ),
        SpeedCameraZone(
            id = "kanda_highway",
            name = "Kanda Highway",
            roadName = "Kanda Highway",
            latitude = 5.5780,
            longitude = -0.1970,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Automated camera monitoring the Kanda Highway arterial corridor."
        ),

        // -----------------------------------------------------------------
        // 2. Central / East Accra & Coastal Corridors
        // -----------------------------------------------------------------
        SpeedCameraZone(
            id = "liberation_nehru_jct",
            name = "Liberation Rd (Jawaharlal Nehru / Liberation Link)",
            roadName = "Liberation Road (N4)",
            latitude = 5.5882,
            longitude = -0.1818,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Mobile speedcam hotspot monitoring Liberation Rd near Airport City and Jawaharlal Nehru Rd junction.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "obasanjo_liberation_end",
            name = "Obasanjo Highway (37 / Liberation End)",
            roadName = "Olusegun Obasanjo Way",
            latitude = 5.5862,
            longitude = -0.1865,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            description = "Mobile patrol checkpoint on the eastern end of Obasanjo Way near 37 Interchange.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "cantonments_road",
            name = "Cantonments Road (Osu / Cantonments)",
            roadName = "Cantonments Road",
            latitude = 5.5565,
            longitude = -0.1812,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            description = "Mobile speed monitoring on the Cantonments / Osu arterial route.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "ring_road_nima_kanda",
            name = "Ring Road Central (Nima / Kanda Overpass)",
            roadName = "Ring Road Central",
            latitude = 5.5742,
            longitude = -0.1982,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            description = "Mobile radar trap enforcing urban speed along Ring Road Central near Nima junction.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "teshie_link",
            name = "Teshie Link Corridor",
            roadName = "Teshie Link",
            latitude = 5.5905,
            longitude = -0.0982,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Frequent mobile speed enforcement along Teshie Link connecting Spintex to Beach Road.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "laterbiokorshie_star_oil",
            name = "Laterbiokorshie (Near Star Oil)",
            roadName = "Link Road / Laterbiokorshie",
            latitude = 5.5502,
            longitude = -0.2335,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            description = "Mobile camera checkpoint enforcing 50 km/h urban speed limit near Star Oil.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "n1_densu_weija",
            name = "N1 Highway (Densu River Bridge / Weija)",
            roadName = "Accra - Cape Coast Highway (N1)",
            latitude = 5.5552,
            longitude = -0.3364,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "Mobile radar checkpoint on N1 western exit at Densu River bridge near Weija.",
            isMobilePatrol = true
        ),

        // -----------------------------------------------------------------
        // 3. Northern Stretch & N6 Highway (Accra West to North)
        // -----------------------------------------------------------------
        SpeedCameraZone(
            id = "apenkwa_achimota_overhead",
            name = "Apenkwa / Achimota Overhead",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.6145,
            longitude = -0.2225,
            speedLimitKmh = 80,
            radiusMeters = 1400f,
            description = "Mobile patrol hotspot monitoring N1 flow at Apenkwa and Achimota interchange.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "abelenkpe_main",
            name = "Abelenkpe Main Road",
            roadName = "Abelenkpe Road",
            latitude = 5.6012,
            longitude = -0.2085,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            description = "Mobile radar patrol on the Abelenkpe - Roman Ridge transit road.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "ofankor_taifa_barrier",
            name = "Ofankor Barrier / Taifa (N6)",
            roadName = "Accra - Nsawam Highway (N6)",
            latitude = 5.6565,
            longitude = -0.2582,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "Mobile radar setup monitoring northbound N6 traffic at Ofankor Barrier / Taifa.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "sowutuom_christian_village",
            name = "Pillar 2 / Sowutuom (Mobile Setup)",
            roadName = "Sowutuom Road / Pillar 2",
            latitude = 5.6162,
            longitude = -0.2692,
            speedLimitKmh = 40,
            radiusMeters = 1100f,
            description = "Frequent mobile tripod setup near Pillar 2 where limit drops sharply between 30 and 50 km/h.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "haatso_atomic_madina",
            name = "Haatso - Atomic - Madina Road",
            roadName = "Atomic - Haatso Road",
            latitude = 5.6705,
            longitude = -0.1872,
            speedLimitKmh = 50,
            radiusMeters = 1300f,
            description = "Mobile patrol monitoring Haatso, Papao, and Madina Zongo transit stretch.",
            isMobilePatrol = true
        ),

        // -----------------------------------------------------------------
        // 4. East Legon & Adentan Cluster
        // -----------------------------------------------------------------
        SpeedCameraZone(
            id = "east_legon_shiashie",
            name = "East Legon (Shiashie / Lagos Ave)",
            roadName = "Lagos Avenue",
            latitude = 5.6265,
            longitude = -0.1772,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            description = "Camera monitoring Shiashie entrance into East Legon along Lagos Avenue.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "east_legon_bawaleshie",
            name = "Bawaleshie / American House (East Legon)",
            roadName = "Boundary Road",
            latitude = 5.6372,
            longitude = -0.1582,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            description = "Mobile patrol checkpoint near Bawaleshie and American House junction.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "adjiringanor_ogbojo",
            name = "Adjiringanor - Ogbojo Link",
            roadName = "Adjiringanor Road",
            latitude = 5.6482,
            longitude = -0.1385,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            description = "Mobile patrol along Adjiringanor and Ogbojo residential arterial.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "east_legon_madina_30kmh",
            name = "East Legon - Madina Road (30 km/h School Zone)",
            roadName = "East Legon - Madina Link Road",
            latitude = 5.6582,
            longitude = -0.1552,
            speedLimitKmh = 30,
            radiusMeters = 1000f,
            description = "Strict 30 km/h zone in school and pedestrian crossing zone.",
            isMobilePatrol = true
        ),

        // -----------------------------------------------------------------
        // 5. Kumasi & Ashanti Region (N6 Corridor & Metro)
        // -----------------------------------------------------------------
        SpeedCameraZone(
            id = "kumasi_n6_bomso_ayigya",
            name = "Kumasi N6 (Bomso / Ayigya / KNUST Gate)",
            roadName = "Kumasi - Accra Highway (N6)",
            latitude = 6.6852,
            longitude = -1.5832,
            speedLimitKmh = 50,
            radiusMeters = 1300f,
            description = "Mobile patrol hotspot near KNUST Main Gate, Bomso, and Ayigya on N6.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "kumasi_n6_kentinkrono",
            name = "Kumasi N6 (Kentinkrono / Nsenie)",
            roadName = "Kumasi - Accra Highway (N6)",
            latitude = 6.6972,
            longitude = -1.5542,
            speedLimitKmh = 60,
            radiusMeters = 1400f,
            description = "Mobile radar trap monitoring eastern entry into Kumasi at Kentinkrono and Nsenie.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "kumasi_anwiankwanta",
            name = "Anwiankwanta Junction (Kumasi - Obuasi)",
            roadName = "Kumasi - Obuasi Highway (N10)",
            latitude = 6.4782,
            longitude = -1.6372,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "Highway mobile radar monitoring approach to Anwiankwanta junction.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "kumasi_asokwa_mall",
            name = "Kumasi Asokwa (City Mall / Bypass)",
            roadName = "24th February Road / Asokwa",
            latitude = 6.6712,
            longitude = -1.6062,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Mobile radar trap near Kumasi City Mall on the Asokwa bypass.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "kumasi_adukrom_asokore_mampong",
            name = "Kumasi (Adukrom / Asokore Mampong)",
            roadName = "Asokore Mampong Arterial",
            latitude = 6.7022,
            longitude = -1.5952,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Mobile speedcam monitoring side routes off N6 towards Asokore Mampong and Adukrom.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "kumasi_knust_ayeduase",
            name = "KNUST Ayeduase Gate",
            roadName = "Ayeduase Road",
            latitude = 6.6752,
            longitude = -1.5652,
            speedLimitKmh = 30,
            radiusMeters = 900f,
            description = "Campus and hostel residential zone mobile trap enforcing 30 km/h.",
            isMobilePatrol = true
        ),
        SpeedCameraZone(
            id = "kumasi_boankra_trap",
            name = "Boankra N6 Highway (Mobile Speed Trap)",
            roadName = "Accra - Kumasi Highway (N6)",
            latitude = 6.6385,
            longitude = -1.4180,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            description = "Frequent rotating police mobile speedcam setup where highway limit unpredictably drops to 50 km/h near Boankra.",
            isMobilePatrol = true
        )
    )

    // =====================================================================
    // LAYER 2: SPEED LIMIT ZONES (named road segments, no cameras)
    // These provide accurate speed limits for roads without cameras.
    // =====================================================================
    val GHANA_SPEED_LIMIT_ZONES = listOf(
        // -----------------------------------------------------------------
        // Greater Accra — Major Highways & Motorways
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "tema_motorway_east",
            roadName = "Tema Motorway",
            latitude = 5.6225,
            longitude = -0.0765,
            speedLimitKmh = 100,
            radiusMeters = 2500f,
            areaType = "Motorway"
        ),
        SpeedLimitZone(
            id = "tema_motorway_central",
            roadName = "Tema Motorway",
            latitude = 5.6350,
            longitude = -0.1130,
            speedLimitKmh = 100,
            radiusMeters = 2500f,
            areaType = "Motorway"
        ),
        SpeedLimitZone(
            id = "tema_motorway_west",
            roadName = "Tema Motorway",
            latitude = 5.6182,
            longitude = -0.1420,
            speedLimitKmh = 100,
            radiusMeters = 2000f,
            areaType = "Motorway"
        ),
        SpeedLimitZone(
            id = "n1_pokuase_interchange",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.6692,
            longitude = -0.2812,
            speedLimitKmh = 80,
            radiusMeters = 2000f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "n1_mallam_jct",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.5772,
            longitude = -0.2755,
            speedLimitKmh = 80,
            radiusMeters = 1800f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "n1_santa_maria",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.6015,
            longitude = -0.2285,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "n1_tetteh_quarshie",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.6135,
            longitude = -0.1680,
            speedLimitKmh = 80,
            radiusMeters = 1800f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "airport_bypass_north",
            roadName = "Airport Bypass Road",
            latitude = 5.6145,
            longitude = -0.1725,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "airport_bypass_south",
            roadName = "Airport Bypass Road",
            latitude = 5.5985,
            longitude = -0.1635,
            speedLimitKmh = 80,
            radiusMeters = 1200f,
            areaType = "Highway"
        ),

        // -----------------------------------------------------------------
        // Greater Accra — Urban Arterials (50 km/h)
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "independence_ave",
            roadName = "Independence Avenue",
            latitude = 5.5565,
            longitude = -0.1965,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "liberation_road_west",
            roadName = "Liberation Road (N4)",
            latitude = 5.5832,
            longitude = -0.1925,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "liberation_road_east",
            roadName = "Liberation Road (N4)",
            latitude = 5.5905,
            longitude = -0.1685,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "ring_road_east",
            roadName = "Ring Road East",
            latitude = 5.5662,
            longitude = -0.1842,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "ring_road_west",
            roadName = "Ring Road West",
            latitude = 5.5608,
            longitude = -0.2132,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "ring_road_south_makola",
            roadName = "Ring Road South",
            latitude = 5.5498,
            longitude = -0.2068,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "graphic_road",
            roadName = "Graphic Road",
            latitude = 5.5555,
            longitude = -0.2198,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "abossey_okai",
            roadName = "Abossey Okai Road",
            latitude = 5.5535,
            longitude = -0.2285,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "winneba_road_urban",
            roadName = "Winneba Road",
            latitude = 5.5412,
            longitude = -0.2395,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kaneshie_odorkor",
            roadName = "Kaneshie - Odorkor Road",
            latitude = 5.5688,
            longitude = -0.2445,
            speedLimitKmh = 50,
            radiusMeters = 1300f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "dansoman_road",
            roadName = "Dansoman High Street",
            latitude = 5.5365,
            longitude = -0.2510,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "spintex_road_west",
            roadName = "Spintex Road",
            latitude = 5.6315,
            longitude = -0.1172,
            speedLimitKmh = 50,
            radiusMeters = 1400f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "spintex_road_central",
            roadName = "Spintex Road",
            latitude = 5.6378,
            longitude = -0.0825,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "spintex_road_east",
            roadName = "Spintex Road",
            latitude = 5.6458,
            longitude = -0.0525,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "oxford_street_osu",
            roadName = "Oxford Street (Osu)",
            latitude = 5.5528,
            longitude = -0.1782,
            speedLimitKmh = 50,
            radiusMeters = 800f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "labone_residential",
            roadName = "Labone Residential Road",
            latitude = 5.5618,
            longitude = -0.1732,
            speedLimitKmh = 50,
            radiusMeters = 900f,
            areaType = "Residential"
        ),
        SpeedLimitZone(
            id = "dzorwulu_north",
            roadName = "Dzorwulu Road",
            latitude = 5.6092,
            longitude = -0.1985,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "north_dzorwulu",
            roadName = "North Dzorwulu Road",
            latitude = 5.6155,
            longitude = -0.2025,
            speedLimitKmh = 50,
            radiusMeters = 900f,
            areaType = "Residential"
        ),
        SpeedLimitZone(
            id = "east_legon_main",
            roadName = "East Legon Main Road",
            latitude = 5.6328,
            longitude = -0.1678,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "madina_highway",
            roadName = "Madina - Accra Highway",
            latitude = 5.6755,
            longitude = -0.1645,
            speedLimitKmh = 50,
            radiusMeters = 1400f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "adenta_dodowa_road",
            roadName = "Adenta - Dodowa Road",
            latitude = 5.7025,
            longitude = -0.1532,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "community_1_tema",
            roadName = "Community 1 Road, Tema",
            latitude = 5.6695,
            longitude = -0.0162,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "tema_harbour_road",
            roadName = "Tema Harbour Road",
            latitude = 5.6348,
            longitude = -0.0045,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "beach_road_labadi",
            roadName = "Beach Road (Labadi)",
            latitude = 5.5568,
            longitude = -0.1512,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "beach_road_nungua",
            roadName = "Beach Road (Nungua)",
            latitude = 5.5805,
            longitude = -0.0785,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "sakumono_village",
            roadName = "Sakumono Village Road",
            latitude = 5.6168,
            longitude = -0.0455,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            areaType = "Residential"
        ),
        SpeedLimitZone(
            id = "kasoa_main_road",
            roadName = "Kasoa Main Road",
            latitude = 5.5335,
            longitude = -0.4165,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "dodowa_road_madina",
            roadName = "Dodowa Road (Madina)",
            latitude = 5.6895,
            longitude = -0.1575,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "adentan_bypass",
            roadName = "Adentan Bypass Road",
            latitude = 5.7108,
            longitude = -0.1432,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            areaType = "Highway"
        ),

        // -----------------------------------------------------------------
        // Greater Accra — School Zones (30 km/h)
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "achimota_school_zone",
            roadName = "Achimota School Zone",
            latitude = 5.6275,
            longitude = -0.2295,
            speedLimitKmh = 30,
            radiusMeters = 800f,
            areaType = "School Zone"
        ),
        SpeedLimitZone(
            id = "legon_university_zone",
            roadName = "University of Ghana Campus Road",
            latitude = 5.6502,
            longitude = -0.1862,
            speedLimitKmh = 30,
            radiusMeters = 1200f,
            areaType = "School Zone"
        ),
        SpeedLimitZone(
            id = "presec_legon_zone",
            roadName = "Presbyterian Boys (Presec) Zone",
            latitude = 5.6595,
            longitude = -0.1788,
            speedLimitKmh = 30,
            radiusMeters = 700f,
            areaType = "School Zone"
        ),

        // -----------------------------------------------------------------
        // N1 Accra - Cape Coast / Winneba Highway Segments
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "n1_kasoa_bawjiase",
            roadName = "Accra - Cape Coast Highway (N1)",
            latitude = 5.5205,
            longitude = -0.4485,
            speedLimitKmh = 80,
            radiusMeters = 2500f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "n1_winneba_jct",
            roadName = "Winneba Junction (N1)",
            latitude = 5.3632,
            longitude = -0.5985,
            speedLimitKmh = 80,
            radiusMeters = 2000f,
            areaType = "Highway"
        ),

        // -----------------------------------------------------------------
        // N6 Accra - Kumasi Highway Segments (between cities)
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "n6_nsawam",
            roadName = "Accra - Kumasi Highway (N6) at Nsawam",
            latitude = 5.8085,
            longitude = -0.3492,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban" // Township speed limit
        ),
        SpeedLimitZone(
            id = "n6_suhum",
            roadName = "Accra - Kumasi Highway (N6) at Suhum",
            latitude = 6.0392,
            longitude = -0.4508,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "n6_nkawkaw",
            roadName = "Accra - Kumasi Highway (N6) at Nkawkaw",
            latitude = 6.5538,
            longitude = -0.7688,
            speedLimitKmh = 50,
            radiusMeters = 1800f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "n6_konongo",
            roadName = "Accra - Kumasi Highway (N6) at Konongo",
            latitude = 6.6172,
            longitude = -1.2225,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "n6_ejisu",
            roadName = "Accra - Kumasi Highway (N6) at Ejisu",
            latitude = 6.6878,
            longitude = -1.4582,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "n6_highway_suhum_nkawkaw",
            roadName = "Accra - Kumasi Highway (N6)",
            latitude = 6.2965,
            longitude = -0.6098,
            speedLimitKmh = 100,
            radiusMeters = 5000f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "n6_highway_nkawkaw_konongo",
            roadName = "Accra - Kumasi Highway (N6)",
            latitude = 6.5855,
            longitude = -0.9958,
            speedLimitKmh = 100,
            radiusMeters = 5000f,
            areaType = "Highway"
        ),

        // -----------------------------------------------------------------
        // Kumasi Metro — Urban Roads
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "kumasi_kejetia",
            roadName = "Kejetia - Central Market Road",
            latitude = 6.6892,
            longitude = -1.6262,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_adum_prempeh",
            roadName = "Adum / Prempeh II Street",
            latitude = 6.6905,
            longitude = -1.6218,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_harper_road",
            roadName = "Harper Road, Kumasi",
            latitude = 6.6948,
            longitude = -1.6152,
            speedLimitKmh = 50,
            radiusMeters = 1000f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_lake_road",
            roadName = "Lake Road, Kumasi",
            latitude = 6.6808,
            longitude = -1.6115,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_suame_magazine",
            roadName = "Suame Magazine Road",
            latitude = 6.7122,
            longitude = -1.6335,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_ahinsan_bypass",
            roadName = "Ahinsan - Atonsu Bypass",
            latitude = 6.6588,
            longitude = -1.6278,
            speedLimitKmh = 50,
            radiusMeters = 1300f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_bantama_road",
            roadName = "Bantama Road, Kumasi",
            latitude = 6.6955,
            longitude = -1.6422,
            speedLimitKmh = 50,
            radiusMeters = 1100f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "kumasi_knust_campus",
            roadName = "KNUST Campus Internal Roads",
            latitude = 6.6725,
            longitude = -1.5722,
            speedLimitKmh = 30,
            radiusMeters = 1500f,
            areaType = "School Zone"
        ),

        // -----------------------------------------------------------------
        // Kumasi — Highways
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "kumasi_n10_obuasi_highway",
            roadName = "Kumasi - Obuasi Highway (N10)",
            latitude = 6.5725,
            longitude = -1.6418,
            speedLimitKmh = 80,
            radiusMeters = 3000f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "kumasi_n6_north_exit",
            roadName = "Kumasi - Mampong Highway (N10)",
            latitude = 6.7325,
            longitude = -1.5952,
            speedLimitKmh = 80,
            radiusMeters = 2500f,
            areaType = "Highway"
        ),

        // -----------------------------------------------------------------
        // Other Major Cities
        // -----------------------------------------------------------------
        SpeedLimitZone(
            id = "takoradi_market_circle",
            roadName = "Market Circle Road, Takoradi",
            latitude = 4.8985,
            longitude = -1.7565,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "cape_coast_castle_road",
            roadName = "Cape Coast Castle Road",
            latitude = 5.1058,
            longitude = -1.2468,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "tamale_bolgatanga_road",
            roadName = "Tamale - Bolgatanga Road (N10)",
            latitude = 9.4075,
            longitude = -0.8392,
            speedLimitKmh = 50,
            radiusMeters = 1500f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "tamale_bypass",
            roadName = "Tamale Ring Road / Bypass",
            latitude = 9.3938,
            longitude = -0.8555,
            speedLimitKmh = 80,
            radiusMeters = 2000f,
            areaType = "Highway"
        ),
        SpeedLimitZone(
            id = "ho_main_road",
            roadName = "Ho Main Road",
            latitude = 6.6112,
            longitude = 0.4698,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "sunyani_ring_road",
            roadName = "Sunyani Ring Road",
            latitude = 7.3398,
            longitude = -2.3265,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        ),
        SpeedLimitZone(
            id = "koforidua_main_road",
            roadName = "Koforidua Main Road",
            latitude = 6.0942,
            longitude = -0.2568,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            areaType = "Urban"
        )
    )

    // Reusable Location object to avoid GC churn in hot loop
    private val probeLocation = Location("detector_probe")

    /**
     * Dual-layer location evaluation:
     * 1. Check speed camera zones (highest priority)
     * 2. Check speed limit zones (medium priority)
     * 3. Fallback to the user-selected limit when no mapped zone matches
     */
    fun evaluateLocation(
        currentLocation: Location,
        currentSpeedKmh: Float,
        autoDetectEnabled: Boolean,
        manualLimitKmh: Int
    ): CameraDetectionResult {
        if (!autoDetectEnabled) {
            return CameraDetectionResult(
                isAutoDetectActive = false,
                activeZone = null,
                activeSpeedLimitZone = null,
                distanceToCameraMeters = Float.MAX_VALUE,
                isMovingTowardsCamera = false,
                hasCameraAhead = false,
                detectedRoadName = null,
                areaType = "Manual",
                recommendedLimitKmh = manualLimitKmh,
                statusDescription = "Manual Limit ($manualLimitKmh km/h)"
            )
        }

        val mappedRoadLimit = findOpenStreetMapSpeedLimit(currentLocation, currentSpeedKmh)

        // ── Layer 1: Speed Camera Zone Detection ──
        val cameraResult = findClosestCameraZone(currentLocation, currentSpeedKmh)
        if (cameraResult != null) {
            val (zone, distance, movingTowards) = cameraResult
            val dist = distance.toInt()
            val effectiveLimit = mappedRoadLimit?.speedLimitKmh ?: zone.speedLimitKmh
            val cameraDescription = if (zone.isMobilePatrol) {
                "⚠️ Potential Police Speedcam: ${zone.roadName} (${dist}m) • $effectiveLimit km/h"
            } else {
                "📷 ${zone.roadName} (${dist}m) • $effectiveLimit km/h"
            }
            return CameraDetectionResult(
                isAutoDetectActive = true,
                activeZone = zone,
                activeSpeedLimitZone = mappedRoadLimit,
                distanceToCameraMeters = distance,
                isMovingTowardsCamera = movingTowards,
                hasCameraAhead = true,
                isMobilePatrol = zone.isMobilePatrol,
                detectedRoadName = mappedRoadLimit?.roadName ?: zone.roadName,
                areaType = mappedRoadLimit?.areaType
                    ?: if (zone.speedLimitKmh >= 80) "Highway" else if (zone.speedLimitKmh <= 30) "School Zone" else "Urban",
                recommendedLimitKmh = effectiveLimit,
                statusDescription = if (mappedRoadLimit != null) {
                    "$cameraDescription • OpenStreetMap road limit"
                } else {
                    cameraDescription
                }
            )
        }

        // ── Layer 2: Speed Limit Zone Detection ──
        val limitZone: SpeedLimitInfo? = mappedRoadLimit ?: findClosestSpeedLimitZone(currentLocation)
        if (limitZone != null) {
            return CameraDetectionResult(
                isAutoDetectActive = true,
                activeZone = null,
                activeSpeedLimitZone = limitZone,
                distanceToCameraMeters = Float.MAX_VALUE,
                isMovingTowardsCamera = false,
                hasCameraAhead = false,
                detectedRoadName = limitZone.roadName,
                areaType = limitZone.areaType,
                recommendedLimitKmh = limitZone.speedLimitKmh,
                statusDescription = if (limitZone is MatchedRoadSpeedLimit) {
                    "📍 OpenStreetMap road limit: ${limitZone.roadName} • ${limitZone.speedLimitKmh} km/h"
                } else {
                    "📍 ${limitZone.roadName} • ${limitZone.speedLimitKmh} km/h"
                }
            )
        }

        // ── Layer 3: Unmapped road fallback ──
        return CameraDetectionResult(
            isAutoDetectActive = true,
            activeZone = null,
            activeSpeedLimitZone = null,
            distanceToCameraMeters = Float.MAX_VALUE,
            isMovingTowardsCamera = false,
            hasCameraAhead = false,
            detectedRoadName = null,
            areaType = "Unmapped",
            recommendedLimitKmh = manualLimitKmh,
            statusDescription = "No mapped road limit; using selected limit ($manualLimitKmh km/h)"
        )
    }

    /**
     * Searches camera zones for the closest one the driver is heading towards.
     *
     * Direction logic:
     * - With GPS bearing + speed ≥ 10 km/h: requires camera to be within a 70° forward cone.
     *   Moving-away suppression: angle > 110° → skip unless within the close-proximity override.
     * - Close-proximity override: within 50m warns regardless of direction (safety buffer
     *   for roundabouts, U-turns, or congested intersections).
     * - No bearing data (stationary, GPS not locked): only warns within 75m.
     *
     * Returns Triple(zone, distance, isMovingTowards) or null if no approaching zone found.
     */
    private fun findClosestCameraZone(
        currentLocation: Location,
        currentSpeedKmh: Float
    ): Triple<SpeedCameraZone, Float, Boolean>? {
        var closestZone: SpeedCameraZone? = null
        var minDistance = Float.MAX_VALUE
        val hasHeading = currentLocation.hasBearing() && currentSpeedKmh >= 10f

        for (zone in GHANA_SPEED_CAMERAS) {
            probeLocation.latitude = zone.latitude
            probeLocation.longitude = zone.longitude

            val distance = currentLocation.distanceTo(probeLocation)
            if (distance > minOf(zone.radiusMeters, CAMERA_ALERT_DISTANCE_METERS)) continue

            val approaching: Boolean = if (hasHeading) {
                val bearingToCamera = currentLocation.bearingTo(probeLocation)
                // Angle between driver's heading and direction to camera
                // 0° = directly ahead, 180° = directly behind
                val angleDiff = abs((currentLocation.bearing - bearingToCamera + 540) % 360 - 180)

                when {
                    angleDiff <= 70f -> true          // Camera is in the forward 70° cone — approaching
                    distance <= CAMERA_DIRECTION_OVERRIDE_DISTANCE_METERS -> true // Very close: warn regardless of angle
                    else -> false                      // Moving away or perpendicular — suppress warning
                }
            } else {
                // No heading data: only warn within close proximity to avoid false alarms
                distance <= CAMERA_ALERT_DISTANCE_METERS
            }

            if (approaching && distance < minDistance) {
                minDistance = distance
                closestZone = zone
            }
        }

        return if (closestZone != null) Triple(closestZone, minDistance, true) else null
    }

    /**
     * Searches speed limit zones for the closest matching road segment.
     * Unlike camera zones, heading is not checked — being on the road is sufficient.
     */
    private fun findClosestSpeedLimitZone(currentLocation: Location): SpeedLimitZone? {
        var closestZone: SpeedLimitZone? = null
        var minDistance = Float.MAX_VALUE

        for (zone in GHANA_SPEED_LIMIT_ZONES) {
            probeLocation.latitude = zone.latitude
            probeLocation.longitude = zone.longitude

            val distance = currentLocation.distanceTo(probeLocation)
            if (distance <= zone.radiusMeters && distance < minDistance) {
                minDistance = distance
                closestZone = zone
            }
        }

        return closestZone
    }

    private fun findOpenStreetMapSpeedLimit(
        currentLocation: Location,
        currentSpeedKmh: Float
    ): MatchedRoadSpeedLimit? {
        val matcher = openStreetMapMatcher ?: return null
        val accuracy = if (currentLocation.hasAccuracy()) currentLocation.accuracy else null
        val bearing = if (currentLocation.hasBearing()) currentLocation.bearing else null
        return matcher.findMatch(
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            accuracyMeters = accuracy,
            bearingDegrees = bearing,
            speedKmh = currentSpeedKmh
        )
    }
}
