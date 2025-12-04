import type { Meta, StoryObj } from "@storybook/react-vite";

import { InformationCircles } from "./index";

const meta = {
  title: "Smithy/LandingPage/InformationCircles",
  component: InformationCircles,
  args: {
    circleUrls: ["/icons/dark/swift.svg"],
    name: "Swift",
  },
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof InformationCircles>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
