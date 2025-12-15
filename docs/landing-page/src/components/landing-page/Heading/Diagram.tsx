import React, { useRef } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { LineConnector } from "@/components/svg/line";
import { Arrow } from "@/components/svg/arrow";
import { Spider } from "@/components/svg/spider";

import { ServiceExample } from "../ServiceExample";
import { SmithyGlow } from "@/components/ui/SmithyGlow";
import { IdePanel } from "@/components/ui/ide-panel";

export const Diagram = () => {
  const modelRef = useRef<HTMLDivElement>(null);
  const serviceExampleRef = useRef<HTMLDivElement>(null);
  const smithyBuildRef = useRef<HTMLDivElement>(null);
  const sunRef = useRef<HTMLDivElement>(null);
  const moonRef = useRef<HTMLDivElement>(null);
  const earthRef = useRef<HTMLDivElement>(null);
  const waterRef = useRef<HTMLDivElement>(null);

  return (
    <div className="flex flex-col justify-center grow items-center overflow-hidden">
      <Card variant={"default"} className="bg-white text-center" ref={modelRef}>
        <CardHeader className="text-smithy-black">
          <div className="flex w-[450px] justify-between items-center">
            <div className="text-2xl w-24 text-left text-smithy-purple">
              Smithy Service Example
            </div>
            <IdePanel>
              <ServiceExample />
            </IdePanel>
          </div>
        </CardHeader>
      </Card>

      <SmithyGlow className="mt-20 p-24 bg-position-[center_center] bg-size-[91%_85%]">
        <Card
          variant={"gradient-border"}
          className="bg-smithy-black text-center"
          ref={serviceExampleRef}
        >
          <CardHeader className="text-smithy-red-15 my-5">
            <div className="flex w-[450px] justify-between gap-14 items-center">
              <div className="text-2xl w-24 text-left">Model</div>
              <IdePanel>
                <div className="w-64 my-10 text-left ml-5">Something else</div>
              </IdePanel>
            </div>
          </CardHeader>
        </Card>
      </SmithyGlow>

      <div className="mt-20">
        <Card
          variant={"gradient-border"}
          className="bg-smithy-black text-center"
          ref={smithyBuildRef}
        >
          <CardHeader>
            <CardTitle>
              <code>&gt; smithy build</code>
            </CardTitle>
          </CardHeader>
        </Card>
      </div>

      <div className="flex flex-row justify-between w-full mt-20">
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

        <LineConnector
          startComponent={modelRef}
          endComponent={serviceExampleRef}
        />

        <Arrow
          startComponent={serviceExampleRef}
          endComponent={smithyBuildRef}
        />

        <Spider
          startComponent={smithyBuildRef}
          endComponents={[sunRef, moonRef, earthRef, waterRef]}
        />
      </div>
    </div>
  );
};
