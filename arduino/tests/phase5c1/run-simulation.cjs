// Usage: node run-simulation.cjs <avr8js package directory> <test_sketch.bin>
const fs = require('node:fs');
const path = require('node:path');
const { CPU, avrInstruction } = require(path.resolve(process.argv[2]));
const flash = new Uint8Array(32768);
flash.set(fs.readFileSync(process.argv[3]));
const cpu = new CPU(new Uint16Array(flash.buffer));
for (let i = 0; i < 2000000; i++) {
  avrInstruction(cpu);
  cpu.tick();
  if (cpu.data[0x25] === 0xee) throw new Error(`Sketch check ${cpu.data[0x28]} failed`);
  if (cpu.data[0x25] === 0xa5) {
    console.log(`PASS: ${cpu.data[0x28]} actual-sketch checks with mocked time/serial/pins on AVR8js`);
    process.exit(0);
  }
}
throw new Error('Simulation did not finish within instruction limit');
