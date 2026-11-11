#!/usr/bin/env python3
"""Add offline safety and built-up-area zones to the existing Ghana OSM speed data."""

import argparse
import json
import math
from datetime import datetime, timezone
from pathlib import Path

import osmium


ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "docs" / "data" / "ghana_speed_limits.json"
REQUIRED_LICENSE = "Open Data Commons Open Database License (ODbL) v1.0"
METERS_PER_DEGREE = 111_320.0
HAZARD_CATEGORIES = {
    "school": (("amenity", {"school", "kindergarten", "college", "university"}), 250, 500),
    "playground": (("leisure", {"playground"}), 200, 350),
    "health": (("amenity", {"hospital", "clinic"}), 250, 500),
    "worship": (("amenity", {"place_of_worship"}), 200, 350),
    "market": (("amenity", {"marketplace"}), 250, 400),
}
SHOPPING_TAGS = {"mall", "supermarket", "department_store", "shopping_centre"}
PLACE_RADIUS_METERS = {
    "city": 4_000,
    "town": 2_000,
    "village": 600,
    "hamlet": 350,
    "suburb": 900,
    "neighbourhood": 450,
}
BUILT_UP_LANDUSES = {"residential", "commercial", "retail"}
HEALTH_CROSSING_DISTANCE_METERS = 150


def distance_meters(latitude1, longitude1, latitude2, longitude2):
    latitude_scale = METERS_PER_DEGREE
    longitude_scale = latitude_scale * math.cos(math.radians(latitude1))
    return math.hypot(
        (latitude2 - latitude1) * latitude_scale,
        (longitude2 - longitude1) * longitude_scale,
    )


