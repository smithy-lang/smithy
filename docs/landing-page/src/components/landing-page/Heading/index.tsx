import React from "react";
import { Tagline } from "./Tagline";
import { SmithyPopGradient } from "@/components/ui/SmithyPopGradient";

import { Card, CardHeader } from "@/components/ui/card";
import { IdePanel } from "@/components/ui/ide-panel";
import { ServiceExample } from "../ServiceExample";
import { Button } from "@/components/ui/button.tsx";
import { Navigation } from "lucide-react";

interface HeadingProps {
  serviceExampleRef: React.RefObject<HTMLDivElement>;
}

export const Heading = (props: HeadingProps) => {
  return (
    <SmithyPopGradient withSparks>
      <div className="max-w-7xl mx-auto">
        <div className="py-16 px-8 flex flex-col md:flex-row justify-around grow items-center">
          <Tagline />

          <Card
            variant={"default"}
            className="bg-white text-center mt-12 mx-12 w-fit lg:w-auto lg:mt-0 border-white"
            ref={props.serviceExampleRef}
          >
            <CardHeader className="text-smithy-black">
              <div className="flex flex-col lg:w-[450px] lg:flex-row lg:justify-between lg:items-center">
                <div className="text-2xl m-auto mb-4 lg:w-24 text-center lg:text-left text-smithy-purple lg:m-0 lg:ml-8">
                  Smithy Service Example
                </div>
                <IdePanel>
                  <ServiceExample />
                </IdePanel>
              </div>
            </CardHeader>
          </Card>
        </div>
      </div>
    </SmithyPopGradient>
  );
};
