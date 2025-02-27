import React from "react";

import { cn } from "@/lib/utils";

const idePanelClasses =
  "relative inline-block w-fit border border-smithy-dark-gray rounded-xl before:content-['_'] before:block before:w-full before:h-6 before:border-b before:border-smithy-dark-gray";

export const IdePanel = (props: React.HTMLAttributes<HTMLDivElement>) => {
  return (
    <div className={cn(idePanelClasses, props.className)}>
      <div className="absolute top-[6px] left-2 flex flex-row gap-2">
        <div className="rounded-full bg-red-500 h-3 w-3" />
        <div className="rounded-full bg-yellow-500 h-3 w-3" />
        <div className="rounded-full bg-green-500 h-3 w-3" />
      </div>
      {props.children}
    </div>
  );
};
