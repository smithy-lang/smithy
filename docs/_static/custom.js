// Reliable mobile sidebar toggles.
//
// sphinx-book-theme and its pydata base both bind click handlers to the same
// sidebar toggle buttons. PyData moves sidebar contents into <dialog> elements,
// while SBT also styles the original sidebars as off-canvas panels. Under
// browser zoom the page can enter that mobile path and the handlers race,
// leaving the toggles apparently inert. Intercept clicks before the theme
// handlers and drive the original sidebars directly.
(function () {
  var states = {
    primary: false,
    secondary: false,
  };

  var configs = {
    primary: {
      toggleSelector: ".primary-toggle",
      sidebarId: "pst-primary-sidebar",
      modalId: "pst-primary-sidebar-modal",
      openMarginProperty: "margin-left",
    },
    secondary: {
      toggleSelector: ".secondary-toggle",
      sidebarId: "pst-secondary-sidebar",
      modalId: "pst-secondary-sidebar-modal",
      openMarginProperty: "margin-right",
    },
  };

  function sidebar(name) {
    return document.getElementById(configs[name].sidebarId);
  }

  function modal(name) {
    return document.getElementById(configs[name].modalId);
  }

  function toggleButtons(name) {
    return document.querySelectorAll(configs[name].toggleSelector);
  }

  function moveContents(from, to) {
    Array.from(from.childNodes).forEach(function (node) {
      to.appendChild(node);
    });
    Array.from(from.classList).forEach(function (className) {
      from.classList.remove(className);
      to.classList.add(className);
    });
  }

  function restoreThemeModal(name) {
    var m = modal(name);
    var s = sidebar(name);
    if (!m || !s) return;
    if (m.open) {
      m.close();
      return;
    }
    if (m.classList.contains("bd-sidebar-primary") || m.classList.contains("bd-sidebar-secondary")) {
      moveContents(m, s);
    }
  }

  function setOpen(name, open) {
    restoreThemeModal(name);
    var s = sidebar(name);
    if (!s) return;
    states[name] = open;
    s.toggleAttribute("open", open);
    toggleButtons(name).forEach(function (button) {
      button.setAttribute("aria-expanded", String(open));
    });
    if (open) {
      s.style.setProperty(configs[name].openMarginProperty, "0", "important");
      s.style.setProperty("visibility", "visible", "important");
      s.style.setProperty("z-index", "1055", "important");
    } else {
      s.style.removeProperty(configs[name].openMarginProperty);
      s.style.removeProperty("visibility");
      s.style.removeProperty("z-index");
    }
  }

  function clickedToggle(el) {
    if (el.closest(configs.primary.toggleSelector)) return "primary";
    if (el.closest(configs.secondary.toggleSelector)) return "secondary";
    return null;
  }

  window.addEventListener(
    "click",
    function (e) {
      var el = e.target;
      if (!el || !el.closest) return;

      var toggleName = clickedToggle(el);
      if (toggleName) {
        e.preventDefault();
        e.stopImmediatePropagation();
        setOpen(toggleName, !states[toggleName]);
        return;
      }

      // Close on tap of a nav link inside, or anywhere outside.
      Object.keys(configs).forEach(function (name) {
        var s = sidebar(name);
        if (states[name] && s) {
          if (!s.contains(el) || el.closest("a")) setOpen(name, false);
        }
      });
    },
    true
  );

  window.addEventListener("resize", function () {
    Object.keys(configs).forEach(function (name) {
      var visibleButton = Array.from(toggleButtons(name)).find(function (button) {
        return window.getComputedStyle(button).display !== "none";
      });
      if (states[name] && !visibleButton) setOpen(name, false);
    });
  });
})();
