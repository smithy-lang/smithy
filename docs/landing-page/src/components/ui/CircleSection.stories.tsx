import type { Meta, StoryObj } from "@storybook/react";

import { CircleSection } from "./CircleSection";

const meta = {
  title: "Smithy/ui/CircleSection",
  component: CircleSection,
  args: {
    title: "hello world",
    description: "some random text",
    circleUrls: [
      { src: "/icons/dark/swift.svg", name: "Swift" },
      { src: "/icons/dark/javaScript.svg", name: "JavaScript" },
      { src: "/icons/dark/rust.svg", name: "Rust" },
    ],
  },
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof CircleSection>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
