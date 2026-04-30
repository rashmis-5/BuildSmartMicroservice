export const ROLE_TO_DASHBOARD = {
  ADMIN: '/dashboard/admin',
  PROJECT_MANAGER: '/dashboard/project',
  SAFETY_OFFICER: '/dashboard/safety',
  FINANCE_OFFICER: '/dashboard/finance',
  VENDOR: '/dashboard/vendor',
  SITE_ENGINEER: '/dashboard/site',
};

export function dashboardPathFor(role) {
  return ROLE_TO_DASHBOARD[role] || '/login';
}

export const ROLES = Object.keys(ROLE_TO_DASHBOARD);
