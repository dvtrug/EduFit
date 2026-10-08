"use client";

import React from "react";
import Link from "next/link";

interface AuthCardHeaderProps {
  title: string;
  subtitle: string;
}

export function AuthCardHeader({ title, subtitle }: AuthCardHeaderProps) {
  return (
    <div className="text-center space-y-2">
      <Link href="/" className="inline-flex items-center gap-2 group">
        <div className="size-10 rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-400 flex items-center justify-center text-white font-heading font-black text-xl shadow-md group-hover:scale-105 transition-transform">
          E
        </div>
        <span className="font-heading text-2xl font-bold tracking-tight text-neutral-900 group-hover:text-amber-600 transition-colors">
          EduFit
        </span>
      </Link>
      <h1 className="text-2xl font-bold font-heading text-neutral-900">{title}</h1>
      <p className="text-sm text-neutral-500">{subtitle}</p>
    </div>
  );
}
