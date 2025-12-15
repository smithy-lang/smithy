import React, { useRef } from "react";
import type { Meta, StoryObj } from "@storybook/react-vite";

import { Spider } from "./spider";
import { Card, CardHeader, CardTitle } from "../ui/card";

const MyComponent: React.FC = (props: { curveLevel?: number }) => {
  const startRef = useRef<HTMLDivElement>(null);
  const sunRef = useRef<HTMLDivElement>(null);
  const moonRef = useRef<HTMLDivElement>(null);
  const earthRef = useRef<HTMLDivElement>(null);
  const waterRef = useRef<HTMLDivElement>(null);

  return (
    <div className="dark py-5 w-full flex flex-col justify-center items-center">
      <Spider
        startComponent={startRef}
        endComponents={[sunRef, moonRef, earthRef, waterRef]}
        curveLevel={props.curveLevel}
      />
      {/* <div className="z-10 bg-smithy-rose text-white w-24" ref={startRef}>Start Component</div> */}

      <Card ref={startRef} variant={"gradient-border"}>
        <CardHeader>
          <CardTitle>Universe</CardTitle>
        </CardHeader>
      </Card>

      <br />
      <br />
      <br />
      <br />
      <div className="flex flex-row justify-between w-7/12">
        <Card ref={sunRef} variant={"gradient-border"}>
          <CardHeader>
            <CardTitle>Sun</CardTitle>
          </CardHeader>
        </Card>
        <Card ref={moonRef} variant={"gradient-border"}>
          <CardHeader>
            <CardTitle>Moon</CardTitle>
          </CardHeader>
        </Card>
        <Card ref={earthRef} variant={"gradient-border"}>
          <CardHeader>
            <CardTitle>Earth</CardTitle>
          </CardHeader>
        </Card>
        <Card ref={waterRef} variant={"gradient-border"}>
          <CardHeader>
            <CardTitle>Water</CardTitle>
          </CardHeader>
        </Card>
      </div>
    </div>
  );
};

const meta = {
  title: "Smithy/svg/Spider",
  component: MyComponent,
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof Spider>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
export const CurveLevel: Story = {
  args: {
    curveLevel: 10,
  },
};
