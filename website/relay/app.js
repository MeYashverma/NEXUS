/*
 * Elevon Relay — browser side.
 *
 * Connects to the Elevon app over Web Bluetooth (GATT client), performs an
 * ECDH P-256 handshake with AES-GCM session encryption, and sends typed keys
 * to the phone. Scope honesty: keys are only captured from the focused type
 * box on this page — never the system-wide keyboard.
 *
 * Protocol (matches app/elevon/relay/RelayManager.kt):
 *   service  f3a1e0c2-6b7d-4c8a-9e2f-0a1b2c3d4e5f
 *   input    f3a1e0c3-…  (write / write-without-response, laptop → phone)
 *   status   f3a1e0c4-…  (read/notify, phone → laptop)
 *   frame 0x10 = hello: 0x10 || rawPublicKey(65), chunkable at 20 bytes
 *   frame 0x11 = data:  0x11 || nonce(12) || AES-GCM(payload)
 *   payload 0x01 text | 0x02 backspace(n) | 0x03 enter | 0x04 end
 *   session key = HKDF-SHA256(ECDH, salt="elevon-relay-v1", info="session")
 *   code = first 5 digits of SHA-256(sorted public keys) mod 100000
 */
(() => {
  "use strict";

  const SERVICE_UUID = "f3a1e0c2-6b7d-4c8a-9e2f-0a1b2c3d4e5f";
  const CHAR_INPUT = "f3a1e0c3-6b7d-4c8a-9e2f-0a1b2c3d4e5f";
  const HKDF_SALT = new TextEncoder().encode("elevon-relay-v1");
  const HKDF_INFO = new TextEncoder().encode("session");

  const WRITE_CHUNK = 20; // safe ATT payload floor; larger MTUs just mean fewer writes

  const $ = (id) => document.getElementById(id);
  const state = {
    device: null,
    server: null,
    inputChar: null,
    keyPair: null,
    phonePub: null,   // Uint8Array(65)
    sessionKey: null, // CryptoKey AES-GCM
    active: false,
    paused: false,
    queue: Promise.resolve(),
    textBuffer: [],
    flushTimer: null,
  };

  // ------------------------------------------------------------------ UI --
  function setStatus(text, kind) {
    $("status-text").textContent = text;
    $("status-dot").className = "dot " + (kind || "");
  }
  function pill(id, ok, okText, badText) {
    const el = $(id);
    el.textContent = ok ? okText : badText;
    el.className = "pill " + (ok ? "ok" : "bad");
  }
  function banner(on, text) {
    const b = $("relay-banner");
    b.classList.toggle("on", on);
    if (text) $("relay-banner-text").textContent = text;
  }
  function showSection(id, on) {
    $(id).classList.toggle("relay-hidden", !on);
  }

  function refreshUi() {
    const hasBt = !!navigator.bluetooth;
    pill("pill-browser", hasBt, "Web Bluetooth ✓", "Web Bluetooth ✗");
    pill("pill-gatt", !!state.server, "Paired ✓", "Not paired");
    pill("pill-crypto", !!state.sessionKey, "Encrypted ✓", "Handshake pending");
    $("device-name").textContent = state.device?.name || "—";
    showSection("connect-panel", !state.active);
    showSection("session-panel", state.active);
    banner(state.active && !state.paused,
      state.paused ? "RELAY PAUSED — return to this tab to resume"
                   : `RELAY ACTIVE — connected to: ${state.device?.name || "your phone"}`);
    if (!hasBt) {
      setStatus("This browser can't use Web Bluetooth", "");
      showSection("browser-help", true);
    }
  }

  // -------------------------------------------------------------- crypto --
  async function generateCode(a, b) {
    const [first, second] =
      compareBytes(a, b) > 0 ? [b, a] : [a, b];
    const digest = new Uint8Array(
      await crypto.subtle.digest("SHA-256", concat(first, second)),
    );
    let value = 0n;
    for (let i = 0; i < 8; i++) value = (value << 8n) | BigInt(digest[i]);
    return String(Number(value % 100000n)).padStart(5, "0");
  }
  const compareBytes = (a, b) => {
    for (let i = 0; i < Math.min(a.length, b.length); i++) {
      if (a[i] !== b[i]) return a[i] - b[i];
    }
    return a.length - b.length;
  };
  const concat = (a, b) => {
    const out = new Uint8Array(a.length + b.length);
    out.set(a, 0);
    out.set(b, a.length);
    return out;
  };

  async function deriveSessionKey(phonePubRaw) {
    const [pubRaw, privateKey] = await exportOwnPublicKey();
    const phoneKey = await crypto.subtle.importKey(
      "raw", phonePubRaw, { name: "ECDH", namedCurve: "P-256" }, false, [],
    );
    const shared = new Uint8Array(
      await crypto.subtle.deriveBits({ name: "ECDH", public: phoneKey }, privateKey, 256),
    );
    const hkdfKey = await crypto.subtle.importKey("raw", shared, "HKDF", false, ["deriveBits"]);
    const bits = new Uint8Array(await crypto.subtle.deriveBits(
      { name: "HKDF", hash: "SHA-256", salt: HKDF_SALT, info: HKDF_INFO },
      hkdfKey, 256,
    ));
    state.phonePub = phonePubRaw;
    state.sessionKey = await crypto.subtle.importKey("raw", bits, "AES-GCM", false, ["encrypt"]);
    $("pair-code").textContent = await generateCode(pubRaw, phonePubRaw);
  }

  async function exportOwnPublicKey() {
    if (!state.keyPair) {
      state.keyPair = await crypto.subtle.generateKey(
        { name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"],
      );
    }
    const raw = new Uint8Array(await crypto.subtle.exportKey("raw", state.keyPair.publicKey));
    return [raw, state.keyPair.privateKey];
  }

  // ------------------------------------------------------------ transport --
  async function writeChunked(bytes) {
    for (let i = 0; i < bytes.length; i += WRITE_CHUNK) {
      await state.inputChar.writeValueWithoutResponse(bytes.slice(i, i + WRITE_CHUNK));
    }
  }

  async function sendEncrypted(payload) {
    const nonce = crypto.getRandomValues(new Uint8Array(12));
    const ct = new Uint8Array(
      await crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce }, state.sessionKey, payload),
    );
    const frame = new Uint8Array(1 + 12 + ct.length);
    frame[0] = 0x11;
    frame.set(nonce, 1);
    frame.set(ct, 13);
    if (frame.length > WRITE_CHUNK) {
      // Cap payload sizes at the call sites so this stays within one write.
      throw new Error("frame too large for safe MTU");
    }
    await state.inputChar.writeValueWithoutResponse(frame);
  }

  function enqueueText(text) {
    const bytes = new TextEncoder().encode(text);
    // Chunk to ≤5 payload bytes per frame: frame = 1 + 12 + GCM tag(16) + n ≤ 20.
    for (let i = 0; i < bytes.length; i += 5) {
      state.textBuffer.push(bytes.slice(i, i + 5));
    }
    scheduleFlush();
  }

  function scheduleFlush(immediate = false) {
    if (state.flushTimer) clearTimeout(state.flushTimer);
    state.flushTimer = setTimeout(flushText, immediate ? 0 : 60);
  }

  async function flushText() {
    if (!state.active || state.paused || state.textBuffer.length === 0) return;
    const batch = state.textBuffer.splice(0, state.textBuffer.length);
    state.queue = state.queue.then(async () => {
      for (const chunk of batch) {
        if (!state.active || state.paused) return;
        const payload = new Uint8Array(1 + chunk.length);
        payload[0] = 0x01;
        payload.set(chunk, 1);
        try {
          await sendEncrypted(payload);
        } catch (err) {
          console.warn("send failed", err);
          setStatus("Connection lost — stop and reconnect", "waiting");
          stopRelay();
          return;
        }
      }
    });
  }

  // ------------------------------------------------------------- session --
  async function startRelay() {
    if (!navigator.bluetooth) {
      showSection("browser-help", true);
      return;
    }
    try {
      setStatus("Choose your phone in the browser picker…", "waiting");
      const device = await navigator.bluetooth.requestDevice({
        filters: [{ services: [SERVICE_UUID] }],
        optionalServices: [SERVICE_UUID],
      });
      state.device = device;
      device.addEventListener("gattserverdisconnected", () => {
        if (state.active) {
          setStatus("Phone disconnected", "waiting");
          stopRelay();
        }
      });
      setStatus("Connecting…", "waiting");
      state.server = await device.gatt.connect();
      const service = await state.server.getPrimaryService(SERVICE_UUID);
      state.inputChar = await service.getCharacteristic(CHAR_INPUT);
      refreshUi();

      setStatus("Exchanging keys…", "waiting");
      await handshake();
      state.active = true;
      state.paused = false;
      setStatus("Relay active — type below", "active");
      refreshUi();
      $("type-box").focus();
    } catch (err) {
      console.warn(err);
      const msg = err?.name === "NotFoundError"
        ? "No phone selected (or the phone isn't advertising — start Relay there first)."
        : "Couldn't connect: " + (err?.message || err);
      setStatus(msg, "waiting");
      refreshUi();
    }
  }

  async function handshake() {
    // Send our public key; the phone replies with its own via notifications
    // on the status characteristic, which we read after a short settle.
    const [pubRaw] = await exportOwnPublicKey();
    const hello = new Uint8Array(66);
    hello[0] = 0x10;
    hello.set(pubRaw, 1);
    await writeChunked(hello);

    const service = await state.server.getPrimaryService(SERVICE_UUID);
    const statusChar = await service.getCharacteristic(
      "f3a1e0c4-6b7d-4c8a-9e2f-0a1b2c3d4e5f",
    );
    const value = await statusChar.readValue();
    if (value.length >= 66 && value[0] === 0x10) {
      await deriveSessionKey(new Uint8Array(value.slice(1, 66)));
    } else {
      // Fall back: poll briefly while the phone finalises registration.
      for (let attempt = 0; attempt < 10 && value.length < 66; attempt++) {
        await new Promise((r) => setTimeout(r, 300));
        const retry = await statusChar.readValue();
        if (retry.length >= 66 && retry[0] === 0x10) {
          await deriveSessionKey(new Uint8Array(retry.slice(1, 66)));
          break;
        }
      }
    }
    if (!state.sessionKey) throw new Error("handshake incomplete");
  }

  function stopRelay() {
    state.active = false;
    if (state.flushTimer) clearTimeout(state.flushTimer);
    if (state.sessionKey && state.inputChar) {
      const payload = new Uint8Array([0x04]);
      const nonce = crypto.getRandomValues(new Uint8Array(12));
      state.queue = state.queue
        .then(() =>
          crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce }, state.sessionKey, payload),
        )
        .then((ct) => {
          const frame = new Uint8Array(13 + ct.byteLength);
          frame[0] = 0x11;
          frame.set(nonce, 1);
          frame.set(new Uint8Array(ct), 13);
          return state.inputChar.writeValueWithoutResponse(frame);
        })
        .catch(() => {});
    }
    state.queue.then(() => {
      try { state.server?.disconnect(); } catch (_) {}
    });
    state.sessionKey = null;
    state.textBuffer = [];
    $("pair-code").textContent = "·····";
    $("type-box").value = "";
    setStatus("Relay stopped. Nothing is being sent.", "");
    refreshUi();
  }

  // ------------------------------------------------------------- capture --
  function setupCapture() {
    const box = $("type-box");
    box.addEventListener("keydown", (e) => {
      if (!state.active || state.paused) return;
      if (e.key === "Backspace") {
        e.preventDefault();
        state.queue = state.queue.then(() =>
          sendEncrypted(new Uint8Array([0x02, 1])),
        ).catch(() => {});
        return;
      }
      if (e.key === "Enter") {
        e.preventDefault();
        state.queue = state.queue.then(() =>
          sendEncrypted(new Uint8Array([0x03])),
        ).catch(() => {});
        return;
      }
      if (e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) {
        e.preventDefault();
        enqueueText(e.key);
      }
    });
    box.addEventListener("paste", (e) => {
      if (!state.active || state.paused) return;
      e.preventDefault();
      const text = e.clipboardData?.getData("text") || "";
      if (text) enqueueText(text);
    });
    document.addEventListener("visibilitychange", () => {
      if (!state.active) return;
      state.paused = document.hidden;
      refreshUi();
      if (!state.paused) $("type-box").focus();
    });
    window.addEventListener("beforeunload", (e) => {
      if (state.active) {
        stopRelay();
        e.preventDefault();
        e.returnValue = "";
      }
    });
  }

  // ---------------------------------------------------------------- init --
  document.addEventListener("DOMContentLoaded", () => {
    $("connect-btn").addEventListener("click", startRelay);
    $("stop-btn").addEventListener("click", stopRelay);
    $("send-box-btn").addEventListener("click", () => {
      const text = $("paste-box").value;
      if (text && state.active && !state.paused) {
        enqueueText(text);
        $("paste-box").value = "";
      }
    });
    setupCapture();
    refreshUi();
  });
})();
