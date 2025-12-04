import React, { useRef, RefObject } from "react";
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { LineConnector } from "@/components/svg/line";

interface WebProps {
  smithyBuildRef: RefObject<HTMLDivElement | null>;
}

export const Web = (props: WebProps) => {
  // const centerRef = useRef<HTMLDivElement>(null);
  const leftRef = useRef<HTMLDivElement>(null);
  const bottomLeftRef = useRef<HTMLDivElement>(null);
  const bottomRightRef = useRef<HTMLDivElement>(null);
  const rightRef = useRef<HTMLDivElement>(null);

  return (
    <div className="flex flex-row flex-wrap gap-6 w-full">
      {/* CENTER */}
      <div className="flex-1 basis-full flex items-center justify-center mb-8 lg:mb-4">
        <Card
          variant={"gradient-border"}
          className="bg-smithy-black text-center"
          ref={props.smithyBuildRef}
        >
          <CardHeader className="text-smithy-red-15">
            <code>
              <span className="font-bold">&rsaquo;</span> smithy build
            </code>
          </CardHeader>
        </Card>
      </div>

      <div className="flex-1 flex items-end justify-center">
        <Card
          variant={"gradient-border"}
          className="bg-smithy-black text-center"
          ref={leftRef}
        >
          <CardHeader className="text-smithy-red-15">
            <code>SDKs</code>
          </CardHeader>
        </Card>
      </div>

      <div className="flex-1 flex items-end justify-center">
        <Card
          variant={"gradient-border"}
          className="bg-smithy-black text-center"
          ref={rightRef}
        >
          <CardHeader className="text-smithy-red-15">
            <code>Docs</code>
          </CardHeader>
        </Card>
      </div>

      <div className="flex-1 basis-full flex items-center justify-center">
        <Card
          variant={"gradient-border"}
          className="bg-smithy-black text-center"
          ref={bottomLeftRef}
        >
          <CardHeader className="text-smithy-red-15">
            <code>Server Stubs</code>
          </CardHeader>
        </Card>
      </div>

      <LineConnector
        startComponent={props.smithyBuildRef}
        endComponent={leftRef}
        lineColor="hsl(var(--smithy-gradient-midpoint))"
      />
      <LineConnector
        startComponent={bottomLeftRef}
        endComponent={props.smithyBuildRef}
        lineColor="hsl(var(--smithy-gradient-midpoint))"
      />
      <LineConnector
        startComponent={props.smithyBuildRef}
        endComponent={bottomRightRef}
        lineColor="hsl(var(--smithy-gradient-midpoint))"
      />
      <LineConnector
        startComponent={props.smithyBuildRef}
        endComponent={rightRef}
        lineColor="hsl(var(--smithy-gradient-midpoint))"
      />
    </div>
  );
};
