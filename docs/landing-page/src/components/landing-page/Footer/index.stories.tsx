import type { Meta, StoryObj } from "@storybook/react-vite";

import { Footer } from "./";

const meta = {
  title: "Smithy/LandingPage/Footer",
  component: Footer,
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof Footer>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
