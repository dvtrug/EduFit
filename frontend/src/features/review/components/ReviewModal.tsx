"use client";

import React, { useState } from "react";
import { Star, X, Loader2, AlertCircle } from "lucide-react";
import { reviewApi } from "../api/reviewApi";
import type { ReviewItem } from "../types";

interface ReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  tutorId: string;
  studentId: string;
  tutorName?: string;
  existingReview?: ReviewItem | null;
  onSuccess?: (review: ReviewItem) => void;
}

export const ReviewModal: React.FC<ReviewModalProps> = ({
  isOpen,
  onClose,
  tutorId,
  studentId,
  tutorName,
  existingReview,
  onSuccess,
}) => {
  const isEditing = Boolean(existingReview);
  const [rating, setRating] = useState<number>(existingReview?.rating || 5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [comment, setComment] = useState<string>(existingReview?.comment || "");
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (rating < 1 || rating > 5) {
      setError("Vui lòng chọn số sao đánh giá (1 - 5 sao).");
      return;
    }

    const trimmedComment = comment.trim();
    if (trimmedComment.length < 10) {
      setError("Nội dung nhận xét phải có ít nhất 10 ký tự.");
      return;
    }
    if (trimmedComment.length > 1000) {
      setError("Nội dung nhận xét không được vượt quá 1000 ký tự.");
      return;
    }

    try {
      setLoading(true);
      let response;
      if (isEditing && existingReview) {
        response = await reviewApi.updateReview(existingReview.reviewId, {
          rating,
          comment: trimmedComment,
        });
      } else {
        response = await reviewApi.createReview({
          tutorId,
          studentId,
          rating,
          comment: trimmedComment,
        });
      }

      if (response && response.data) {
        if (onSuccess) onSuccess(response.data);
        onClose();
      } else {
        setError(response?.message || "Có lỗi xảy ra khi gửi đánh giá.");
      }
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-xl border border-stone-200 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-stone-100 bg-stone-50/70">
          <div>
            <h3 className="text-base font-bold text-neutral-900">
              {isEditing ? "Chỉnh sửa đánh giá gia sư" : "Đánh giá gia sư"}
            </h3>
            {tutorName && (
              <p className="text-xs text-neutral-500 mt-0.5">Dành cho: {tutorName}</p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-full text-neutral-400 hover:text-neutral-700 hover:bg-stone-200/60 transition-colors"
          >
            <X className="size-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-xs text-red-700 flex items-start gap-2.5">
              <AlertCircle className="size-4 shrink-0 text-red-600 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Chấm điểm sao */}
          <div className="space-y-2 text-center">
            <label className="text-xs font-semibold text-neutral-700 block">
              Mức độ hài lòng của bạn
            </label>
            <div className="flex items-center justify-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => {
                const isFilled = (hoverRating || rating) >= star;
                return (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                    className="p-1 transition-transform hover:scale-115 focus:outline-hidden"
                  >
                    <Star
                      className={`size-7 transition-colors ${
                        isFilled
                          ? "fill-amber-400 text-amber-400"
                          : "text-stone-300 hover:text-amber-200"
                      }`}
                    />
                  </button>
                );
              })}
            </div>
            <p className="text-xs text-amber-700 font-medium">
              {rating === 5 && "⭐ Tuyệt vời! Rất hài lòng"}
              {rating === 4 && "⭐ Tốt, hài lòng"}
              {rating === 3 && "⭐ Bình thường, tạm ổn"}
              {rating === 2 && "⭐ Chưa hài lòng"}
              {rating === 1 && "⭐ Kém, cần cải thiện"}
            </p>
          </div>

          {/* Ô nhập nhận xét */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <label htmlFor="review-comment" className="font-semibold text-neutral-800">
                Nhận xét chi tiết <span className="text-red-500">*</span>
              </label>
              <span
                className={`text-[11px] ${
                  comment.trim().length > 1000
                    ? "text-red-600 font-bold"
                    : "text-neutral-400"
                }`}
              >
                {comment.trim().length}/1000 ký tự (tối thiểu 10)
              </span>
            </div>
            <textarea
              id="review-comment"
              rows={4}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Chia sẻ trải nghiệm học tập, thái độ giảng dạy và phương pháp của gia sư..."
              className="w-full text-xs sm:text-sm p-3.5 rounded-2xl border border-stone-200 focus:outline-hidden focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 transition-all resize-none"
            />
          </div>

          {/* Nút hành động */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-neutral-600 hover:bg-stone-100 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-amber-600 hover:bg-amber-700 active:bg-amber-800 disabled:opacity-50 transition-colors flex items-center gap-2 shadow-sm"
            >
              {loading && <Loader2 className="size-3.5 animate-spin" />}
              {isEditing ? "Lưu thay đổi" : "Gửi đánh giá"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

