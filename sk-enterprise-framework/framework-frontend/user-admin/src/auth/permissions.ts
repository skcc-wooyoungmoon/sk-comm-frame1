import type { Permission, UserRole } from '../types';

/**
 * 역할(Role) → 권한(Permission) 매핑.
 * 화면 라우팅 가드와 버튼/메뉴 노출 제어에 사용한다.
 * (프론트 게이팅은 UX용이며, 실제 권한 검증은 백엔드가 담당한다.)
 */
const ROLE_PERMISSIONS: Record<UserRole, Permission[]> = {
  ADMIN: [
    'dashboard:view',
    'user:view',
    'user:manage',
    'product:view',
    'product:manage',
    'order:view',
    'order:manage',
    'monitoring:view',
    'log:view',
  ],
  MANAGER: [
    'dashboard:view',
    'user:view',
    'product:view',
    'product:manage',
    'order:view',
    'order:manage',
    'monitoring:view',
  ],
  USER: ['dashboard:view', 'product:view', 'order:view'],
};

export function permissionsFor(role: UserRole): Permission[] {
  return ROLE_PERMISSIONS[role] ?? [];
}

export function hasPermission(role: UserRole | undefined, permission: Permission): boolean {
  if (!role) return false;
  return permissionsFor(role).includes(permission);
}
