export interface TeamMember {
  id: string;
  keycloakUserId: string;
  username: string | null;
  role: 'OWNER' | 'WAITER';
  joinedAt: string;
}

export interface AddTeamMemberRequest {
  keycloakUserId: string;
}

export interface AvailableWaiter {
  id: string;
  username: string;
  displayName: string;
}