class SafetyZoneExtractor(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.zones = []
        self.crossings = []

    def node(self, node):
        if not node.location.valid():
            return
        latitude = node.location.lat
        longitude = node.location.lon
        self._record(node.tags, f"node/{node.id}", latitude, longitude, [])
        if node.tags.get("highway") == "crossing" or "crossing" in node.tags:
            self.crossings.append((latitude, longitude))

    def way(self, way):
        tagged = self._has_relevant_tags(way.tags)
        is_crossing = way.tags.get("highway") == "crossing" or "crossing" in way.tags
        if not tagged and not is_crossing:
            return

        coordinates = [
            (node.location.lat, node.location.lon)
            for node in way.nodes
            if node.location.valid()
        ]
        if not coordinates:
            return

        latitude = (min(point[0] for point in coordinates) + max(point[0] for point in coordinates)) / 2
        longitude = (min(point[1] for point in coordinates) + max(point[1] for point in coordinates)) / 2
        if is_crossing:
            self.crossings.append((latitude, longitude))
        if not tagged:
            return
        self._record(way.tags, f"way/{way.id}", latitude, longitude, coordinates)

    def _has_relevant_tags(self, tags):
        if tags.get("place") in PLACE_RADIUS_METERS:
            return True
        if tags.get("landuse") in BUILT_UP_LANDUSES:
            return True
        return (
            tags.get("amenity") in SHOPPING_TAGS
            or tags.get("shop") in SHOPPING_TAGS
            or tags.get("amenity") in {
                "school", "kindergarten", "college", "university", "hospital",
                "clinic", "place_of_worship", "marketplace",
            }
            or tags.get("leisure") == "playground"
        )

    def _record(self, tags, osm_id, latitude, longitude, geometry):
        if not (-90 <= latitude <= 90 and -180 <= longitude <= 180):
            return

        category = None
        radius = None
        for candidate, ((tag_key, values), base_radius, max_radius) in HAZARD_CATEGORIES.items():
            if tags.get(tag_key) in values:
                category = candidate
                radius = base_radius
                if geometry:
                    radius = max(
                        radius,
                        max(distance_meters(latitude, longitude, lat, lon) for lat, lon in geometry),
                    )
                    radius = min(radius, max_radius)
                break

        if category is None and (
            tags.get("shop") in SHOPPING_TAGS or tags.get("amenity") in SHOPPING_TAGS
        ):
            category = "market"
            radius = 250
            if geometry:
                radius = min(
                    400,
                    max(radius, max(distance_meters(latitude, longitude, lat, lon) for lat, lon in geometry)),
                )

        if category is None and tags.get("landuse") == "retail":
            category = "market"
            radius = 250
            if geometry:
                radius = min(
                    400,
                    max(radius, max(distance_meters(latitude, longitude, lat, lon) for lat, lon in geometry)),
                )

        if category == "health":
            self.zones.append({
                "id": osm_id,
                "name": tags.get("name", ""),
                "category": category,
                "latitude": latitude,
                "longitude": longitude,
                "radiusMeters": radius,
                "requiresCrossing": True,
            })
            return

        if category is not None:
            self.zones.append({
                "id": osm_id,
                "name": tags.get("name", ""),
                "category": category,
                "latitude": latitude,
                "longitude": longitude,
                "radiusMeters": radius,
            })
            return

        place = tags.get("place")
        if place in PLACE_RADIUS_METERS:
            self.zones.append({
                "id": osm_id,
                "name": tags.get("name", ""),
                "category": "built_up",
                "latitude": latitude,
                "longitude": longitude,
                "radiusMeters": PLACE_RADIUS_METERS[place],
            })
            return

        if tags.get("landuse") in BUILT_UP_LANDUSES and geometry:
            radius = max(distance_meters(latitude, longitude, lat, lon) for lat, lon in geometry)
            if radius >= 75:
                self.zones.append({
                    "id": osm_id,
                    "name": tags.get("name", ""),
                    "category": "built_up",
                    "latitude": latitude,
                    "longitude": longitude,
                    "radiusMeters": min(radius, 1_000),
                })

    def finish(self):
        crossing_cells = {}
        for latitude, longitude in self.crossings:
            key = self._cell(latitude, longitude)
            crossing_cells.setdefault(key, []).append((latitude, longitude))

        def has_nearby_crossing(zone):
            latitude = zone["latitude"]
            longitude = zone["longitude"]
            latitude_cell, longitude_cell = self._cell(latitude, longitude)
            for lat_cell in range(latitude_cell - 1, latitude_cell + 2):
                for lon_cell in range(longitude_cell - 1, longitude_cell + 2):
                    for crossing in crossing_cells.get((lat_cell, lon_cell), ()):
                        if distance_meters(latitude, longitude, *crossing) <= HEALTH_CROSSING_DISTANCE_METERS:
                            return True
            return False

        zones = [
            zone for zone in self.zones
            if not zone.get("requiresCrossing") or has_nearby_crossing(zone)
        ]
        for zone in zones:
            zone.pop("requiresCrossing", None)
        return zones

    @staticmethod
    def _cell(latitude, longitude):
        return math.floor(latitude / 0.01), math.floor(longitude / 0.01)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("osm_pbf", type=Path, help="Geofabrik Ghana .osm.pbf extract")
    parser.add_argument("--dataset", type=Path, default=DATASET, help="Speed data JSON to update")
    args = parser.parse_args()

    with args.dataset.open(encoding="utf-8") as source:
        dataset = json.load(source)
    if dataset.get("license") != REQUIRED_LICENSE or not isinstance(dataset.get("ways"), list):
        raise ValueError("The speed data must contain the expected ODbL attribution and ways.")

    extractor = SafetyZoneExtractor()
    extractor.apply_file(str(args.osm_pbf), locations=True, idx="flex_mem")
    safety_zones = extractor.finish()
    if not safety_zones:
        raise ValueError("The OSM extract did not yield any safety or built-up zones.")

    dataset["schemaVersion"] = 2
    dataset["snapshotDate"] = datetime.now(timezone.utc).date().isoformat()
    safety_policy = (
        " Safety zones use tagged OSM school, playground, worship, market, and health facilities; "
        "health facilities are included only when a mapped crossing is within 150 m. "
        "Built-up zones use tagged city, town, village, suburb, neighbourhood, and landuse areas."
    )
    if safety_policy.strip() not in dataset["extractionPolicy"]:
        dataset["extractionPolicy"] += safety_policy
    dataset["safetyZones"] = safety_zones
    with args.dataset.open("w", encoding="utf-8", newline="\n") as output:
        json.dump(dataset, output, ensure_ascii=False, separators=(",", ":"))
        output.write("\n")
    print(f"Wrote {len(safety_zones)} OSM safety and built-up zones to {args.dataset}")


if __name__ == "__main__":
    main()
