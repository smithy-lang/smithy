import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";

export const Features = () => {
  const { t } = useTranslation("translation", { keyPrefix: "featuresList" });
  const featuresList = useMemo(
    () => [
      {
        title: t("one"),
        description: t("oneDescription"),
        icon: "/icons/dark/protocol.svg",
        altText:
          "Simple drawing of a smiling robot face with a round antenna on top",
      },
      {
        title: t("two"),
        description: t("twoDescription"),
        icon: "/icons/dark/codify.svg",
        altText:
          "Minimalist illustration of a DNA double helix with red and black stripes",
      },
      {
        title: t("three"),
        description: t("threeDescription"),
        icon: "/icons/dark/evolve.svg",
        altText: "Line drawing of a balance scale with red triangular pans",
      },
      {
        title: t("four"),
        description: t("fourDescription"),
        icon: "/icons/dark/resource.svg",
        altText: "Stylized icon of a paperclip",
      },
    ],
    [t],
  );
  return (
    <section className="bg-smithy-light-gray text-smithy-black w-screen py-12 px-8">
      <h3 className="text-3xl mb-4">{t("title")}</h3>
      <div className="flex flex-row justify-around flex-wrap gap-2 pb-4">
        {featuresList.map(({ title, description, icon, altText }) => (
          <div
            key={title}
            className="w-full md:w-1/3 flex justify-start items-start my-4"
          >
            <img className="w-12" src={icon} alt={altText} />
            <div className="ml-8">
              <h4 className="pb-2 font-bold text-lg">{title}</h4>
              <p className="max-w-md">{description}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};
