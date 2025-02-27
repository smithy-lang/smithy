import React from "react";
import type { Meta, StoryObj } from "@storybook/react";

import { SmithyGlow } from "./SmithyGlow";
import { Tagline } from "../landing-page/Heading/Tagline";

const meta = {
  title: "Smithy/ui/SmithyGlow",
  component: () => {
    return (
      <SmithyGlow className="w-full h-96">
        <div className="flex flex-col w-full h-full justify-center ml-12">
          <Tagline />
        </div>
      </SmithyGlow>
    );
  },
  parameters: {
    layout: "fullscreen",
  },
  tags: ["autodocs"],
} satisfies Meta<typeof SmithyGlow>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
