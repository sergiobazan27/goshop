import { Page } from './page.model';

export type SubscriptionStatus = 'NOT_STARTED' | 'ACTIVE' | 'EXPIRED';
export type SubscriptionAction = 'ASSIGNED' | 'RENEWED' | 'PLAN_CHANGED';
export type SubscriptionMode = 'assign' | 'renew' | 'change';

export interface SubscriptionPlan {
  id: string;
  name: string;
  maxTables: number;
  maxUsers: number;
  maxProducts: number;
}

export interface Subscription {
  id: string;
  businessId: string;
  plan: SubscriptionPlan;
  startsAt: string;
  expiresAt: string;
  status: SubscriptionStatus;
}

export interface AssignSubscriptionRequest {
  planId: string;
  startsAt: string;
  expiresAt: string;
}

export interface RenewSubscriptionRequest {
  planId: string;
  startsAt?: string;
  expiresAt: string;
}

export interface SubscriptionHistoryEntry {
  id: string;
  action: SubscriptionAction;
  actorSubject: string;
  actorName: string;
  changedAt: string;
  subscription: Subscription;
}

export interface SubscriptionDetails {
  current: Subscription | null;
  history: Page<SubscriptionHistoryEntry>;
}
