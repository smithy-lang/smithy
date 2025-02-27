import React, { useCallback, useEffect, useMemo, useState } from "react";

import { SVG } from "./svg";
import { useResizeObserver } from "./useResizeObserver";

type Coords = {
  x: number;
  y: number;
};

export interface SpiderProps {
  startComponent: React.RefObject<HTMLElement>;
  endComponents: React.RefObject<HTMLElement>[];
  curveLevel?: number;
}

export const Spider = (props: SpiderProps) => {
  const [paths, setPaths] = useState<React.ReactNode[]>([]);
  const rect = props.startComponent?.current?.getBoundingClientRect();
  const size = useResizeObserver();

  useEffect(() => {
    if (!rect) return;
    const startingCoords: Coords = {
      x: rect.left + rect.width / 2 + window.scrollX,
      y: rect.bottom + window.scrollY,
    };

    const calculatedPaths = props.endComponents.map((endComponent) => {
      const endRect = endComponent.current?.getBoundingClientRect();
      if (!endRect) return;
      const endCoords: Coords = {
        x: endRect.left + endRect.width / 2 + window.scrollX,
        y: endRect.top + window.scrollY,
      };
      return connect(startingCoords, endCoords, props.curveLevel || 100);
    });

    setPaths(calculatedPaths);
  }, [props.startComponent, props.endComponents, size]);

  /**
   * curveLevel is the amount of curve present in the line, 2 is roughly vertical
   */
  const connect = useCallback(
    (start: Coords, end: Coords, curveLevel: number = 2) => {
      const calculatedPaths: React.ReactNode[] = [];

      const distanceBetweenPoints = Math.sqrt(
        Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2),
      );

      const midpoint = {
        x: (start.x + end.x) / 2,
        y: (start.y + end.y) / 2,
      };
      const offset = distanceBetweenPoints / curveLevel;

      calculatedPaths.push(
        <path
          key={JSON.stringify(start)}
          d={`M ${start.x} ${start.y} C ${start.x} ${start.y}, ${start.x} ${midpoint.y - offset}, ${midpoint.x} ${midpoint.y}`}
          stroke="url(#topGradient)"
          fill="transparent"
        />,
      );
      calculatedPaths.push(
        <path
          key={JSON.stringify(end)}
          d={`M ${end.x} ${end.y} C ${end.x} ${end.y}, ${end.x} ${midpoint.y + offset}, ${midpoint.x} ${midpoint.y}`}
          stroke="url(#gradient)"
          fill="transparent"
        />,
      );

      return calculatedPaths;
    },
    [],
  );

  return (
    <SVG>
      <defs>
        <linearGradient id="topGradient" gradientTransform="rotate(90)">
          <stop offset="0%" stopColor="hsl(var(--primary))" />
          <stop
            offset="100%"
            stopColor="hsl(var(--smithy-gradient-midpoint))"
          />
        </linearGradient>
        <linearGradient id="gradient" gradientTransform="rotate(90)">
          <stop offset="0%" stopColor="hsl(var(--smithy-gradient-midpoint))" />
          <stop offset="100%" stopColor="hsl(var(--smithy-red-15))" />
        </linearGradient>
      </defs>
      {paths}
    </SVG>
  );
};
