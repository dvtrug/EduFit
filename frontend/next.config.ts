import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async redirects() {
    return [
      {
        source: "/admin/kyc-queue",
        destination: "/admin/verification-queue",
        permanent: true,
      },
      {
        source: "/admin/learners",
        destination: "/admin/links",
        permanent: true,
      },
      {
        source: "/admin/tutors-management",
        destination: "/admin/verification-queue",
        permanent: true,
      },
      {
        source: "/admin/disputes",
        destination: "/admin/dashboard",
        permanent: true,
      },

    ];
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
};

export default nextConfig;
