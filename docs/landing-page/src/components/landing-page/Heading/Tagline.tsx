import React from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button.tsx";

export const Tagline = () => {
  const { t } = useTranslation("translation", { keyPrefix: "title" });
  return (
    <div className="max-w-xl">
      <h1 className="text-3xl md:text-4xl text-left leading-tight">
        <span className="block">{t("lineOne")}</span>
        <span className="block">{t("lineTwo")}</span>
        <span className="text-smithy-red-33 block font-bold">
          {t("tagline")}
        </span>
      </h1>
      <p className="pt-7 text-lg">{t("subtitle")}</p>
      <div className="mt-8">
        <a className="flex flex-col md:flex-row" href="/2.0/quickstart.html">
          <Button variant="gradient-outline" darkBg className="hidden sm:flex">
            Get Started
          </Button>
        </a>
      </div>
    </div>
  );
};
