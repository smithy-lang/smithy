import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";

const tmDivClasses = "text-smithy-light-gray text-xs text-right my-1";

export const Footer = () => {
  const year = useMemo(() => new Date().getFullYear(), []);
  const { t } = useTranslation("translation", { keyPrefix: "footer" });

  return (
    <footer className="bg-smithy-black flex flex-row space-between px-2 py-4 lg:p-8 w-full">
      <div className="ml-2 lg:ml-8">
        <a className="h-24 w-24" href="/">
          <img
            className="h-24 w-24"
            src="/logos/smithy_anvil_red.svg"
            alt={t("logoAlt")}
          />
        </a>
      </div>
      <div className="flex-1" />
      <div className="mr-2 lg:mr-8 my-6 flex flex-col justify-between">
        <div className={tmDivClasses}>
          &copy;{t("Copyright Smithy")} {year}
        </div>
      </div>
    </footer>
  );
};
