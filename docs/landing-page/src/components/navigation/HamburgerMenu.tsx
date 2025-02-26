import React from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { HamburgerMenuIcon } from "@radix-ui/react-icons";
import {
  NavigationMenu,
  NavigationMenuList,
  NavigationMenuItem,
} from "@/components/ui/navigation-menu";
import { MenuLinks } from "./MenuLinks";
import { useTranslation } from "react-i18next";

export const HamburgerMenu = () => {
  const { t } = useTranslation("translation", { keyPrefix: "menu" });
  return (
    <Sheet>
      <SheetTrigger className="h-9 w-9 flex justify-center items-center hover:bg-accent rounded-full">
        <HamburgerMenuIcon />
      </SheetTrigger>
      <SheetContent>
        <SheetHeader>
          <SheetTitle className="mb-8 flex items-center justify-center">
            <a href="/">
              <img
                src="/logos/smithy_logo_lt.svg"
                alt={t("logoAlt")}
                className="h-8 w-auto"
              />
            </a>
          </SheetTitle>
          <NavigationMenu orientation="vertical" className="w-full max-w-full">
            <NavigationMenuList aria-orientation="vertical" className="w-full">
              <NavigationMenuItem
                aria-orientation="vertical"
                className="flex flex-col gap-4 w-full"
              >
                <MenuLinks className="px-10 py-2" />
              </NavigationMenuItem>
            </NavigationMenuList>
          </NavigationMenu>
        </SheetHeader>
      </SheetContent>
    </Sheet>
  );
};
