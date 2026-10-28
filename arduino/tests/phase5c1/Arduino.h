// Test-only Arduino API; never included by the real Mega sketch build.
#pragma once
#include <stdint.h>
#include <stddef.h>
#include <string.h>
typedef uint8_t byte;
#define HIGH 1
#define LOW 0
#define OUTPUT 1
#define LED_BUILTIN 13
#define F(text) text
extern uint32_t testMillis;
extern byte testPins[16];
inline uint32_t millis() { return testMillis; }
inline void pinMode(byte, byte) {}
inline void digitalWrite(byte pin, byte value) { testPins[pin] = value; }
struct TestSerial {
  const char* input = "";
  size_t position = 0;
  char output[160] = {};
  unsigned long baud = 0;
  void begin(unsigned long value) { baud = value; }
  int available() { return input[position] != '\0'; }
  int read() { return input[position++]; }
  void println(const char* text) {
    if (strlen(output) + strlen(text) + 2 < sizeof(output)) {
      strcat(output, text); strcat(output, "\r\n");
    }
  }
};
extern TestSerial Serial;
