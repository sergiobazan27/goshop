import { BusinessSummary } from './business.model';
import { Subscription } from './subscription.model';

export interface CurrentUser {
  sub: string;
  username: string;
  roles: string[];
  membershipRole: 'OWNER' | 'WAITER' | null;
  business: BusinessSummary | null;
  subscription: Subscription | null;
}
