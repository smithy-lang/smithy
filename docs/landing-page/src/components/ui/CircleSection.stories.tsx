import type { Meta, StoryObj } from "@storybook/react";

import { CircleSection } from "./CircleSection";

const meta = {
  title: "Smithy/ui/CircleSection",
  component: CircleSection,
  args: {
    title: "hello world",
    description: "some random text",
    circleUrls: [
      { src: "/icons/dark/swift.svg" },
      { src: "/icons/dark/javaScript.svg" },
      { src: "/icons/dark/rust.svg" },
    ],
  },
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof CircleSection>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
