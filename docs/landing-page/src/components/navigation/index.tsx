import React from "react";
import { useTranslation } from "react-i18next";
import { Icons } from "@/components/ui/icons";
import { Button } from "@/components/ui/button";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
} from "@/components/ui/navigation-menu";
import { HamburgerMenu } from "./HamburgerMenu";
import { MenuLinks } from "./MenuLinks";

export const TopNavigation = () => {
  const { t } = useTranslation("translation", { keyPrefix: "navigation" });
  const githubAlt = t("githubLabel");
  return (
    <div className="bg-smithy-black text-white h-(--nav-offset) fixed top-0 z-50">
      <NavigationMenu>
        <NavigationMenuList className="h-(--nav-offset) px-10 w-screen flex flex-row justify-between">
          <NavigationMenuItem className="flex-1">
            <div className="flex-1 min-w-36">
              <a href="/" aria-label="Return to index">
                <img
                  className="max-w-36 h-auto"
                  src={"/logos/smithy_logo_lt.svg"}
                  alt={t("logoAlt")}
                />
              </a>
            </div>
          </NavigationMenuItem>
          <NavigationMenuItem className="w-1/3 flex flex-row gap-x-8 justify-between lg:flex hidden">
            <MenuLinks className="hover:bg-linear-to-l hover:from-primary hover:to-secondary inline-block hover:text-transparent hover:bg-clip-text" />
          </NavigationMenuItem>
          <NavigationMenuItem className="flex-1 flex justify-end items-end">
            <a
              target="_black"
              rel="noopener noreferrer"
              href="https://github.com/smithy-lang/smithy"
              aria-label={githubAlt}
            >
              <Button size="icon" variant={"ghost"} aria-label={githubAlt}>
                <Icons.gitHub />
              </Button>
            </a>
          </NavigationMenuItem>
          <NavigationMenuItem className="lg:hidden">
            <div>
              <HamburgerMenu />
            </div>
          </NavigationMenuItem>
        </NavigationMenuList>
      </NavigationMenu>
    </div>
  );
};
