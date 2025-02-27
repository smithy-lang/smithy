import { StrictMode } from "react";
import { createRoot, hydrateRoot } from "react-dom/client";
import App from "./App.tsx";
import "./i18n/client.tsx";
import "./index.css";

if (window === undefined) {
  // static site generation
  hydrateRoot(document.getElementById("root")!, <App />);
} else {
  // development
  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
}
