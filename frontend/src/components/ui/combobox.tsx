"use client";

import * as React from "react";
import { cn } from "cn";
import { CheckIcon, ChevronsUpDownIcon, SearchIcon } from "lucide-react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Spinner } from "@/components/ui/spinner";

export type ComboboxOption = {
  value: string;
  /** Nhãn ngắn hiển thị trên nút mở khi mục này đang được chọn. */
  label: string;
  /** Nội dung dòng tùy biến trong danh sách; bỏ trống thì hiển thị `label`. */
  content?: React.ReactNode;
  disabled?: boolean;
};

type ComboboxProps = {
  id?: string;
  value: string;
  onValueChange: (value: string) => void;
  onBlur?: () => void;
  options: readonly ComboboxOption[];
  /** Nhãn hiển thị khi mục đã chọn không còn nằm trong trang kết quả hiện tại. */
  selectedLabel?: string;
  placeholder?: string;
  searchValue: string;
  onSearchChange: (query: string) => void;
  searchPlaceholder?: string;
  /** Nhãn trợ năng cho ô tìm kiếm bên trong khung mở. */
  searchLabel?: string;
  /** Nhãn trợ năng cho danh sách kết quả. */
  listLabel?: string;
  emptyText?: string;
  errorText?: string;
  loading?: boolean;
  disabled?: boolean;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  "aria-invalid"?: boolean;
  "aria-describedby"?: string;
  "aria-required"?: boolean;
  className?: string;
};

/**
 * Ô tìm chọn một mục từ danh sách dài (WAI-ARIA 1.2 Combobox, spec 0014 AC-17, AC-18).
 * Ghép trực tiếp với `FormField` qua `id`, `ref`, `onBlur` và các thuộc tính `aria-*`.
 */
