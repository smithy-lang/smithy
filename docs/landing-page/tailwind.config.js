/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{html,ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        ember: ["sans-serif"],
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      colors: {
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        "smithy-red": "hsl(var(--smithy-red))",
        "smithy-red-33": "hsl(var(--smithy-red-33))",
        "smithy-red-15": "hsl(var(--smithy-red-15))",
        "smithy-black": "hsl(var(--smithy-black))",
        "smithy-light-gray": "hsl(var(--smithy-light-gray))",
        "smithy-dark-gray": "hsl(var(--smithy-dark-gray))",
        "smithy-purple": "hsl(var(--smithy-purple))",
        "smithy-purple-50": "hsl(var(--smithy-purple-50))",
        "smithy-rose": "hsl(var(--smithy-rose))",
        "smithy-plum": "hsl(var(--smithy-plum))",
        "smithy-gradient-midpoint": "hsl(var(--smithy-gradient-midpoint))",
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        chart: {
          1: "hsl(var(--chart-1))",
          2: "hsl(var(--chart-2))",
          3: "hsl(var(--chart-3))",
          4: "hsl(var(--chart-4))",
          5: "hsl(var(--chart-5))",
        },
      },
      backgroundImage: {
        "radial-gradient": "radial-gradient(var(--tw-gradient-stops))",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
  safelist: ["sm:hidden"],
};
