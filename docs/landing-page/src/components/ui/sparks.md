To generate the sparks svg:

```
import React, { PropsWithChildren } from "react";
import { renderToString } from "react-dom/server";
const circles = [
  { r: 175, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 26", strokeDashoffset: "15", strokeOpacity: .65 },
  { r: 155, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 26", strokeDashoffset: "16", strokeOpacity: .55 },
  { r: 135, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 22", strokeDashoffset: "14", strokeOpacity: .45 },
  { r: 115, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 18", strokeDashoffset: "25", strokeOpacity: .35 },
  { r: 95, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 16", strokeDashoffset: "15", strokeOpacity: .15 },
  { r: 75, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 12", strokeDashoffset: "17", strokeOpacity: .15 },
  { r: 55, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 12", strokeDashoffset: "13", strokeOpacity: .15 },
  { r: 35, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 10", strokeDashoffset: "21", strokeOpacity: .15 },
  { r: 15, fillOpacity: 0, strokeWidth: 20, strokeDasharray: "1 7", strokeDashoffset: "17", strokeOpacity: .15 },
]

const sparks = renderToString(<>
  {`<?xml version="1.0" encoding="UTF-8"?>`}
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400">
    {
      circles.map((circleProps) => {
        return (
          <circle
            key={circleProps.r}
            cx={200}
            cy={200}
            stroke={"#FFFFFF"}
            {...circleProps}
          />
        )
      })
    }
  </svg>
</>
)
```
