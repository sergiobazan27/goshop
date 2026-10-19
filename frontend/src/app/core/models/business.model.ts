export type BusinessStatus = 'ACTIVE' | 'SUSPENDED';

export interface ChangeBusinessStatusRequest {
  status: BusinessStatus;
}

export interface BusinessSummary {
  id: string;
  name: string;
  status: BusinessStatus;
}

export interface Business extends BusinessSummary {
  createdAt: string;
  updatedAt: string;
}

export interface CreateBusinessRequest {
  name: string;
  ownerKeycloakUserId: string;
}

export interface BusinessListQuery {
  search: string;
  status: BusinessStatus | '';
  page: number;
  size: number;
}
