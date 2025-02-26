import type { Meta, StoryObj } from "@storybook/react";
import { fn } from "@storybook/test";

import { Button } from "./button";
import { Icons } from "./icons";

const meta = {
  title: "Smithy/ui/Icons",
  component: ({ children }) => {
    return (
      <Button variant={"ghost"} size={"icon"}>
        {children}
      </Button>
    );
  },
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
} satisfies Meta<typeof Icons.gitHub>;

export default meta;
type Story = StoryObj<typeof meta>;

export const GitHub: Story = {
  args: {
    children: <Icons.gitHub />,
  },
};
export const React: Story = {
  args: {
    children: <Icons.react />,
  },
};
export const Spinner: Story = {
  args: {
    children: <Icons.spinner />,
  },
};
