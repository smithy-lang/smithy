import React from "react";
import { useTranslation } from "react-i18next";
import { CircleSection } from "@/components/ui/CircleSection";
import { useSupportedLanguages } from "@/components/landing-page/useSupportedLanguagesHook";

export const InformationCircles = () => {
  const { t } = useTranslation("translation", {
    keyPrefix: "informationCircles",
  });
  const clients = useSupportedLanguages({ filter: "clients" });
  const servers = useSupportedLanguages({ filter: "servers" });
  return (
    <div className="py-12 bg-smithy-light-gray">
      <CircleSection
        title={t("clients.title")}
        description={t("clients.description")}
        circleUrls={clients}
      />
      <CircleSection
        title={t("servers.title")}
        description={t("servers.description")}
        circleUrls={servers}
      />
    </div>
  );
};
