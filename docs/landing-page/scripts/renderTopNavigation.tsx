import React from "react";
import path from "path";
import fs from "node:fs/promises";
import { I18nextProvider } from "react-i18next";
import i18n from "../src/i18n/ssr";
import { renderToString } from "react-dom/server";

import { TopNavigation } from "../src/components/navigation";
// get app root directory
const root = path.dirname(import.meta.dirname);
const distDir = path.resolve(root, "dist");
const indexhtml = path.resolve(distDir, "index.html");

console.log("Rendering the navigation-specific html...");

await checkAssetsExist();
const html = await fs.readFile(indexhtml, "utf8");
const { styles, scripts } = await getScriptAndStyleTagContents(html);

// add the found styles and scripts to the top of the nav-specific output
const topNavigationHtmlString =
  [...styles, ...scripts].join("\n") +
  "\n" +
  renderToString(
    <I18nextProvider i18n={i18n}>
      <TopNavigation />
    </I18nextProvider>,
  );

// Write the navbar file out for consumption
fs.writeFile(path.resolve(distDir, "nav.html"), topNavigationHtmlString);

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

// Extract the generated script and style tags, this should significantly
// improve page load speed on subsequent loads
function getScriptAndStyleTagContents(html: string): {
  scripts: string[];
  styles: string[];
} {
  const scriptTagRegex = /<script[^>]*>([\s\S]*?)<\/script>/gi;
  const styleTagRegex = /<link[^>]*rel="stylesheet"[^>]*href="([^"]*)"[^>]*>/gi;

  const scripts: string[] = [];
  const styles: string[] = [];

  let match: RegExpExecArray | null;

  while ((match = scriptTagRegex.exec(html))) {
    scripts.push(match[0].trim());
  }

  while ((match = styleTagRegex.exec(html))) {
    styles.push(match[0].trim());
  }

  return { scripts, styles };
}
