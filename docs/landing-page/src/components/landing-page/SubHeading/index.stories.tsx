import { useRef } from "react";
import type { Meta, StoryObj } from "@storybook/react-vite";

import { SubHeading } from "./index";

const meta = {
  title: "Smithy/LandingPage/SubHeading",
  component: () => {
    const modelRef = useRef<HTMLDivElement>(null);
    return <SubHeading modelRef={modelRef} />;
  },
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof SubHeading>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
