import React from "react";
import { cn } from "@/lib/utils";
import { Section, type SectionProps } from "@/components/ui/Section";

export interface CircleSectionProps extends SectionProps {
  circleUrls: (React.ImgHTMLAttributes<HTMLImageElement> & {
    name: string;
    location?: string;
  })[];
}

export const CircleSection = (props: CircleSectionProps) => {
  return (
    <Section
      title={props.title}
      description={props.description}
      className="bg-smithy-light-gray text-smithy-black p-4 lg:p-8"
    >
      <div className="flex flex-row gap-8 lg:gap-16 flex-wrap justify-center lg:justify-start">
        {props.circleUrls.map((imgProps) => (
          <a
            key={imgProps.src}
            href={imgProps.location}
            className="hover:bg-gradient-to-r hover:from-secondary hover:to-primary hover:text-primary-foreground hover:shadow-lg rounded-full relative group"
          >
            <div className="flex justify-center bg-[rgb(241,239,237)] rounded-full hover:bg-[rgb(241,239,237)]/80">
              <img
                {...imgProps}
                className={cn("h-20 w-20 p-4 ", imgProps.className)}
              />
            </div>

            <div className="absolute z-50 px-3 py-2 text-sm text-white bg-smithy-black rounded-md shadow-md whitespace-nowrap top-full mt-2 left-1/2 transform -translate-x-1/2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity duration-200">
              {imgProps.name}
              <div className="absolute w-2 h-2 bg-smithy-black rotate-45 top-[-4px] left-1/2 -translate-x-1/2" />
            </div>
          </a>
        ))}
      </div>
      {props.action}
    </Section>
  );
};
