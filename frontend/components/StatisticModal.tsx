"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";

export interface BreakdownItem {
  label: string;
  value: string | number;
  detail?: string;
  progress?: number; // 0 - 100
  highlight?: boolean;
}

export interface StatisticModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  value: string | number;
  badge?: {
    text: string;
    colorClass: string;
  };
  explanation: string;
  breakdownTitle: string;
  breakdownItems: BreakdownItem[];
  emptyBreakdownMessage?: string;
  primaryAction: {
    label: string;
    href: string;
  };
}

export default function StatisticModal({
  isOpen,
  onClose,
  title,
  value,
  badge,
  explanation,
  breakdownTitle,
  breakdownItems,
  emptyBreakdownMessage = "Detailed breakdown metrics are not currently available for this category.",
  primaryAction,
}: StatisticModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  // Focus trap and keyboard handling
  useEffect(() => {
    if (!isOpen) return;

    // Focus close button on open
    const focusTimer = setTimeout(() => {
      closeButtonRef.current?.focus();
    }, 50);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
        return;
      }

      // Focus trap for Tab key
      if (e.key === "Tab" && modalRef.current) {
        const focusableElements = modalRef.current.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === firstElement) {
            e.preventDefault();
            lastElement.focus();
          }
        } else {
          if (document.activeElement === lastElement) {
            e.preventDefault();
            firstElement.focus();
          }
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      clearTimeout(focusTimer);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/60 backdrop-blur-xs transition-opacity duration-200"
      onClick={onClose}
    >
      <div
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="stat-modal-title"
        aria-describedby="stat-modal-desc"
        className="bg-white rounded-2xl border border-gray-200 shadow-2xl max-w-lg w-full p-6 text-gray-900 relative flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 motion-reduce:animate-none duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header - Fixed */}
        <div className="flex items-start justify-between gap-4 pb-3 border-b border-gray-100 shrink-0">
          <div>
            <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-600 block">
              Metric Deep Dive
            </span>
            <h2 id="stat-modal-title" className="text-xl font-bold text-gray-900 mt-0.5">
              {title}
            </h2>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500"
            aria-label="Close modal"
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Scrollable Modal Content */}
        <div className="overflow-y-auto py-4 space-y-4 pr-1 flex-1">
          {/* Highlight Score Banner */}
          <div className="bg-gray-50/80 border border-gray-100 rounded-xl p-4 flex items-center justify-between">
            <div>
              <span className="text-xs text-gray-500 font-medium block">
                Current Evaluation
              </span>
              <div className="flex items-center gap-2.5 mt-1">
                <span className="text-3xl font-extrabold text-gray-900 tracking-tight">
                  {value}
                </span>
                {badge && (
                  <span
                    className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${badge.colorClass}`}
                  >
                    {badge.text}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Explanation */}
          <div>
            <p id="stat-modal-desc" className="text-xs text-gray-600 leading-relaxed">
              {explanation}
            </p>
          </div>

          {/* Breakdown List */}
          <div className="space-y-2.5 pt-1">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gray-700">
              {breakdownTitle}
            </h3>

            {breakdownItems.length === 0 ? (
              <div className="p-4 bg-gray-50 border border-dashed border-gray-200 rounded-xl text-center">
                <p className="text-xs text-gray-500">{emptyBreakdownMessage}</p>
              </div>
            ) : (
              <div className="space-y-2">
                {breakdownItems.map((item, idx) => (
                  <div
                    key={idx}
                    className={`p-3 rounded-xl border text-xs transition ${
                      item.highlight
                        ? "bg-indigo-50/60 border-indigo-200"
                        : "bg-gray-50/70 border-gray-100 hover:bg-gray-50"
                    }`}
                  >
                    <div className="flex items-center justify-between font-medium">
                      <span className="text-gray-900 font-semibold">{item.label}</span>
                      <span className="text-gray-900 font-bold">{item.value}</span>
                    </div>

                    {item.detail && (
                      <p className="text-[11px] text-gray-500 mt-0.5 leading-normal">{item.detail}</p>
                    )}

                    {typeof item.progress === "number" && (
                      <div className="w-full bg-gray-200 h-1.5 rounded-full overflow-hidden mt-2">
                        <div
                          className="bg-indigo-600 h-1.5 rounded-full transition-all duration-300 motion-reduce:transition-none"
                          style={{ width: `${Math.min(100, Math.max(0, item.progress))}%` }}
                        />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions - Fixed */}
        <div className="pt-3 flex items-center justify-end gap-3 border-t border-gray-100 shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-gray-400"
          >
            Close
          </button>
          <Link
            href={primaryAction.href}
            className="px-4 py-2 bg-gray-950 hover:bg-black text-white text-xs font-bold rounded-xl transition shadow-xs flex items-center gap-1.5 focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-indigo-500"
            onClick={onClose}
          >
            {primaryAction.label}
          </Link>
        </div>
      </div>
    </div>
  );
}
