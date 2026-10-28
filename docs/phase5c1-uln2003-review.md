# Phase 5C-1: ULN2003 alternative — archived, not selected

**Archived research only.** The finalized project retains the transistor design as **Designed — Hardware Validation Pending**. No further module investigation, photographs, purchase or wiring action is requested. The historical assessment below is not the selected implementation.

## Historical assessment

A ULN2003A channel is electrically suitable in principle for switching the documented ELEGOO kit active buzzer. The user's exact module has not been photographed or identified, so this is a circuit review, not an approved physical wiring map. Do not infer motor socket positions from another board's picture. No multimeter purchase is needed for the next step: provide clear, unpowered front and back photographs, including all labels, chip marking, jumper, power connector and five-pin motor connector.

## Evidence

The [official ELEGOO kit download](https://www.elegoo.com/blogs/arduino-projects/elegoo-mega-2560-the-most-complete-starter-kit-tutorial), version V1.0.2023.05.05, contains `##Datasheet/Lesson 31 ULN2003A Stepper motor driver.pdf`. Its illustrated board has IN1–IN4 inputs, motor outputs A–D and V+, a motor-power jumper, and +/− power connections. It specifies 5–12 V supply and up to 500 mA per output. This identifies the documented board, not necessarily the user's revision. Use only 5 V for this buzzer.

The kit's [active buzzer datasheet](phase5c1-elegoo-active-buzzer-datasheet.pdf) specifies nominal 5 V, operating 3.5–5.5 V and current less than 25 mA. That does not establish compliance with a 20 mA direct GPIO limit.

The [TI ULN2003A datasheet](https://www.ti.com/lit/ds/symlink/uln2003a.pdf) specifies low-side Darlington outputs, 500 mA single-output capability, 2.7 kΩ input resistors for TTL/5 V CMOS, and common-cathode clamp diodes. The board carries the buzzer current; D8 supplies only control current. The 500 mA figure is not a simultaneous all-channel allowance. A Darlington has a voltage drop, so the buzzer receives less than the supply voltage and may be quieter. Operation at the buzzer's minimum voltage must not be guaranteed under all USB supply conditions.

## Functional circuit only — no physical socket positions assigned

```text
Mega D8 -------- verified input IN1
Mega GND ------ verified module GND
Mega 5V ------- buzzer (+)
          +---- verified clamp COM / positive motor supply path
Buzzer (−) ---- verified output corresponding to IN1
```

HIGH on IN1 makes the corresponding output sink current to GND, so the existing D8 HIGH pulse remains compatible. The chip's COM is the clamp-diode common, not ground and not a logic supply. On a module its connection may pass through a jumper; that path must be established before relying on the internal flyback protection. Do not connect the buzzer to an input, an arbitrary motor-header contact, or 12 V. Do not connect a stepper motor during this test.

The ULN2003A includes input resistors, so a separate transistor/base resistor is not normally needed. A separate flyback diode is unnecessary only when the correct output and the internal clamp path to +5 V have been verified. We have not assigned jumper position or physical header order. Connector compatibility also needs checking; do not force loose wires into the motor socket.

## If a different module is needed

A documented inexpensive alternative is the [ADIY Active Buzzer Module A149686](https://adiy.in/shop/active-buzzer-module/). Its [manufacturer datasheet](https://adiy.in/wp-content/uploads/2022/09/A149686_ADIY-Active-Buzzer-Module_Datasheet.pdf) specifies 3.3–5 V, an integrated 8550 transistor, and VCC/GND/I/O interfaces. The manufacturer listed ₹25 before shipping when checked; Canadian delivered cost/availability is not established. Buy that documented part rather than assuming all generic three-pin buzzers share its circuit.

The datasheet does not establish trigger polarity explicitly. Verify polarity before substituting it: a LOW-triggered module would require an isolated firmware output-polarity change to preserve the BUZZ serial interface and silent idle state. No such change has been made.

## Software and verification

No Arduino, Java or Python source changed for this review. Existing PING, LED_ON, LED_OFF, BLINK and BUZZ remain intact. BUZZ acknowledges acceptance with ACK BUZZ and pulses D8 HIGH for 180 ms. Existing upload/manual test instructions remain in [the Phase 5C-1 guide](phase5c1-active-buzzer.md).

Performed: reviewed the kit module PDF and its connector illustration, checked the ULN2003A manufacturer specifications and the alternative module's manufacturer datasheet. No physical wiring, voltage/current measurement, upload or buzzer test was performed. No software tests were rerun for this documentation-only change. Physical testing remains pending board identification and final wiring approval based on that evidence.
