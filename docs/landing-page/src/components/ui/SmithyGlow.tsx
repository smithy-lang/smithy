import React from "react";

import { cn } from "@/lib/utils";

const glowClasses =
  "text-white bg-smithy-black bg-radial-gradient from-primary/50 to-smithy-black to-65% bg-no-repeat bg-size-[150%_200%] bg-position-[center_-25%]";

export const SmithyGlow = (props: React.HTMLAttributes<HTMLDivElement>) => {
  return (
    <div className={cn(glowClasses, props.className)}>{props.children}</div>
  );
};