export const Combobox = React.forwardRef<HTMLButtonElement, ComboboxProps>(function Combobox(
  {
    id,
    value,
    onValueChange,
    onBlur,
    options,
    selectedLabel,
    placeholder = "Chọn một mục",
    searchValue,
    onSearchChange,
    searchPlaceholder = "Nhập từ khóa để tìm...",
    searchLabel = "Tìm trong danh sách",
    listLabel = "Danh sách lựa chọn",
    emptyText = "Không tìm thấy kết quả phù hợp.",
    errorText,
    loading = false,
    disabled = false,
    open: controlledOpen,
    onOpenChange,
    className,
    ...aria
  },
  ref,
) {
  const [uncontrolledOpen, setUncontrolledOpen] = React.useState(false);
  const open = controlledOpen ?? uncontrolledOpen;
  const openRef = React.useRef(open);
  const searchInputRef = React.useRef<HTMLInputElement>(null);
  const baseId = React.useId();
  const listboxId = `${baseId}-listbox`;
  const [activeIndex, setActiveIndex] = React.useState(0);

  const enabledIndices = options.reduce<number[]>((acc, opt, idx) => {
    if (!opt.disabled) acc.push(idx);
    return acc;
  }, []);

  const currentIndex =
    enabledIndices.length === 0 ? -1 : enabledIndices.includes(activeIndex) ? activeIndex : enabledIndices[0];
  const activeOptionId = currentIndex >= 0 ? `${baseId}-opt-${currentIndex}` : undefined;
  const displayLabel = selectedLabel ?? options.find((opt) => opt.value === value)?.label;

  function setOpen(next: boolean) {
    openRef.current = next;
    if (controlledOpen === undefined) setUncontrolledOpen(next);
    onOpenChange?.(next);
    if (next) {
      const idx = options.findIndex((opt) => opt.value === value && !opt.disabled);
      setActiveIndex(idx >= 0 ? idx : 0);
    } else {
      onBlur?.();
    }
  }

  function chonMuc(opt: ComboboxOption) {
    if (opt.disabled) return;
    onValueChange(opt.value);
    setOpen(false);
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (enabledIndices.length === 0) return;
    const pos = enabledIndices.indexOf(currentIndex);
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex(enabledIndices[(pos + 1) % enabledIndices.length]);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex(enabledIndices[(pos - 1 + enabledIndices.length) % enabledIndices.length]);
    } else if (event.key === "Home") {
      event.preventDefault();
      setActiveIndex(enabledIndices[0]);
    } else if (event.key === "End") {
      event.preventDefault();
      setActiveIndex(enabledIndices[enabledIndices.length - 1]);
    } else if (event.key === "Enter" && currentIndex >= 0) {
      event.preventDefault();
      chonMuc(options[currentIndex]);
    }
  }

  return (
    <Popover modal open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          ref={ref}
          id={id}
          type="button"
          role="combobox"
          aria-expanded={open}
          aria-controls={listboxId}
          aria-haspopup="listbox"
          disabled={disabled}
          onBlur={() => {
            if (!openRef.current) onBlur?.();
          }}
          className={cn(
            "flex h-10 w-full cursor-pointer items-center justify-between gap-2 rounded-md border border-input bg-card px-3 text-left text-base text-foreground transition-colors duration-150 ease-out disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive sm:text-sm",
            !displayLabel && "text-muted-foreground",
            className,
          )}
          {...aria}
        >
          <span className="truncate">{displayLabel || placeholder}</span>
          <ChevronsUpDownIcon aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
        </button>
      </PopoverTrigger>
      {!open && <span id={listboxId} hidden />}
      <PopoverContent
        aria-label={listLabel}
        onOpenAutoFocus={(event) => {
          event.preventDefault();
          searchInputRef.current?.focus();
        }}
        className="flex flex-col gap-2 p-2"
      >
        <div className="relative flex items-center">
          <SearchIcon aria-hidden="true" className="pointer-events-none absolute left-3 size-4 text-muted-foreground" />
          <input
            ref={searchInputRef}
            type="text"
            role="combobox"
            aria-expanded={true}
            aria-controls={listboxId}
            aria-autocomplete="list"
            aria-activedescendant={activeOptionId}
            aria-label={searchLabel}
            value={searchValue}
            maxLength={100}
            autoComplete="off"
            placeholder={searchPlaceholder}
            onChange={(event) => {
              onSearchChange(event.target.value);
              setActiveIndex(0);
            }}
            onKeyDown={handleKeyDown}
            className="h-10 w-full rounded-md border border-input bg-card pr-3 pl-9 text-base text-foreground placeholder:text-muted-foreground sm:text-sm"
          />
        </div>
        {errorText ? (
          <p role="alert" className="px-2 py-3 type-ui text-destructive">
            {errorText}
          </p>
        ) : loading ? (
          <div
            role="status"
            aria-live="polite"
            className="flex items-center gap-2 px-2 py-3 type-ui text-muted-foreground"
          >
            <Spinner />
            <span>Đang tải danh sách...</span>
          </div>
        ) : options.length === 0 ? (
          <p role="status" aria-live="polite" className="px-2 py-3 type-ui text-muted-foreground">
            {emptyText}
          </p>
        ) : null}
        <div
          id={listboxId}
          role="listbox"
          aria-label={listLabel}
          className={cn(
            "max-h-60 overflow-y-auto",
            (loading || Boolean(errorText) || options.length === 0) && "hidden",
          )}
        >
          {options.map((opt, idx) => {
            const isSelected = opt.value === value;
            const isActive = idx === currentIndex;
            return (
              <div
                key={opt.value}
                id={`${baseId}-opt-${idx}`}
                role="option"
                tabIndex={-1}
                aria-selected={isSelected}
                aria-disabled={opt.disabled || undefined}
                onMouseEnter={() => {
                  if (!opt.disabled) setActiveIndex(idx);
                }}
                onClick={() => chonMuc(opt)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    chonMuc(opt);
                  }
                }}
                className={cn(
                  "relative flex min-h-10 w-full cursor-pointer items-start justify-between gap-2 rounded-sm py-2 pr-8 pl-2.5 text-sm select-none",
                  isActive && "bg-accent text-accent-foreground",
                  isSelected && !isActive && "bg-primary-soft",
                  opt.disabled && "pointer-events-none opacity-50",
                )}
              >
                <div className="min-w-0 flex-1">{opt.content ?? opt.label}</div>
                {isSelected && (
                  <span className="absolute top-2.5 right-2 flex size-4 items-center justify-center">
                    <CheckIcon aria-hidden="true" className="size-4 text-primary" />
                  </span>
                )}
              </div>
            );
          })}
        </div>
      </PopoverContent>
    </Popover>
  );
});
