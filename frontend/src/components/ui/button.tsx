"use client";

import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";
import { Slot } from "radix-ui";
import { Spinner } from "@/components/ui/spinner";

const buttonVariants = cva(
  "inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-full border border-transparent font-medium whitespace-nowrap no-underline transition-colors duration-150 ease-out select-none disabled:cursor-not-allowed disabled:opacity-50 aria-busy:cursor-progress [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        primary:
          "bg-primary text-primary-foreground hover:bg-primary-hover hover:text-primary-foreground active:bg-primary-hover",
        secondary: "bg-secondary text-secondary-foreground hover:bg-accent hover:text-accent-foreground",
        outline: "border-input bg-card text-foreground hover:bg-accent hover:text-accent-foreground",
        ghost: "text-foreground hover:bg-accent hover:text-accent-foreground",
        destructive:
          "bg-destructive text-destructive-foreground hover:bg-destructive-hover hover:text-destructive-foreground active:bg-destructive-hover",
        link: "h-auto rounded-sm px-0 text-primary underline underline-offset-3 hover:text-primary-hover",
      },
      size: {
        sm: "h-8 px-3 text-[13px]",
        md: "h-10 px-4 text-sm",
        lg: "h-12 px-6 text-[15px]",
        icon: "size-10",
        "icon-sm": "size-8",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

type ButtonProps = React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
    /** Đang xử lý: hiện vòng quay, đặt aria-busy và aria-disabled, chặn bấm; không đặt disabled để focus không rơi mất. */
    loading?: boolean;
  };

function Button({
  className,
  variant = "primary",
  size = "md",
  asChild = false,
  loading = false,
  children,
  onClick,
  type = "button",
  "aria-disabled": ariaDisabled,
  ...props
}: ButtonProps) {
  const Comp = asChild ? Slot.Root : "button";

  function handleClick(event: React.MouseEvent<HTMLButtonElement>) {
    if (loading || ariaDisabled === true || ariaDisabled === "true") {
      // Chặn cả submit của form trong lúc đang tải
      event.preventDefault();
      return;
    }
    onClick?.(event);
  }

  return (
    <Comp
      {...props}
      data-slot="button"
      data-variant={variant}
      data-size={size}
      type={asChild ? undefined : type}
      aria-busy={loading || undefined}
      aria-disabled={loading || ariaDisabled || undefined}
      className={cn(buttonVariants({ variant, size }), className)}
      onClick={handleClick}
    >
      {asChild ? (
        children
      ) : (
        <>
          {loading && (
            <>
              <Spinner />
              <span className="sr-only">Đang tải: </span>
            </>
          )}
          {children}
        </>
      )}
    </Comp>
  );
}

export { Button, buttonVariants, type ButtonProps };
