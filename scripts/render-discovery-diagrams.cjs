// Run from any directory with sharp and playwright available through Node resolution.
const fs = require('node:fs/promises');
const path = require('node:path');
const { existsSync } = require('node:fs');
const sharp = require('sharp');
const { chromium } = require('playwright');

const names = ['discovery-architecture', 'discovery-search-flow',
  'discovery-matching-flow', 'discovery-transaction-sequence'];
const directory = path.resolve(__dirname, '../docs/images');

async function main() {
  const edge = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
  const executablePath = process.env.DIAGRAM_BROWSER || (existsSync(edge) ? edge : undefined);
  const browser = await chromium.launch({ headless: true, executablePath });
  try {
    const page = await browser.newPage();
    for (const name of names) {
      const source = await fs.readFile(path.join(directory, `${name}.svg`), 'utf8');
      await page.setContent(source);
      await page.evaluate(() => document.fonts.ready);
      const violations = await page.evaluate(() => {
        const svg = document.querySelector('svg');
        const frame = svg.viewBox.baseVal;
        const boxes = [...svg.querySelectorAll('rect')]
          .filter(rect => !rect.classList.contains('bg')).map(rect => rect.getBBox());
        return [...svg.querySelectorAll('text')].flatMap(text => {
          const bounds = text.getBBox();
          const x = text.x.baseVal.getItem(0).value;
          const y = text.y.baseVal.getItem(0).value;
          const box = boxes.find(box => x >= box.x && x <= box.x + box.width
            && y >= box.y && y <= box.y + box.height) || frame;
          return bounds.x < box.x + 8 || bounds.y < box.y + 4
            || bounds.x + bounds.width > box.x + box.width - 8
            || bounds.y + bounds.height > box.y + box.height - 4
            ? [text.textContent] : [];
        });
      });
      if (violations.length) throw new Error(`${name}: overflowing text: ${violations.join(' | ')}`);
      const output = path.join(directory, `${name}.png`);
      await sharp(Buffer.from(source)).png().toFile(output);
      const metadata = await sharp(output).metadata();
      const stats = await sharp(output).stats();
      if (!metadata.width || !metadata.height || stats.channels.every(channel => channel.stdev < 1)) {
        throw new Error(`${name}: empty or blank image`);
      }
      console.log(`${name}.png ${metadata.width}x${metadata.height}: text bounds and nonblank pixels PASS`);
    }
  } finally {
    await browser.close();
  }
}

main().catch(error => { console.error(error.message); process.exitCode = 1; });
