export interface NavItem {
  label: string;
  href: string;
  icon: string;
  badge?: string;
  description?: string;
}

/**
 * Strict administrator-only navigation items.
 * Administrators must NEVER see student-oriented items (e.g., /dashboard, /resume, /practice, /quiz).
 */
export const ADMIN_NAV_ITEMS: readonly NavItem[] = [
  { label: "Admin Dashboard", href: "/admin", icon: "🛡️", description: "Platform overview & KPIs" },
  { label: "User Governance", href: "/admin/users", icon: "👥", description: "Candidate management & status" },
  { label: "Mentor Governance", href: "/admin/mentors", icon: "🎓", description: "Mentor applications & approval" },
  { label: "Mentorship Oversight", href: "/admin/mentorships", icon: "🤝", description: "Active mentorships & capacity" },
  { label: "Support Inbox", href: "/admin/messages", icon: "📬", description: "User support tickets" },
  { label: "Audit Logs", href: "/admin/audit-logs", icon: "📋", description: "Immutable governance records" },
  { label: "Platform Settings", href: "/admin/settings", icon: "⚙️", description: "System configuration & limits" },
];

/**
 * Student/candidate navigation items.
 */
export const STUDENT_NAV_ITEMS: readonly NavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: "📊" },
  { label: "AI Career Advisor", href: "/chat", icon: "🤖" },
  { label: "Messages", href: "/messages", icon: "💬" },
  { label: "Mentorship", href: "/mentorship", icon: "👥" },
  { label: "Resume Analyzer", href: "/resume", icon: "📄" },
  { label: "Mock Interview", href: "/mock-interview", icon: "🎯" },
  { label: "Practice Hub", href: "/practice", icon: "💡" },
  { label: "Careers", href: "/careers", icon: "🚀" },
  { label: "Roadmap", href: "/roadmap", icon: "🗺️" },
  { label: "Skills", href: "/skills", icon: "⚡" },
  { label: "Assessment", href: "/quiz", icon: "📝" },
  { label: "Profile", href: "/profile", icon: "👤" },
];

/**
 * Mentor-specific navigation items.
 */
export const MENTOR_NAV_ITEMS: readonly NavItem[] = [
  { label: "Mentor Dashboard", href: "/mentorship/manage", icon: "📊" },
  { label: "Mentorship Hub", href: "/mentorship", icon: "👥" },
  { label: "Messages", href: "/messages", icon: "💬" },
  { label: "AI Career Advisor", href: "/chat", icon: "🤖" },
  { label: "Careers", href: "/careers", icon: "🚀" },
  { label: "Profile", href: "/profile", icon: "👤" },
];

/**
 * Returns role-appropriate navigation items.
 */
export function getNavItemsForRole(role?: string | null): NavItem[] {
  if (role === "ADMIN") {
    return [...ADMIN_NAV_ITEMS];
  }
  if (role === "MENTOR") {
    return [...MENTOR_NAV_ITEMS];
  }
  return [...STUDENT_NAV_ITEMS];
}

/**
 * Returns the default landing path for a given role upon authentication.
 */
export function getDefaultRouteForRole(role?: string | null): string {
  if (role === "ADMIN") {
    return "/admin";
  }
  if (role === "MENTOR") {
    return "/mentorship/manage";
  }
  return "/dashboard";
}

/**
 * Checks whether a pathname is an administrative path.
 */
export function isAdminRoute(pathname: string): boolean {
  return pathname === "/admin" || pathname.startsWith("/admin/");
}

/**
 * Checks whether a pathname is an active match for a navigation link.
 */
export function isNavItemActive(pathname: string, itemHref: string): boolean {
  if (itemHref === "/dashboard" || itemHref === "/admin" || itemHref === "/mentorship") {
    return pathname === itemHref;
  }
  return pathname.startsWith(itemHref);
}
