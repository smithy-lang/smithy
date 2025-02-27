import type { Meta, StoryObj } from "@storybook/react";
import { fn } from "@storybook/test";

import { Features } from ".";

const meta = {
  title: "Smithy/LandingPage/Features",
  component: Features,
  parameters: {
    layout: "full",
  },
  tags: ["autodocs"],
  args: {
    onClick: fn(),
  },
} satisfies Meta<typeof Features>;

export default meta;
type Story = StoryObj<typeof Features>;

export const Primary: Story = {};
