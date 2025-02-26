import React from "react";

const lines: (string | React.ReactElement)[] = [
  <>
    <span className="smithy-highlight-purple">service</span> CoffeeShop &#123;
  </>,
  "  operations: [",
  "      GetMenu",
  "  ]",
  "  resources: [",
  "      Order",
  "  ]",
  "}",
  "",
  <>
    <span className="smithy-highlight-purple">operation</span> GetMenu &#123;
  </>,
  "    output : = {",
  "        ...",
  "    }",
  "}",
  "",
  <>
    <span className="smithy-highlight-purple">resource</span> Order &#123;
  </>,
  "    identifiers: {",
  "        id: Uuid",
  "    }",
  "    ...",
  "}",
];

export const ServiceExample = () => {
  return (
    <pre className="text-left p-4 rounded text-xs min-w-64 bg-stone-100 rounded-b-xl">
      <code className="text-left">
        {lines.map((line, i) => (
          <div key={i} className="leading-none">
            {/* <span className="text-smithy-red-15">
              {i < 9 ? " " : ""}
              {i + 1}
              &#124;&#32;
            </span> */}
            {line === "" && <>&nbsp;</>}
            {line}
          </div>
        ))}
      </code>
    </pre>
  );
};
