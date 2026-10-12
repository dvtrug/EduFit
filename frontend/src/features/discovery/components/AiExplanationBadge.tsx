"use client";

import React from "react";
import { Sparkles, Bot } from "lucide-react";

export interface AiExplanationBadgeProps {
  explanation: string;
  aiGenerated?: boolean;
}

export function AiExplanationBadge({ explanation, aiGenerated = false }: AiExplanationBadgeProps) {
  return (
    <div className="relative overflow-hidden p-4 sm:p-5 rounded-2xl bg-gradient-to-br from-amber-500/10 via-orange-500/5 to-white border border-amber-200/90 shadow-2xs space-y-2">
      <div className="flex items-center justify-between">
        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-100 text-amber-900 text-[11px] font-bold">
          <Sparkles className="size-3 text-amber-600" />
          <span>{aiGenerated ? "AI Grounded Match Explanation" : "Phân tích mức độ tương thích"}</span>
        </div>
        {aiGenerated && (
          <span className="text-[10px] text-amber-700 font-semibold flex items-center gap-1">
            <Bot className="size-3" /> Sinh bởi AI Model
          </span>
        )}
      </div>

      <p className="text-xs text-neutral-800 leading-relaxed font-medium">
        {explanation}
      </p>
    </div>
  );
}
