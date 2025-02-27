import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-3xl text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground shadow hover:bg-primary/90",
        gradient:
          "border border-input bg-gradient-to-r from-secondary to-primary shadow-sm hover:bg-accent hover:text-accent-foreground",
        destructive:
          "bg-destructive text-destructive-foreground shadow-sm hover:bg-destructive/90",
        outline:
          "border border-input bg-background shadow-sm hover:bg-accent hover:text-accent-foreground",
        "gradient-outline":
          "bg-gradient-to-r from-secondary to-primary text-primary-foreground shadow hover:bg-background/90",
        secondary:
          "bg-secondary text-secondary-foreground shadow-sm hover:bg-secondary/80",
        ghost: "hover:bg-accent hover:text-accent-foreground",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-9 px-6 py-2",
        sm: "h-8 rounded-md px-4 text-xs",
        lg: "h-10 rounded-md px-9",
        icon: "h-9 w-9",
        "gradient-outline": "",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  darkBg?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant,
      size,
      asChild = false,
      children,
      darkBg = false,
      ...props
    },
    ref,
  ) => {
    const Comp = asChild ? Slot : "button";

    let renderedChildren = children;
    if (variant === "gradient-outline") {
      size = "gradient-outline";
      renderedChildren = (
        <span
          className={`flex w-full bg-background text-foreground rounded-3xl h-8 py-1.5 px-6 mx-px my-px hover:bg-background/90 hover:text-foreground ${darkBg ? "bg-smithy-black text-white hover:bg-smithy-black/70 hover:text-white" : ""}`}
        >
          {children}
        </span>
      );
    }

    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      >
        {renderedChildren}
      </Comp>
    );
  },
);
Button.displayName = "Button";

export { Button, buttonVariants };
