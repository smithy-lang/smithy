import React, { PropsWithChildren } from "react";
import { renderToString } from "react-dom/server";

import { cn } from "@/lib/utils";

interface SmithyPopGradientProps extends PropsWithChildren<
  React.HTMLAttributes<HTMLDivElement>
> {
  withSparks?: boolean;
}

// from is bottom right
// to is top left
const gradientPopClasses = `bg-smithy-pop-gradient relative overflow-hidden`;

export const SmithyPopGradient = (props: SmithyPopGradientProps) => {
  return (
    <>
      <div className={cn(gradientPopClasses, props.className)}>
        <div className="z-20">{props.children}</div>
      </div>
    </>
  );
};
