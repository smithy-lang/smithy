import React, { useMemo } from "react";
import { useTranslation, Trans } from "react-i18next";

interface useSupportedLanguagesProps {
  filter?: "clients" | "servers";
}

export const useSupportedLanguages = (props?: useSupportedLanguagesProps) => {
  const { t } = useTranslation("translation", {
    keyPrefix: "supportedLanguages",
  });
  return useMemo(() => {
    const serverUrls = [
      {
        src: "/icons/duke.svg",
        name: "Java",
        alt: t("java.alt"),
        trademark: t("java.trademark"),
        location: "https://github.com/smithy-lang/smithy-java",
      },
      {
        src: "/icons/ts.svg",
        name: "TypeScript",
        alt: t("ts.alt"),
        trademark: t("ts.trademark"),
        location: "https://github.com/smithy-lang/smithy-typescript",
      },
      {
        src: "/icons/rust.svg",
        name: "Rust",
        alt: t("rust.alt"),
        trademark: (
          <>
            <Trans t={t}>rust.trademark</Trans>
            <a
              className="text-smithy-red-15 hover:underline block"
              href="https://foundation.rust-lang.org/policies/logo-policy-and-media-guide/#the-rust-trademarks"
              aria-label="Rust Logo License"
            >
              {t("Source")}
            </a>
          </>
        ),
        location: "https://github.com/awslabs/smithy-rs",
      },
      {
        src: "/icons/scala.svg",
        name: "Scala",
        alt: t("scala.alt"),
        trademark: t("scala.trademark"),
        location: "https://github.com/disneystreaming/smithy4s",
      },
    ];
    const clientUrls = [
      {
        src: "/icons/python.svg",
        name: "Python",
        alt: t("python.alt"),
        trademark: t("python.trademark"),
        location: "https://github.com/smithy-lang/smithy-python",
      },
      {
        src: "/icons/go.svg",
        name: "Go",
        alt: t("go.alt"),
        trademark: t("go.trademark"),
        location: "https://github.com/aws/smithy-go",
      },
      {
        src: "/icons/kotlin.svg",
        name: "Kotlin",
        alt: t("kotlin.alt"),
        trademark: t("kotlin.trademark"),
        location: "https://github.com/awslabs/smithy-kotlin",
      },
      {
        src: "/icons/swift.svg",
        name: "Swift",
        alt: t("swift.alt"),
        trademark: t("swift.trademark"),
        location: "https://github.com/awslabs/smithy-swift",
      },
      {
        src: "/icons/ruby.png",
        name: "Ruby",
        alt: t("ruby.alt"),
        trademark: t("ruby.trademark"),
        location: "https://github.com/awslabs/smithy-ruby",
      },
    ];

    switch (props?.filter) {
      case "servers":
        return serverUrls;
      case "clients":
      default:
        return [...serverUrls, ...clientUrls];
    }
  }, [props?.filter, t]);
};
