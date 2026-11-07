const test = require('node:test');
const assert = require('node:assert/strict');
const SpeedEstimator = require('./speed-estimator.js');

const start = { latitude: 5.6, longitude: -0.2 };

function position(timestamp, { latitude, longitude, accuracy = 5, speed = null }) {
  return {
    timestamp,
    coords: { latitude, longitude, accuracy, speed }
  };
}

function longitudeAfterMeters(longitude, meters) {
  return longitude + meters / (111320 * Math.cos(5.6 * Math.PI / 180));
}

test('a single extreme hardware-speed reading while stationary is ignored', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));
  const result = estimator.update(position(2_000, { ...start, speed: 50 }));

  assert.equal(result.speedKmh, 0);
  assert.equal(result.isMoving, false);
});

test('repeated hardware-speed readings without location movement stay at zero', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));

  for (let second = 2; second <= 6; second++) {
    const result = estimator.update(position(second * 1_000, { ...start, speed: 30 }));
    assert.equal(result.speedKmh, 0);
    assert.equal(result.isMoving, false);
  }
});

test('hardware speed cannot override contradictory position movement', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));

  for (let second = 2; second <= 5; second++) {
    const result = estimator.update(position(second * 1_000, {
      latitude: start.latitude,
      longitude: longitudeAfterMeters(start.longitude, (second - 1) * 2),
      speed: 30
    }));
    assert.equal(result.speedKmh, 0);
    assert.equal(result.isMoving, false);
  }
});

test('a one-off GPS coordinate jump does not create a speed spike', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));

  const jumped = {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 100)
  };
  assert.equal(estimator.update(position(2_000, jumped)).speedKmh, 0);
  const settled = estimator.update(position(3_000, jumped));
  assert.equal(settled.speedKmh, 0);
  assert.equal(settled.isMoving, false);
});

test('consecutive, position-supported vehicle movement is detected and smoothed', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));

  const firstMovingPosition = {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 20)
  };
  const first = estimator.update(position(2_000, { ...firstMovingPosition, speed: 30 }));
  assert.equal(first.speedKmh, 0);
  assert.equal(first.isMoving, false);

  const secondMovingPosition = {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 40)
  };
  const second = estimator.update(position(3_000, { ...secondMovingPosition, speed: 30 }));
  assert.equal(second.isMoving, true);
  assert.ok(second.speedKmh >= 8 && second.speedKmh <= 25);
});

test('unreliable accuracy and out-of-order locations are rejected', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(2_000, start));
  assert.equal(estimator.update(position(1_000, { ...start, speed: 30 })), null);
  assert.equal(estimator.update(position(3_000, { ...start, accuracy: 36, speed: 30 })), null);
  assert.equal(estimator.update(position(4_000, { ...start, accuracy: null, speed: 30 })), null);
});

test('a location gap resets stale motion instead of preserving an old speed', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));
  estimator.update(position(2_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 20),
    speed: 30
  }));
  estimator.update(position(3_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 40),
    speed: 30
  }));
  assert.ok(estimator.speedKmh > 0);

  const afterGap = estimator.update(position(14_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 400),
    speed: 30
  }));
  assert.equal(afterGap.speedKmh, 0);
  assert.equal(afterGap.isMoving, false);
});

test('sub-second samples are accumulated instead of losing movement distance', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));
  estimator.update(position(1_500, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 10),
    speed: 10
  }));
  const result = estimator.update(position(2_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 20),
    speed: 10
  }));

  assert.equal(result.speedKmh, 0);
  assert.equal(estimator.lastPosition.timestamp, 2_000);
  const next = estimator.update(position(3_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 40),
    speed: 10
  }));
  assert.equal(next.isMoving, true);
  assert.ok(next.speedKmh <= 25);
});

test('reset clears the speed and movement confirmation state', () => {
  const estimator = new SpeedEstimator();
  estimator.update(position(1_000, start));
  estimator.update(position(2_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 20),
    speed: 30
  }));
  estimator.update(position(3_000, {
    latitude: start.latitude,
    longitude: longitudeAfterMeters(start.longitude, 40),
    speed: 30
  }));
  estimator.reset();

  assert.equal(estimator.speedKmh, 0);
  assert.equal(estimator.isMoving, false);
  assert.equal(estimator.lastPosition, null);
});
