import type { Meta, StoryObj } from "@storybook/react";

import { Brand } from "./brand";

const meta = {
  title: "Smithy/Brand",
  component: Brand,
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof Brand>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
