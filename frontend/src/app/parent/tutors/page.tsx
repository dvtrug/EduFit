"use client";

import React, { Suspense } from "react";
import { ParentDiscoveryView } from "@/features/discovery/components/views/ParentDiscoveryView";

export default function ParentTutorsPage() {
  return (
    <Suspense fallback={<div className="p-8 text-center text-xs text-neutral-500">Đang tải danh sách gia sư...</div>}>
      <ParentDiscoveryView />
    </Suspense>
  );
}
