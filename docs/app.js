/**
 * SpeedCheck Web (PWA) - Smart GPS Speed Monitor & MTTD Camera Radar for Ghana
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
  }
];

// 2. Application State
const state = {
  isMonitoring: false,
  currentSpeedKmh: 0,
  manualLimitKmh: parseInt(localStorage.getItem('sc_manual_limit') || '80', 10),
  autoDetectEnabled: localStorage.getItem('sc_auto_detect') !== 'false',
  voiceAlertsEnabled: localStorage.getItem('sc_voice_alerts') !== 'false',
  voiceGender: localStorage.getItem('sc_voice_gender') || 'FEMALE',
  alertCooldownSeconds: parseInt(localStorage.getItem('sc_cooldown') || '15', 10),
  approachingOffsetKmh: 5,
  isDarkMode: localStorage.getItem('sc_theme') !== 'light',

  // Live Location & Radar
  lastPosition: null,
  activeZone: null,
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

// 4. Camera Detection Engine
function evaluateCameraZones(currentLat, currentLon, speedKmh, heading) {
  if (!state.autoDetectEnabled) {
    state.activeZone = null;
    state.distanceToCameraMeters = null;
    state.effectiveLimitKmh = state.manualLimitKmh;
    return;
  }

  let closestZone = null;
  let minDistance = Infinity;
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

      if (movingTowards && dist < minDistance) {
        minDistance = dist;
        closestZone = zone;
        isMovingTowards = true;
      }
    }
  }

  if (closestZone) {
    state.activeZone = closestZone;
    state.distanceToCameraMeters = minDistance;
    state.isMovingTowardsCamera = isMovingTowards;
    state.effectiveLimitKmh = closestZone.speedLimitKmh;

    // Trigger Camera Zone Voice Announcement once per corridor
    if (state.voiceAlertsEnabled && closestZone.id !== state.lastAnnouncedCameraZoneId && isMovingTowards) {
      speakVoice(`Speed camera ahead on ${closestZone.name}. Speed limit is ${closestZone.speedLimitKmh} kilometers per hour.`);
      state.lastAnnouncedCameraZoneId = closestZone.id;
    }
  } else {
    state.activeZone = null;
    state.distanceToCameraMeters = null;
    state.isMovingTowardsCamera = false;
    state.lastAnnouncedCameraZoneId = null;

    // Outside camera zones, smart default based on road speed (80 km/h highway vs 50 km/h urban)
    state.effectiveLimitKmh = speedKmh >= 65 ? 80 : 50;
  }
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

// Play silent chime or audio buffer on user interaction to unlock Safari Web Audio
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

// 6. Alert Logic
function processSpeedAlerts(speedKmh, limitKmh) {
  if (!state.voiceAlertsEnabled || !state.isMonitoring) return;

  const now = Date.now();
  const approachingThreshold = Math.max(1, limitKmh - state.approachingOffsetKmh);

  if (speedKmh >= limitKmh) {
    const cooldownMs = state.alertCooldownSeconds * 1000;
    if (now - state.lastExceededAlertTime >= cooldownMs) {
      speakVoice(`Caution. Speed is over your limit of ${limitKmh} kilometers per hour. Please slow down.`);
      state.lastExceededAlertTime = now;
    }
  } else if (speedKmh >= approachingThreshold && speedKmh < limitKmh) {
    if (!state.hasApproachingFiredInZone) {
      speakVoice(`Approaching ${limitKmh} kilometers per hour.`);
      state.hasApproachingFiredInZone = true;
    }
  } else if (speedKmh < approachingThreshold - 2) {
    state.hasApproachingFiredInZone = false;
  }
}

// 7. Screen WakeLock (prevents screen lock while mounted on dashboard)
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

// 8. GPS Position Processing
function handlePositionUpdate(position) {
  const coords = position.coords;
  let calculatedSpeed = 0;

  // Use hardware GPS speed if provided (m/s -> km/h)
  if (coords.speed !== null && !isNaN(coords.speed) && coords.speed >= 0) {
    calculatedSpeed = coords.speed * 3.6;
  } else if (state.lastPosition) {
    // Fallback: Haversine distance over delta time
    const prev = state.lastPosition;
    const deltaSeconds = (position.timestamp - prev.timestamp) / 1000;
    if (deltaSeconds > 0) {
      const dist = calculateDistanceMeters(prev.coords.latitude, prev.coords.longitude, coords.latitude, coords.longitude);
      calculatedSpeed = (dist / deltaSeconds) * 3.6;
    }
  }

  state.currentSpeedKmh = Math.max(0, calculatedSpeed);

  // Update trip stats
  if (state.lastPosition) {
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
  if (state.currentSpeedKmh > 3) {
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
  if (statusEl) {
    statusEl.textContent = error.code === 1 ? "Location Denied" : "GPS Signal Lost";
  }
}

// 9. Start / Stop Speed Monitoring
function startMonitoring() {
  unlockAudioContext();
  requestWakeLock();

  state.isMonitoring = true;
  state.trip.startTime = Date.now();
  state.trip.maxSpeedKmh = 0;
  state.trip.speedSum = 0;
  state.trip.speedReadingsCount = 0;
  state.trip.distanceKm = 0;

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
  } else {
    alert("Geolocation is not supported by your browser.");
  }

  speakVoice(`SpeedCheck active. Limit set to ${state.effectiveLimitKmh} kilometers per hour.`);
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
  state.activeZone = null;
  state.distanceToCameraMeters = null;
  state.lastPosition = null;

  updateUI();
}

// 10. UI Rendering & Gauge Control
function updateUI() {
  const speed = Math.round(state.currentSpeedKmh);
  const limit = state.effectiveLimitKmh;

  // Digital Number
  const speedNumberEl = document.getElementById('speedNumber');
  if (speedNumberEl) speedNumberEl.textContent = speed;

  // Gauge Arc Progress (stroke-dasharray = 440)
  const maxDisplaySpeed = 160;
  const ratio = Math.min(1, speed / maxDisplaySpeed);
  const offset = 440 - (ratio * 440);
  const gaugeEl = document.getElementById('gaugeProgress');

  // Status and Gauge Color States
  const badgeEl = document.getElementById('statusBadge');
  const badgeDot = document.getElementById('statusDot');
  const badgeText = document.getElementById('statusText');

  let alertState = 'normal';
  const approachingThreshold = Math.max(1, limit - state.approachingOffsetKmh);

  if (speed >= limit) {
    alertState = 'exceeded';
  } else if (speed >= approachingThreshold) {
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
      badgeText.textContent = "Monitoring Active";
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
        radarStatus.innerHTML = `<strong>📍 Camera Ahead:</strong> ${state.activeZone.name} (${d}m) • Limit: ${state.activeZone.speedLimitKmh} km/h`;
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

  // Mute Button Icon
  const muteBtn = document.getElementById('muteBtn');
  if (muteBtn) {
    muteBtn.innerHTML = state.voiceAlertsEnabled ? '🔊' : '🔇';
    muteBtn.title = state.voiceAlertsEnabled ? 'Mute Voice Alerts' : 'Unmute Voice Alerts';
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

  // Mute Button
  const muteBtn = document.getElementById('muteBtn');
  if (muteBtn) {
    muteBtn.addEventListener('click', () => {
      state.voiceAlertsEnabled = !state.voiceAlertsEnabled;
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
      speakVoice("Speed camera zone ahead on N1 Highway Fiesta Royale. Limit is 80 kilometers per hour. Caution, please slow down.");
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
