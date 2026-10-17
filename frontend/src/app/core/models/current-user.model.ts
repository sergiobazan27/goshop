import { BusinessSummary } from './business.model';

export interface CurrentUser {
  sub: string;
  username: string;
  roles: string[];
  membershipRole: 'OWNER' | 'WAITER' | null;
  business: BusinessSummary | null;
}
