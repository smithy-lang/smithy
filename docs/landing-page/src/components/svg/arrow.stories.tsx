import React, { useRef } from "react";
import type { Meta, StoryObj } from "@storybook/react";

import { Arrow } from "./arrow";
import { Card, CardHeader, CardTitle } from "../ui/card";

const MyComponent: React.FC = () => {
  const startRef = useRef<HTMLDivElement>(null);
  const endRef = useRef<HTMLDivElement>(null);

  return (
    <div className="dark py-5 w-full flex flex-col justify-center items-center">
      <Arrow startComponent={startRef} endComponent={endRef} />
      {/* <div className="z-10 bg-smithy-rose text-white w-24" ref={startRef}>Start Component</div> */}

      <Card
        ref={startRef}
        variant={"gradient-border"}
        // className='z-10'
      >
        <CardHeader>
          <CardTitle>Hello World</CardTitle>
        </CardHeader>
      </Card>

      <br />
      <br />
      <br />
      <br />
      <Card ref={endRef} variant={"gradient-border"}>
        <CardHeader>
          <CardTitle>Goodbye World</CardTitle>
        </CardHeader>
      </Card>
    </div>
  );
};

const meta = {
  title: "Smithy/svg/Arrow",
  component: MyComponent,
  parameters: {
    layout: "centered",
  },
} satisfies Meta<typeof Arrow>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
