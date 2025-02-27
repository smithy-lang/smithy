import React, { useRef } from "react";
import type { Meta, StoryObj } from "@storybook/react";

import { Wheel } from "./wheel";
import { Card, CardHeader, CardTitle } from "../ui/card";
import { ServiceExample } from "@/components/landing-page/ServiceExample";

const MyComponent: React.FC<{
  planets: React.ReactNode[];
  lineColor?: string;
}> = (props) => {
  return (
    <div className="dark py-5 w-full flex flex-col justify-center items-center">
      <Wheel
        lineColor={props.lineColor}
        centerComponent={
          <Card variant={"gradient-border"} className="text-center">
            <ServiceExample />
          </Card>
        }
        endComponents={props.planets.map((planet, i) => (
          <Card
            key={i}
            variant={"gradient-border"}
            className="text-center bg-background"
          >
            <CardHeader>
              <CardTitle>{planet}</CardTitle>
            </CardHeader>
          </Card>
        ))}
      />
    </div>
  );
};

const meta = {
  title: "Smithy/svg/wheel",
  component: MyComponent,
  parameters: {
    layout: "centered",
  },
  globals: {
    theme: "dark",
  },
} satisfies Meta<typeof MyComponent>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    planets: [
      <div className="w-20 h-20 flex flex-col justify-center">Server</div>,
      <div className="w-20 h-20 flex flex-col justify-center">IDE</div>,
      <div className="w-20 h-20 flex flex-col justify-center">SDK</div>,
      <div className="w-20 h-20 flex flex-col justify-center">Lint</div>,
      <div className="w-20 h-20 flex flex-col justify-center">Validate</div>,
      <div className="w-20 h-20 flex flex-col justify-center">Lorem</div>,
      <div className="w-20 h-20 flex flex-col justify-center">Ipsum</div>,
      <div className="w-20 h-20 flex flex-col justify-center">Dolor</div>,
    ],
  },
};
export const LineColorOverride: Story = {
  args: {
    planets: ["Earth", "Mars"],
    lineColor: "blue",
  },
};
export const One: Story = {
  args: {
    planets: ["Earth"],
  },
};
export const Two: Story = {
  args: {
    planets: ["Earth", "Mars"],
  },
};
export const Three: Story = {
  args: {
    planets: ["Earth", "Mars", "Jupiter"],
  },
};
export const Four: Story = {
  args: {
    planets: ["Earth", "Mars", "Jupiter", "Saturn"],
  },
};
export const Five: Story = {
  args: {
    planets: ["Mercury", "Venus", "Earth", "Mars", "Jupiter"],
  },
};
export const Six: Story = {
  args: {
    planets: ["Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn"],
  },
};
export const Seven: Story = {
  args: {
    planets: [
      "Mercury",
      "Venus",
      "Earth",
      "Mars",
      "Jupiter",
      "Saturn",
      "Uranus",
    ],
  },
};
