import React, { useRef } from "react";
import { TopNavigation } from "@/components/navigation";
import {
  Heading,
  SubHeading,
  Quote,
  Features,
  InformationCircles,
  Footer,
} from "@/components/landing-page";
import { LineConnector } from "./components/svg/line";

function App() {
  const serviceExample = useRef<HTMLDivElement>(null);
  const modelRef = useRef<HTMLDivElement>(null);

  return (
    <>
      <TopNavigation />
      <main className="pt-(--nav-offset)">
        <Heading serviceExampleRef={serviceExample} />

        <SubHeading modelRef={modelRef} />

        <InformationCircles />

        <Quote />

        <Features />

        <Footer />

        <LineConnector
          startComponent={serviceExample}
          endComponent={modelRef}
          className="hidden lg:block"
        />
      </main>
    </>
  );
}

export default App;
