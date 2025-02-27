import React from "react";

import { cn } from "@/lib/utils";

const gradientPopClasses = "";

export interface SectionProps extends React.HTMLAttributes<HTMLDivElement> {
  title: string;
  description: string;
  action?: React.ReactNode;
}

export const Section = (props: SectionProps) => {
  return (
    <section className={cn(gradientPopClasses, props.className)}>
      <h2 className="text-3xl pb-4">{props.title}</h2>
      <p className="text-md pb-4 whitespace-pre-line">{props.description}</p>
      {props.children}
      {props.action}
    </section>
  );
};
