import React, { useRef } from "react";
import type { Meta, StoryObj } from "@storybook/react-vite";

import { Web } from "./web";

const meta = {
  title: "Smithy/LandingPage/Web",
  component: () => {
    const centerRef = useRef<HTMLDivElement>(null);
    return <Web smithyBuildRef={centerRef} />;
  },
  parameters: {
    layout: "centered",
  },
} satisfies Meta<typeof Web>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
