import type { Meta, StoryObj } from "@storybook/react-vite";
import { fn } from "storybook/test";

import { Button } from "./button";

const meta = {
  title: "Smithy/ui/Button",
  component: Button,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
  args: {
    variant: "default",
    onClick: fn(),
  },
  argTypes: {
    variant: {
      control: "select",
      options: [
        "default",
        "gradient",
        "destructive",
        "outline-solid",
        "gradient-outline",
        "secondary",
        "ghost",
        "link",
      ],
    },
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  args: {
    variant: "default",
    children: "Click Me!",
  },
};
export const Gradient: Story = {
  args: {
    variant: "gradient",
    children: "Click Me!",
  },
};
export const Destructive: Story = {
  args: {
    variant: "destructive",
    children: "Click Me!",
  },
};
export const outline: Story = {
  args: {
    variant: "outline",
    children: "Click Me!",
  },
};
export const GradientOutline: Story = {
  args: {
    variant: "gradient-outline",
    children: "Click Me!",
  },
};
export const secondary: Story = {
  args: {
    variant: "secondary",
    children: "Click Me!",
  },
};
export const ghost: Story = {
  args: {
    variant: "ghost",
    children: "Click Me!",
  },
};
export const link: Story = {
  args: {
    variant: "link",
    children: "Click Me!",
  },
};
