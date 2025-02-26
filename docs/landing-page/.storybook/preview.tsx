import React from "react";
import type { Preview } from "@storybook/react";
import { withThemeByClassName } from "@storybook/addon-themes";
import "../src/i18n/client.tsx";
import "../src/index.css";

const preview: Preview = {
  parameters: {
    backgrounds: { disable: true },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
};

export const decorators = [
  withThemeByClassName({
    themes: {
      // light: "light",
      dark: "dark",
    },
    defaultTheme: "dark",
  }),
  // (Story) => <I18nextProvider i18n={i18n}>
  //   <Story />
  // </I18nextProvider>,
];

export default preview;
