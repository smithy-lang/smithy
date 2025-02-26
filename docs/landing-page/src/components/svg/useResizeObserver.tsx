"use client";
import React, { useState, useEffect } from "react";

export const useResizeObserver = () => {
  const self = typeof window !== "undefined" ? window : null;
  const [size, setSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    if (self === null) return;
    const observer = new ResizeObserver((entries) => {
      if (entries[0]) {
        const { width, height } = entries[0].contentRect;
        setSize({ width, height });
      }
    });

    if (self.document) {
      observer.observe(self.document.body);
    }

    return () => {
      if (self.document) {
        observer.unobserve(self.document.body);
      }
    };
  }, [self]);

  return size;
};
