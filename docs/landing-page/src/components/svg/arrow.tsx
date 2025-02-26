import React, { useEffect, useRef } from "react";

import { SVG } from "./svg";
import { useResizeObserver } from "./useResizeObserver";

export interface SVGArrow {
  startComponent: React.RefObject<HTMLElement>;
  endComponent: React.RefObject<HTMLElement>;
}

export const Arrow = ({ startComponent, endComponent }: SVGArrow) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const size = useResizeObserver();

  useEffect(() => {
    const startRect = startComponent.current?.getBoundingClientRect();
    const endRect = endComponent.current?.getBoundingClientRect();

    if (!startRect || !endRect) {
      return;
    }

    // Calculate the start and end points of the line
    const startX = startRect.left + startRect.width / 2 + window.scrollX;
    const startY = startRect.bottom + window.scrollY;
    const endX = endRect.left + endRect.width / 2 + window.scrollY;
    const endY = endRect.top - 2 + window.scrollX;

    const arrowOffset = 20;

    // Update the vertical SVG line
    const line = svgRef.current?.querySelector(".arrowVerticalLine");
    if (line) {
      line.setAttribute("x1", startX.toString());
      line.setAttribute("y1", startY.toString());
      line.setAttribute("x2", endX.toString());
      line.setAttribute("y2", endY.toString());
    }
    // Update the left SVG line
    const leftLine = svgRef.current?.querySelector(".arrowLeftDiagonal");
    if (leftLine) {
      leftLine.setAttribute("x1", endX.toString());
      leftLine.setAttribute("y1", endY.toString());
      leftLine.setAttribute("x2", (endX - arrowOffset).toString());
      leftLine.setAttribute("y2", (endY - arrowOffset).toString());
    }
    // Update the right SVG line
    const rightLine = svgRef.current?.querySelector(".arrowRightDiagonal");
    if (rightLine) {
      rightLine.setAttribute("x1", endX.toString());
      rightLine.setAttribute("y1", endY.toString());
      rightLine.setAttribute("x2", (endX + arrowOffset).toString());
      rightLine.setAttribute("y2", (endY - arrowOffset).toString());
    }
  }, [startComponent, endComponent, size]);

  return (
    <SVG ref={svgRef}>
      <defs>
        <linearGradient
          id="myGradient"
          // gradientTransform='rotate(90)'
        >
          <stop offset="0%" stopColor="hsl(var(--smithy-red-15))" />
          <stop
            offset="100%"
            stopColor="hsl(var(--smithy-gradient-midpoint))"
          />
        </linearGradient>
        <linearGradient id="myReverseGradient" gradientTransform="rotate(90)">
          <stop offset="0%" stopColor="hsl(var(--primary))" />
          <stop
            offset="100%"
            stopColor="hsl(var(--smithy-gradient-midpoint))"
          />
        </linearGradient>
      </defs>

      <line
        className="arrowVerticalLine"
        x1="0"
        y1="0"
        x2="0"
        y2="0"
        stroke="hsl(var(--smithy-gradient-midpoint))"
        strokeWidth="1"
      />
      <line
        className="arrowLeftDiagonal"
        x1="0"
        y1="0"
        x2="0"
        y2="0"
        stroke="url(#myGradient)"
        strokeWidth="1"
      />
      <line
        className="arrowRightDiagonal"
        x1="0"
        y1="0"
        x2="0"
        y2="0"
        stroke="url(#myReverseGradient)"
        strokeWidth="1"
      />
    </SVG>
  );
};
