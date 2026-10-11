export interface TutorDiscoverySubject {
  subjectId: number;
  subjectName: string;
  educationLevelId: number;
  educationLevelName: string;
}

export type TeachingMode = "ONLINE" | "OFFLINE" | "BOTH";

export interface TutorDiscoveryCard {
  tutorId: string;
  displayName: string;
  headline: string | null;
  shortBio: string | null;
  teachingMode: TeachingMode;
  area: string | null;
  pricePerSession: number;
  ratingAvg: number | null;
  reviewCount: number;
  verified: boolean;
  subjects: TutorDiscoverySubject[];
}

export interface TutorAvailabilitySlot {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface TutorDetail {
  tutorId: string;
  displayName: string;
  headline: string | null;
  bio: string | null;
  teachingMethod: string | null;
  teachingMode: TeachingMode;
  area: string | null;
  pricePerSession: number;
  experienceYears: number;
  ratingAvg: number | null;
  reviewCount: number;
  verified: boolean;
  subjects: TutorDiscoverySubject[];
  availabilitySlots: TutorAvailabilitySlot[];
}

export interface TutorSearchParams {
  subjectId?: number;
  levelId?: number;
  area?: string;
  mode?: TeachingMode;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  keyword?: string;
  dayOfWeek?: number;
  availableFrom?: string;
  availableTo?: string;
  page?: number;
  size?: number;
  sortBy?: "rating" | "relevance" | "price" | "experience";
  sortDirection?: "asc" | "desc";
}

export interface SpringPage<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface MatchScoreBreakdown {
  scheduleFit: number;
  ratingFit: number;
  budgetFit: number;
  overlappingSlots: number;
  subjectFit: number;
  levelFit: number;
}

export interface TutorMatchResult {
  tutorId: string;
  matchScore: number;
  scoreBreakdown: MatchScoreBreakdown;
  explanation: string;
  aiGenerated: boolean;
  tutorInfo: TutorDiscoveryCard;
}

export interface TutorMatchRequest {
  goalId: string;
  topN?: number;
}
