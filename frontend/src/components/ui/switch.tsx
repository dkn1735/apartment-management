"use client";

import * as React from "react";
import { cn } from "cn";
import { Switch as SwitchPrimitive } from "radix-ui";

/** Công tắc bật tắt: tắt là viền --input, bật là --success. */
function Switch({ className, ...props }: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        "peer relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full border border-input bg-muted p-0.5 transition-colors duration-150 ease-out disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive data-[state=checked]:border-success data-[state=checked]:bg-success",
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className="pointer-events-none block size-[18px] rounded-full border border-input bg-card transition-transform duration-150 ease-out data-[state=checked]:translate-x-5 data-[state=checked]:border-transparent"
      />
    </SwitchPrimitive.Root>
  );
}

export { Switch };
