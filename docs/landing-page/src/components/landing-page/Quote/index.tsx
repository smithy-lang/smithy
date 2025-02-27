import React from "react";
import { useTranslation } from "react-i18next";

import { SmithyGlow } from "@/components/ui/SmithyGlow";

const altText = "Two large quotation marks in red and pink neon style";

export const Quote = () => {
  const { t } = useTranslation("translation", { keyPrefix: "heroQuote" });
  return (
    <SmithyGlow className="bg-[position:175%_-50%]">
      <div className="flex flex-col items-center justify-center p-6 pt-10 lg:p-20 text-center">
        <div className="h-12 mb-4">
          <img
            src={"/icons/dark/pull_quote.svg"}
            className="h-full dark:hidden"
            alt={altText}
          />
          <img
            src={"/icons/light/pull_quote.svg"}
            className="h-full hidden dark:block"
            alt={altText}
          />
        </div>
        <div className="text-xl lg:text-2xl">
          <span className="text-smithy-red-33">{t("partOne")}</span>
          <span className="">&nbsp;{t("partTwo")}</span>
        </div>
        <div className="mb-10 lg:mb-0 mt-10 text-center text-smithy-red text-l lg:text-xl">
          &ndash;&nbsp;{t("author")}
        </div>
      </div>
    </SmithyGlow>
  );
};
