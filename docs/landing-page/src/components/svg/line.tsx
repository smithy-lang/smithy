import React, { useRef, useEffect, RefObject } from "react";
import { SVG } from "./svg";
import { useResizeObserver } from "./useResizeObserver";

type Coordinates = {
  x: number;
  y: number;
};

interface LineConnectorProps {
  startComponent: RefObject<HTMLElement | null>;
  endComponent: RefObject<HTMLElement | null>;
  lineColor?: string;
  className?: string;
}

type LineDetails = {
  start: Coordinates;
  end: Coordinates;
  color: string;
};

/**
 * Calculates the coordinates and color for a line connecting two DOM elements
 * @param startRect - The DOMRect of the starting element
 * @param endRect - The DOMRect of the ending element
 * @returns LineDetails object containing start/end coordinates and line color
 */
function getCoordinates(
  startRect: DOMRect,
  endRect: DOMRect,
  window: Window,
): LineDetails {
  // Calculate initial center points of the rectangles
  const start = {
    x: startRect.left + startRect.width / 2,
    y: startRect.top + startRect.height / 2,
  };

  const end = {
    x: endRect.left + endRect.width / 2,
    y: endRect.top + endRect.height / 2,
  };

  let color = "hsl(var(--primary)";

  // Adjust vertical positioning when elements don't overlap vertically
  if (startRect.top > endRect.bottom) {
    start.y = startRect.top;
    end.y = endRect.bottom;
  }
  if (startRect.bottom < endRect.top) {
    start.y = startRect.bottom;
    end.y = endRect.top;
  }

  // Adjust horizontal positioning when elements don't overlap horizontally
  if (startRect.left > endRect.right) {
    start.x = startRect.left;
    end.x = endRect.right;
  }
  if (startRect.right < endRect.left) {
    start.x = startRect.right;
    end.x = endRect.left;
  }

  // Add the scrolling position to get the correct location
  start.x += window.scrollX;
  start.y += window.scrollY;
  end.x += window.scrollX;
  end.y += window.scrollY;

  // Set color based on line orientation and direction
  // For nearly vertical lines
  if (Math.abs(start.x - end.x) < 10) {
    color = "hsl(var(--smithy-gradient-midpoint))";
  }

  // For horizontal lines, apply gradient based on direction
  if (start.x - end.x > 20) {
    color = "url(#lineGradient)";
  }
  if (end.x - start.x > 20) {
    color = "url(#lineGradientTwo)";
  }

  return {
    start,
    end,
    color,
  };
}

export const LineConnector: React.FC<LineConnectorProps> = ({
  startComponent,
  endComponent,
  lineColor,
  className,
}) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const size = useResizeObserver();

  useEffect(() => {
    const startRect = startComponent.current?.getBoundingClientRect();
    const endRect = endComponent.current?.getBoundingClientRect();

    if (!startRect || !endRect) {
      return;
    }

    const { start, end, color } = getCoordinates(startRect, endRect, window);

    // Update the SVG line
    const line = svgRef.current?.querySelector("line");
    if (line) {
      line.setAttribute("x1", start.x.toString());
      // the 0.001 allows a horizontal line to display a gradient
      // see https://www.w3.org/TR/SVG11/coords.html#ObjectBoundingBox for the spec
      line.setAttribute("y1", (start.y + 0.001).toString());
      line.setAttribute("x2", end.x.toString());
      line.setAttribute("y2", end.y.toString());
      line.setAttribute("stroke", lineColor || color);
    }
  }, [startComponent, endComponent, size]);

  return (
    <SVG ref={svgRef} className={className}>
      <defs>
        <linearGradient id="lineGradient">
          <stop offset="0%" stopColor="hsl(var(--primary))" />
          <stop offset="100%" stopColor="hsl(var(--smithy-red-15))" />
        </linearGradient>
        <linearGradient id="lineGradientTwo">
          <stop offset="0%" stopColor="hsl(var(--primary))" />
          <stop offset="100%" stopColor="hsl(var(--smithy-red-15))" />
        </linearGradient>
      </defs>
      <line
        x1="0"
        y1="0"
        x2="0"
        y2="0"
        stroke={"hsl(var(--smithy-gradient-midpoint))"}
        strokeWidth="2"
      />
    </SVG>
  );
};
