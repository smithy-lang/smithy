import React from "react";
import type { Meta, StoryObj } from "@storybook/react";

import { SmithyPopGradient } from "./SmithyPopGradient";
import { Tagline } from "../landing-page/Heading/Tagline";

const meta = {
  title: "Smithy/ui/SmithyPopGradient",
  component: (props) => {
    return (
      <SmithyPopGradient className="w-full h-96" {...props}>
        <div className="flex flex-col w-full h-full justify-center ml-12">
          <Tagline />
        </div>
      </SmithyPopGradient>
    );
  },
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof SmithyPopGradient>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
export const WithSparks: Story = {
  args: {
    withSparks: true,
  },
};
