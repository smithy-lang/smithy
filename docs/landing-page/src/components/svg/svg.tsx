import React, { Ref, forwardRef } from "react";
import { useResizeObserver } from "./useResizeObserver";

interface SVGProps {
  children?: React.ReactNode;
  className?: string;
}

export const SVG = forwardRef<SVGSVGElement, SVGProps>(
  ({ children, className }, ref) => {
    const size = useResizeObserver();
    return (
      <svg
        ref={ref}
        width={size.width}
        height={size.height}
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          pointerEvents: "none",
        }}
        className={className}
      >
        {children}
      </svg>
    );
  },
);
