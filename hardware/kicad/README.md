# Phase 6A editable schematic

Open `guess-who-hardware.kicad_sch` in KiCad 8 or newer Schematic Editor. Symbols are embedded; no external library or footprint is required. This is a standalone schematic, not a PCB project.

**DESIGNED — HARDWARE VALIDATION PENDING.** The installed environment has no detected KiCad executable. Opening in KiCad and ERC have **not** been performed. The generated file has passed only text-structure and intended wire-graph checks; native application compatibility remains to be checked.

U1 is an explicitly reduced Mega board interface containing only the relevant 5V, GND and D8 connections. D13/LED_BUILTIN is described as onboard, not duplicated as an external LED. Q1 represents the PN2222/2N2222A-family NPN electrical function. Its logical pin identifiers are B, C and E, not numbered package pins. The user's part is marked `2N2222A 331`; its manufacturer and physical pin order remain unverified. No footprint or physical package mapping is supplied, and symbols are excluded from PCB placement.

R1 is 470 Ω between D8 and B. R2 is 10 kΩ from B to ground. Buzzer + and D1 cathode K connect to 5V. Buzzer −, D1 anode A and Q1 collector C share the switched node. Q1 emitter E connects to common ground. See `intended-nets.json` for a machine-readable reference, not a KiCad-exported netlist.

The WS2812B extension appears only as a separate proposal note: no parts purchased, no selected supply, no assigned pin and no electrical connection to this circuit. No PCB is designed.

## Remaining native validation

1. Open the schematic in KiCad 8 or newer; confirm symbols, names, values, connections, annotations and sheet layout.
2. Use Inspect → Electrical Rules Checker, run ERC, and record its actual report. Resolve findings without assigning an unsupported transistor footprint or pin order.
3. Compare all five nets with `intended-nets.json`; check the transistor E/B/C functions and diode K/A polarity.
4. Export SVG/PDF from KiCad if needed. The repository's current SVG/PNG drawings are separately rendered documentation, **not verified KiCad exports**.

ERC cannot verify the unidentified physical transistor or demonstrate that a buzzer works. Keep any display assembly unpowered. Hardware validation is deferred, not a completed phase claim.

File generation follows the [KiCad schematic format documentation](https://dev-docs.kicad.org/en/file-formats/sexpr-schematic/). See `validation-report.txt` for the checks actually performed.
