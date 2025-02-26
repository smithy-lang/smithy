import React, { PropsWithChildren } from "react";
import { renderToString } from "react-dom/server";

import { cn } from "@/lib/utils";

interface SmithyPopGradientProps
  extends PropsWithChildren<React.HTMLAttributes<HTMLDivElement>> {
  withSparks?: boolean;
}

// from is bottom right
// to is top left
const gradientPopClasses = `from-smithy-plum via-smithy-purple to-smithy-rose from-20% via-30% bg-[url("/sparks/sparks.svg"),linear-gradient(115deg,var(--tw-gradient-stops))] bg-[position:100%_100%,center] bg-[size:150%,100%] md:bg-[size:85%,100%] lg:bg-[size:65%,100%] bg-no-repeat relative overflow-hidden`;

export const SmithyPopGradient = (props: SmithyPopGradientProps) => {
  return (
    <>
      <div className={cn(gradientPopClasses, props.className)}>
        <div className="z-20">{props.children}</div>
      </div>
    </>
  );
};
