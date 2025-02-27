import React from "react";

import { SmithyPopGradient } from "./SmithyPopGradient";

export const Brand = () => {
  return (
    <div className="text-center text-smithy-black bg-white p-8">
      <h2 className="text-2xl">Colors</h2>
      <div className="grid grid-flow-row-dense grid-cols-4 w-full h-auto">
        <div className="text-white color-white bg-smithy-black col-span-3 h-24">
          Smithy Black
        </div>
        <div className="text-white bg-smithy-red col-span-1 h-24">
          Smithy Red
        </div>

        <div className="bg-smithy-light-gray col-span-2 h-24">
          Smithy Light Gray
        </div>
        <div className="text-white bg-smithy-dark-gray col-span-1 h-24">
          Smithy Dark Gray
        </div>
        <div className="col-span-1 h-24">
          <div className="bg-smithy-red-33 h-1/2">Red 33%</div>
          <div className="bg-smithy-red-15 h-1/2">Red 15%</div>
        </div>

        <div className="h-16 text-white bg-smithy-plum">Plum</div>
        <div className="h-16 text-white bg-smithy-rose">Rose</div>
        <div className="h-16 text-white bg-smithy-purple">Purple</div>
        <div className="h-16 text-white bg-smithy-purple-50">Purple 50%</div>

        <SmithyPopGradient className="text-white col-span-4 h-24">
          Smithy Gradient
        </SmithyPopGradient>
      </div>
    </div>
  );
};
