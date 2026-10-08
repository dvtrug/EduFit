"use client";

import React from "react";
import Link from "next/link";

import { EduFitLogo } from "@/shared/ui/EduFitLogo";

interface AuthCardHeaderProps {
  title: string;
  subtitle: string;
}

export function AuthCardHeader({ title, subtitle }: AuthCardHeaderProps) {
  return (
    <div className="text-center space-y-2">
      <div className="flex justify-center pb-1">
        <EduFitLogo href="/" size="lg" />
      </div>
      <h1 className="text-2xl font-bold font-heading text-neutral-900">{title}</h1>
      <p className="text-sm text-neutral-500">{subtitle}</p>
    </div>
  );
}
