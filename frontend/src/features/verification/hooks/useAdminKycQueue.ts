"use client";

import { useState, useCallback, useEffect } from "react";
import { verificationService, PendingQueueItem } from "@/services/verification";

export function useAdminKycQueue() {
  const [items, setItems] = useState<PendingQueueItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedItem, setSelectedItem] = useState<PendingQueueItem | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

  const loadQueue = useCallback(async () => {
    setLoading(true);
    try {
      const res = await verificationService.getPendingQueue(0, 20);
      setItems(res.items || []);
    } catch (err) {
      console.error("Lỗi khi tải hàng đợi KYC:", err);
      // Giữ mảng rỗng nếu backend chưa có request pending nào hoặc xảy ra lỗi
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadQueue();
  }, [loadQueue]);

  const handleOpenReview = useCallback((item: PendingQueueItem) => {
    setSelectedItem(item);
    setIsReviewModalOpen(true);
  }, []);

  const handleCloseReview = useCallback(() => {
    setIsReviewModalOpen(false);
  }, []);

  const handleReviewSuccess = useCallback(() => {
    setIsReviewModalOpen(false);
    void loadQueue();
  }, [loadQueue]);

  return {
    items,
    loading,
    selectedItem,
    isReviewModalOpen,
    loadQueue,
    handleOpenReview,
    handleCloseReview,
    handleReviewSuccess,
  };
}
