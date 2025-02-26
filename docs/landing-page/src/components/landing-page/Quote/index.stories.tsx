import type { Meta, StoryObj } from "@storybook/react";
import { fn } from "@storybook/test";

import { Quote } from ".";

const meta = {
  title: "Smithy/LandingPage/Quote",
  component: Quote,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
  args: {
    onClick: fn(),
  },
} satisfies Meta<typeof Quote>;

export default meta;
type Story = StoryObj<typeof Quote>;

export const Primary: Story = {};
