import React, {
  useRef,
  useCallback,
  useEffect,
  useMemo,
  useState,
  RefObject,
} from "react";

import { LineConnector } from "./line";

export interface WheelProps {
  centerComponent: React.ReactElement<{ ref?: RefObject<HTMLElement> }>;
  endComponents: React.ReactElement<{ ref?: RefObject<HTMLElement> }>[];
  lineColor?: string;
}

const fillOrderByLength: { [key: string]: number[] } = {
  "1": [6],
  "2": [1, 6],
  "3": [5, 6, 7],
  "4": [0, 2, 5, 7],
  "5": [0, 2, 5, 6, 7],
  "6": [0, 1, 2, 5, 6, 7],
  "7": [0, 1, 2, 4, 5, 6, 7],
  "8": [0, 1, 2, 3, 4, 5, 6, 7],
};

const centerPosition = 4;

export const Wheel = (props: WheelProps) => {
  const centerRef = useRef<HTMLDivElement>(null);
  const spotOneRef = useRef<HTMLDivElement>(null);
  const spotTwoRef = useRef<HTMLDivElement>(null);
  const spotThreeRef = useRef<HTMLDivElement>(null);
  const spotFourRef = useRef<HTMLDivElement>(null);
  const spotFiveRef = useRef<HTMLDivElement>(null);
  const spotSixRef = useRef<HTMLDivElement>(null);
  const spotSevenRef = useRef<HTMLDivElement>(null);
  const spotEightRef = useRef<HTMLDivElement>(null);
  const refs = [
    spotOneRef,
    spotTwoRef,
    spotThreeRef,
    spotFourRef,
    spotFiveRef,
    spotSixRef,
    spotSevenRef,
    spotEightRef,
  ];

  const centerComponent = useMemo(() => {
    return (
      <div className="flex flex-col justify-center">
        {React.cloneElement(props.centerComponent, {
          ref: centerRef,
        })}
      </div>
    );
  }, [props.centerComponent]);

  const [beforeCenter, afterCenter] = useMemo(() => {
    if (!props.endComponents) return [[], []];
    if (props.endComponents.length === 0) return [[], []];

    const beforeCenter: React.ReactNode[] = [];
    const afterCenter: React.ReactNode[] = [];
    const fillOrder = fillOrderByLength[props.endComponents.length.toString()];
    let currentEndComponent = 0;

    for (let i = 0; i < 8; i++) {
      if (fillOrder.includes(i)) {
        const component: React.ReactElement<{ ref?: RefObject<HTMLElement> }> =
          props.endComponents[currentEndComponent];

        const wrappedComponent = (
          <div key={i} className="flex flex-col justify-center">
            {React.cloneElement(component, {
              ref: refs[i],
            })}
          </div>
        );

        // const element = props.endComponents[currentEndComponent];
        if (i < centerPosition) {
          beforeCenter.push(wrappedComponent);
        } else {
          afterCenter.push(wrappedComponent);
        }
        currentEndComponent++;
      } else {
        if (i < centerPosition) {
          beforeCenter.push(<div key={i} />);
        } else {
          afterCenter.push(<div key={i} />);
        }
      }
    }
    return [beforeCenter, afterCenter];
  }, [props.endComponents]);

  return (
    <div className="grid grid-cols-3 grid-rows-3 gap-1 justify-items-center align-items-center">
      {refs.map((ref, index) => {
        if (!ref) return "";
        return (
          <LineConnector
            key={index}
            startComponent={centerRef}
            endComponent={ref}
            lineColor={props.lineColor}
          />
        );
      })}

      {beforeCenter}

      {centerComponent}

      {afterCenter}
    </div>
  );
};
