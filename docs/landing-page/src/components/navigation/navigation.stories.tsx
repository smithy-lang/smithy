import type { Meta, StoryObj } from "@storybook/react-vite";

import { TopNavigation } from ".";

const meta = {
  title: "Smithy/TopNavigation",
  component: TopNavigation,
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof TopNavigation>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
