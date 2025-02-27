import React from "react";
import type { Meta, StoryObj } from "@storybook/react";

import { Section } from "./Section";
import { Button } from "./button";

const meta = {
  title: "Smithy/ui/Section",
  component: Section,
  args: {
    title: "Section Title",
    description:
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum elit nulla, elementum pellentesque mattis sit amet, consectetur nec diam. Curabitur maximus vulputate est maximus suscipit. Suspendisse vehicula volutpat dolor non dapibus. Nullam malesuada vulputate tortor sed euismod. Donec quis lacus egestas orci pharetra eleifend ut consequat lorem. Sed quis metus eu justo iaculis sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Nullam eget nulla ultrices, condimentum mi eu, auctor nunc. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Mauris rutrum id velit eu molestie. Phasellus nulla justo, semper vel massa vel, faucibus molestie purus. Cras auctor iaculis sodales. Donec vestibulum venenatis felis, condimentum venenatis dolor pharetra et. Ut non commodo enim. Pellentesque consequat nibh eget tempus iaculis. Ut ac dignissim ligula.",
  },
  parameters: {
    layout: "fullscreen",
  },
  tags: ["autodocs"],
} satisfies Meta<typeof Section>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};
export const WithAction: Story = {
  args: {
    action: <Button>Click Me</Button>,
  },
};
export const WithChildren: Story = {
  args: {
    children: <div>hello world</div>,
  },
};
