// Phases 5A / 5C-1: ELEGOO MEGA 2560 USB serial LED and active-buzzer test.
#include <Arduino.h>
#include <string.h>

const unsigned long BAUD_RATE = 9600;
const size_t BUFFER_SIZE = 32;  // 31 command characters + terminating zero.
const unsigned long BLINK_INTERVAL_MS = 250;
const byte BUZZER_PIN = 8;  // Drives the transistor base THROUGH its resistor.
const uint32_t BUZZ_DURATION_MS = 180;

bool buzzerActive = false;
uint32_t buzzStartedAt = 0;
bool blinkActive = false;
byte blinkPhase = 0;
uint32_t blinkChangedAt = 0;

char commandBuffer[BUFFER_SIZE];
size_t commandLength = 0;
bool lineTooLong = false;
bool invalidCharacter = false;

void setLed(bool on) {
  digitalWrite(LED_BUILTIN, on ? HIGH : LOW);
}

void startBlink() {
  // Establish an OFF interval so three flashes are visible even if already ON.
  setLed(false);
  blinkPhase = 0;
  blinkChangedAt = millis();
  blinkActive = true;
}

void updateOutputs() {
  const uint32_t now = millis();
  // Unsigned subtraction remains correct when millis() rolls over.
  if (buzzerActive && uint32_t(now - buzzStartedAt) >= BUZZ_DURATION_MS) {
    digitalWrite(BUZZER_PIN, LOW);
    buzzerActive = false;
  }
  if (blinkActive && uint32_t(now - blinkChangedAt) >= BLINK_INTERVAL_MS) {
    blinkChangedAt = now;
    ++blinkPhase;
    if (blinkPhase == 7) {
      blinkActive = false;
      Serial.println(F("ACK BLINK")); // Same completion reply as Phase 5A.
    } else {
      setLed((blinkPhase % 2) == 1);
    }
  }
}

void handleCommand(const char* command) {
  if (strcmp(command, "PING") == 0) {
    Serial.println(F("PONG"));
  } else if (strcmp(command, "LED_ON") == 0) {
    if (blinkActive) { Serial.println(F("ERR BUSY_BLINK")); return; }
    setLed(true);
    Serial.println(F("ACK LED_ON"));
  } else if (strcmp(command, "LED_OFF") == 0) {
    if (blinkActive) { Serial.println(F("ERR BUSY_BLINK")); return; }
    setLed(false);
    Serial.println(F("ACK LED_OFF"));
  } else if (strcmp(command, "BLINK") == 0) {
    if (blinkActive) { Serial.println(F("ERR BUSY_BLINK")); return; }
    startBlink();
  } else if (strcmp(command, "BUZZ") == 0) {
    if (buzzerActive) { Serial.println(F("ERR BUSY_BUZZ")); return; }
    buzzStartedAt = millis();
    buzzerActive = true;
    digitalWrite(BUZZER_PIN, HIGH);
    Serial.println(F("ACK BUZZ")); // Accepted/started, not a completion message.
  } else {
    Serial.println(F("ERR UNKNOWN_COMMAND"));
  }
}

void finishLine() {
  if (lineTooLong) {
    Serial.println(F("ERR LINE_TOO_LONG"));
  } else if (invalidCharacter) {
    Serial.println(F("ERR INVALID_CHARACTER"));
  } else if (commandLength > 0) {
    commandBuffer[commandLength] = '\0';
    handleCommand(commandBuffer);
  }
  // Empty lines are ignored. Start fresh after every newline, including errors.
  commandLength = 0;
  lineTooLong = false;
  invalidCharacter = false;
}

void readSerialCommands() {
  // Bound work per loop so a long stream cannot starve output timers.
  byte processed = 0;
  while (Serial.available() > 0 && processed < 32) {
    updateOutputs();
    ++processed;
    const char incoming = static_cast<char>(Serial.read());
    if (incoming == '\n') {
      finishLine();
    } else if (incoming == '\r') {
      // Ignore CR so both LF and Windows CRLF endings work.
      continue;
    } else if (lineTooLong || invalidCharacter) {
      // Discard the rest of a bad line; never execute a truncated command.
      continue;
    } else if (incoming < ' ' || incoming > '~') {
      invalidCharacter = true;
    } else if (commandLength < BUFFER_SIZE - 1) {
      commandBuffer[commandLength++] = incoming;
    } else {
      lineTooLong = true;
    }
  }
}

void setup() {
  // Set the output latch LOW before enabling D8; external pull-down also
  // keeps the transistor off during the bootloader/reset high-impedance period.
  digitalWrite(BUZZER_PIN, LOW);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  setLed(false);
  Serial.begin(BAUD_RATE);
}

void loop() {
  updateOutputs();
  readSerialCommands();
}
