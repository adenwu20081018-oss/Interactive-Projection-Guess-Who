// Executes the actual .ino with fake serial/pins/time on an emulated AVR CPU.
#include "Arduino.h"
#include <avr/io.h>
uint32_t testMillis = 0;
byte testPins[16] = {};
TestSerial Serial;
#include "../../serial_test/serial_test.ino"

static byte checks = 0;
void check(bool condition) {
  ++checks;
  PORTC = checks;
  if (!condition) { PORTB = 0xee; while (true) {} }
}
void send(const char* command) {
  Serial.output[0] = '\0';
  Serial.input = command; Serial.position = 0;
  while (Serial.available()) loop();
}
void at(uint32_t time) { testMillis = time; loop(); }
bool reply(const char* expected) { return strcmp(Serial.output, expected) == 0; }

int main() {
  setup();
  check(Serial.baud == 9600 && testPins[8] == LOW && testPins[13] == LOW);
  send("PING\r\n"); check(reply("PONG\r\n"));
  send("LED_ON\n"); check(testPins[13] == HIGH && reply("ACK LED_ON\r\n"));
  send("LED_OFF\n"); check(testPins[13] == LOW && reply("ACK LED_OFF\r\n"));
  send("BUZZ\r\n"); check(testPins[8] == HIGH && reply("ACK BUZZ\r\n"));
  at(179); check(testPins[8] == HIGH);
  send("BUZZ\n"); check(reply("ERR BUSY_BUZZ\r\n"));
  at(180); check(testPins[8] == LOW && !buzzerActive); // Duplicate did not extend beep.
  send("BUZZ\nPING\n"); check(reply("ACK BUZZ\r\nPONG\r\n") && testPins[8] == HIGH);
  at(360); check(testPins[8] == LOW && reply("ACK BUZZ\r\nPONG\r\n")); // No extra reply.
  send("BLINK\n"); check(blinkActive && testPins[13] == LOW && reply(""));
  send("BUZZ\n"); check(reply("ACK BUZZ\r\n") && testPins[8] == HIGH);
  send("PING\n"); check(reply("PONG\r\n"));
  at(540); check(testPins[8] == LOW && blinkActive); // Blink cannot stretch BUZZ.
  at(610); check(testPins[13] == HIGH);
  send("LED_ON\nLED_OFF\nBLINK\n");
  check(reply("ERR BUSY_BLINK\r\nERR BUSY_BLINK\r\nERR BUSY_BLINK\r\n"));
  Serial.output[0] = '\0';
  at(860); check(testPins[13] == LOW);
  at(1110); check(testPins[13] == HIGH);
  at(1360); check(testPins[13] == LOW);
  at(1610); check(testPins[13] == HIGH);
  at(1860); check(testPins[13] == LOW && reply(""));
  at(2110); check(!blinkActive && testPins[13] == LOW && reply("ACK BLINK\r\n"));
  send("LED_ON\n"); check(reply("ACK LED_ON\r\n") && testPins[13] == HIGH);
  send("HELLO\n"); check(reply("ERR UNKNOWN_COMMAND\r\n") && !buzzerActive);
  send("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456\nPING\n"); check(reply("ERR LINE_TOO_LONG\r\nPONG\r\n"));
  send("BUZZ\t\nPING\n"); check(reply("ERR INVALID_CHARACTER\r\nPONG\r\n"));
  send("BUZZ"); check(reply("") && !buzzerActive);
  send("\n"); check(reply("ACK BUZZ\r\n") && buzzerActive);
  at(2290);
  send("\r\n\n"); check(reply(""));
  testMillis = UINT32_MAX - 100;
  send("BUZZ\n"); at(78); check(testPins[8] == HIGH);
  at(79); check(testPins[8] == LOW); // 180 ms across uint32 wrap.
  testMillis = UINT32_MAX - 100;
  send("BLINK\n"); at(149); check(testPins[13] == HIGH); // Blink also wraps safely.
  PORTB = 0xa5;
  while (true) {}
}
