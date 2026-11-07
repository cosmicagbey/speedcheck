class SpeedEstimator {
  constructor() {
    this.reset();
  }

  reset() {
    this.lastPosition = null;
    this.speedKmh = 0;
    this.isMoving = false;
    this.movingReadings = 0;
    this.slowReadings = 0;
  }

  update(position) {
    const coords = position && position.coords;
    const timestamp = position && position.timestamp;
    if (!coords ||
        !Number.isFinite(coords.latitude) ||
        !Number.isFinite(coords.longitude) ||
        !Number.isFinite(coords.accuracy) ||
        coords.accuracy <= 0 ||
        coords.accuracy > SpeedEstimator.MAX_ACCURACY_METERS ||
        !Number.isFinite(timestamp) ||
        timestamp <= 0) {
      return null;
    }

    if (this.lastPosition && timestamp <= this.lastPosition.timestamp) {
      return null;
    }

    const currentPosition = {
      timestamp,
      latitude: coords.latitude,
      longitude: coords.longitude,
      accuracy: coords.accuracy
    };
    const previous = this.lastPosition;

    if (!previous) {
      this.lastPosition = currentPosition;
      return this.result();
    }

    const elapsedSeconds = (timestamp - previous.timestamp) / 1000;
    if (elapsedSeconds > SpeedEstimator.MAX_SAMPLE_GAP_SECONDS) {
      this.lastPosition = currentPosition;
      this.speedKmh = 0;
      this.isMoving = false;
      this.movingReadings = 0;
      this.slowReadings = 0;
      return this.result();
    }

    if (elapsedSeconds < SpeedEstimator.MIN_SAMPLE_INTERVAL_SECONDS) {
      return this.result();
    }

    this.lastPosition = currentPosition;
    const distanceMeters = SpeedEstimator.distanceMeters(
      previous.latitude,
      previous.longitude,
      coords.latitude,
      coords.longitude
    );
    const accuracy = Math.max(previous.accuracy, coords.accuracy);
    const minimumDisplacement = Math.max(
      SpeedEstimator.MIN_DISPLACEMENT_METERS,
      Math.min(SpeedEstimator.MAX_DISPLACEMENT_NOISE_METERS, accuracy * 0.2)
    );
    const hasPositionMovement = distanceMeters >= minimumDisplacement;
    const positionSpeedKmh = hasPositionMovement
      ? (distanceMeters / elapsedSeconds) * 3.6
      : 0;
    const hardwareSpeedKmh = Number.isFinite(coords.speed) &&
      coords.speed >= 0 &&
      coords.accuracy <= SpeedEstimator.MAX_HARDWARE_SPEED_ACCURACY_METERS &&
      coords.speed * 3.6 <= SpeedEstimator.MAX_SPEED_KMH
      ? coords.speed * 3.6
      : null;

    let candidateSpeedKmh = positionSpeedKmh;
    if (hardwareSpeedKmh !== null && hasPositionMovement && hardwareSpeedKmh >= SpeedEstimator.MIN_VEHICLE_SPEED_KMH) {
      candidateSpeedKmh = Math.min(hardwareSpeedKmh, positionSpeedKmh);
    }

    if (candidateSpeedKmh < SpeedEstimator.MIN_VEHICLE_SPEED_KMH) {
      this.movingReadings = 0;
      if (this.isMoving) {
        this.slowReadings++;
        if (this.slowReadings >= SpeedEstimator.STOP_CONFIRMATION_READINGS) {
          this.speedKmh = 0;
          this.isMoving = false;
          this.slowReadings = 0;
        }
      } else {
        this.speedKmh = 0;
      }
      return this.result();
    }

    this.slowReadings = 0;
    if (!this.isMoving) {
      this.movingReadings++;
      if (this.movingReadings < SpeedEstimator.MOVEMENT_CONFIRMATION_READINGS) {
        return this.result();
      }
      this.isMoving = true;
    }

    const maximumPlausibleSpeed = Math.min(
      SpeedEstimator.MAX_SPEED_KMH,
      this.speedKmh + SpeedEstimator.MAX_ACCELERATION_KMH_PER_SECOND * elapsedSeconds
    );
    const plausibleSpeed = Math.min(candidateSpeedKmh, maximumPlausibleSpeed);
    if (plausibleSpeed < SpeedEstimator.MIN_VEHICLE_SPEED_KMH) {
      this.speedKmh = 0;
      this.isMoving = false;
      this.movingReadings = 0;
      return this.result();
    }

    this.speedKmh = this.speedKmh === 0
      ? plausibleSpeed
      : (SpeedEstimator.SMOOTHING_FACTOR * plausibleSpeed) +
        ((1 - SpeedEstimator.SMOOTHING_FACTOR) * this.speedKmh);
    return this.result();
  }

  result() {
    return { speedKmh: this.speedKmh, isMoving: this.isMoving };
  }

  static distanceMeters(latitude1, longitude1, latitude2, longitude2) {
    const radians = (degrees) => (degrees * Math.PI) / 180;
    const deltaLatitude = radians(latitude2 - latitude1);
    const deltaLongitude = radians(longitude2 - longitude1);
    const latitude1Radians = radians(latitude1);
    const latitude2Radians = radians(latitude2);
    const haversine = Math.sin(deltaLatitude / 2) ** 2 +
      Math.cos(latitude1Radians) * Math.cos(latitude2Radians) *
      Math.sin(deltaLongitude / 2) ** 2;
    return 6371e3 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
  }
}

SpeedEstimator.MIN_VEHICLE_SPEED_KMH = 8;
SpeedEstimator.MAX_SPEED_KMH = 160;
SpeedEstimator.MAX_ACCURACY_METERS = 35;
SpeedEstimator.MAX_HARDWARE_SPEED_ACCURACY_METERS = 20;
SpeedEstimator.MIN_SAMPLE_INTERVAL_SECONDS = 0.8;
SpeedEstimator.MAX_SAMPLE_GAP_SECONDS = 10;
SpeedEstimator.MIN_DISPLACEMENT_METERS = 1.5;
SpeedEstimator.MAX_DISPLACEMENT_NOISE_METERS = 8;
SpeedEstimator.MOVEMENT_CONFIRMATION_READINGS = 2;
SpeedEstimator.STOP_CONFIRMATION_READINGS = 2;
SpeedEstimator.MAX_ACCELERATION_KMH_PER_SECOND = 25;
SpeedEstimator.SMOOTHING_FACTOR = 0.55;

if (typeof module !== 'undefined' && module.exports) {
  module.exports = SpeedEstimator;
}
