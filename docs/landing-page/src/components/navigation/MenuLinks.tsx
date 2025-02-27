import React from "react";
import { NavigationMenuLink } from "@/components/ui/navigation-menu";
import { cn } from "@/lib/utils";
import { useTranslation } from "react-i18next";

type NavigationMenuLinkProps = Parameters<typeof NavigationMenuLink>[0];

export const MenuLinks = (props: NavigationMenuLinkProps) => {
  const { t } = useTranslation("translation", { keyPrefix: "menu" });
  const CenterMenuLinks = t("items", { returnObjects: true }) as {
    title: string;
    href: string;
    className?: string;
  }[];
  return (
    <>
      {CenterMenuLinks.map((itm) => (
        <NavigationMenuLink
          href={itm.href}
          key={itm.title}
          {...props}
          className={cn(props.className, itm.className)}
        >
          {itm.title}
        </NavigationMenuLink>
      ))}
    </>
  );
};
