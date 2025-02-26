import React from "react";
import { useSSR, I18nextProvider } from "react-i18next";
import i18n from "@/i18n/ssr";
import path from "path";
import fs from "node:fs/promises";
import { renderToString } from "react-dom/server";
import App from "@/App";

const red = "\x1b[31m";
const green = "\x1b[32m";
const yellow = "\x1b[33m";
const lightGray = "\x1b[90m";
const reset = "\x1b[0m";

// get app root directory
const root = path.dirname(import.meta.dirname);
const distDir = path.resolve(root, "dist");
const indexhtml = path.resolve(distDir, "index.html");

console.log("Rendering the static html...");

await checkAssetsExist();
const html = await fs.readFile(indexhtml, "utf8");

const ssgHtml = html.replace(
  `<div id="root"></div>`,
  `<div id="root">${renderToString(
    <I18nextProvider i18n={i18n}>
      <App />
    </I18nextProvider>,
  )}</div>`,
);

// Write index.html for consumption
await fs.writeFile(path.resolve(distDir, "index.html"), ssgHtml);

// Output filesizes to make it easier to see if the first load is too big
console.log("\nFile sizes:\n");
let estimatedFirstloadSize: number = 0;

const assets = await fs.readdir(path.resolve(distDir, "assets"));
const indexSize = await getFileSizeInkB(path.resolve(distDir, "index.html"));

await Promise.all(
  assets.map(async (asset) => {
    const assetPath = path.resolve(distDir, "assets", asset);
    const assetSize = await getFileSizeInkB(assetPath);
    estimatedFirstloadSize += Number(assetSize);
    let color = green;
    if (assetSize > 100) {
      color = yellow;
    }
    if (assetSize > 400) {
      color = red;
    }
    console.log(
      `${lightGray}dist/assets/${color}${asset} - ${assetSize}kB${reset}`,
    );
  }),
);

estimatedFirstloadSize += Number(indexSize);
console.log(`dist/index.html - ${indexSize}kB`);
if (estimatedFirstloadSize > 500) {
  console.log(
    `\n${yellow}WARNING: Estimated first load size is over 500kB. This may cause performance issues.${reset}`,
  );
}
console.log(
  `\nEstimated first load size: ${estimatedFirstloadSize.toFixed(2)}kB`,
);

async function getFileSizeInkB(string: string) {
  const stats = await fs.stat(string);
  return parseFloat((stats.size / 1000).toFixed(2)); // using 1000 mimics Vite's output size
}

// throw a more useful error if the project hasn't been built
async function checkAssetsExist() {
  try {
    await fs.stat(indexhtml);
  } catch (e) {
    throw new Error(
      `Index not found: ${indexhtml}, have you built the project yet?`,
    );
  }
}
